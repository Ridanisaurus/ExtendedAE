package com.glodblock.github.extendedae.common.tileentities;

import appeng.api.behaviors.ContainerItemContext;
import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.glodblock.github.extendedae.api.CanerMode;
import com.glodblock.github.extendedae.api.caps.IGenericInvHost;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.glodium.util.GlodUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class TileCaner extends AENetworkedPoweredBlockEntity implements IGridTickable, ICraftingMachine, IGenericInvHost {

    public static final int POWER_MAXIMUM_AMOUNT = 3200;
    public static final int POWER_USAGE = 80;
    private final AppEngInternalInventory container = new AppEngInternalInventory(this, 1, 1);
    private final GenericStackInv stuff = new GenericStackInv(this::wake, 1);
    private ItemStack target = ItemStack.EMPTY;
    private Direction ejectSide = null;
    private CanerMode mode = CanerMode.FILL;
    private AEKey emptyKey = null;

    public TileCaner(BlockPos pos, BlockState blockState) {
        super(GlodUtil.getTileType(TileCaner.class, TileCaner::new, EAESingletons.CANER), pos, blockState);
        // don't let container item go into it
        this.stuff.setCapacity(AEKeyType.items(), 0);
        this.getMainNode()
                .setFlags()
                .setIdlePowerUsage(0)
                .addService(IGridTickable.class, this);
        this.setInternalMaxPower(POWER_MAXIMUM_AMOUNT);
        this.setPowerSides(getGridConnectableSides(getOrientation()));
    }

    public AppEngInternalInventory getContainer() {
        return this.container;
    }

    @Override
    public GenericStackInv getGenericInv() {
        return this.stuff;
    }

    public CanerMode getMode() {
        return this.mode;
    }

    public void setMode(CanerMode mode) {
        this.mode = mode;
    }

    @Nullable
    private ContainerItemContext getStrategy(AEKey type, Player player, ItemStack target) {
        if (ContainerItemStrategies.isKeySupported(type)) {
            return ContainerItemStrategies.findOwnedItemContext(type.getType(), player, target);
        }
        return null;
    }

    public boolean isDone() {
        if (!this.target.isEmpty()) {
            return ItemStack.isSameItemSameComponents(this.target, this.container.getStackInSlot(0));
        }
        return false;
    }

    private void eject() {
        if (this.level instanceof ServerLevel && !this.container.getStackInSlot(0).isEmpty()) {
            if (this.ejectSide != null) {
                var target = InternalInventory.wrapExternal(this.level, this.getBlockPos().relative(this.ejectSide), this.ejectSide.getOpposite());
                if (target != null) {
                    int startItems = this.container.getStackInSlot(0).getCount();
                    this.container.insertItem(0, target.addItems(this.container.extractItem(0, 64, false)), false);
                    int endItems = this.container.getStackInSlot(0).getCount();
                    long pushed = 0;
                    long origin = 0;
                    var obj = this.stuff.getStack(0);
                    if (obj != null) {
                        var genTarget = StackWorldBehaviors.createExportFacade((ServerLevel) this.level, this.getBlockPos().relative(this.ejectSide), this.ejectSide.getOpposite());
                        origin = obj.amount();
                        pushed = genTarget.push(obj.what(), origin, Actionable.MODULATE);
                        this.stuff.extract(0, obj.what(), pushed, Actionable.MODULATE);
                    }
                    if (startItems != endItems && pushed == origin) {
                        this.target = ItemStack.EMPTY;
                        this.emptyKey = null;
                    }
                }
            }
        }
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(orientation.getSide(RelativeSide.TOP), orientation.getSide(RelativeSide.BOTTOM));
    }

    @Override
    protected void onOrientationChanged(BlockOrientation orientation) {
        super.onOrientationChanged(orientation);
        this.setPowerSides(getGridConnectableSides(orientation));
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, !hasJob());
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        this.markForUpdate();
        if (this.getInternalCurrentPower() < POWER_MAXIMUM_AMOUNT) {
            getMainNode().ifPresent(grid -> {
                double toExtract = Math.min(POWER_USAGE, this.getInternalMaxPower() - this.getInternalCurrentPower());
                final double extracted = grid.getEnergyService().extractAEPower(toExtract, Actionable.MODULATE, PowerMultiplier.ONE);
                this.injectExternalPower(PowerUnit.AE, extracted, Actionable.MODULATE);
            });
        }
        if (this.mode == CanerMode.FILL) {
            this.fill();
        } else if (this.mode == CanerMode.EMPTY) {
            this.empty();
        }
        if (this.isDone()) {
            this.eject();
        }
        return TickRateModulation.FASTER;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.container;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == this.container) {
            this.wake();
        }
    }

    private void wake() {
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.stuff.writeToChildTag(data, "stuff", registries);
        data.put("target", this.target.saveOptional(registries));
        if (this.ejectSide != null) {
            data.putString("ejectSide", this.ejectSide.name());
        }
        data.putByte("mode", (byte) this.mode.ordinal());
        if (this.emptyKey != null) {
            data.put("emptyKey", this.emptyKey.toTag(registries));
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.stuff.readFromChildTag(data, "stuff", registries);
        if (data.contains("target")) {
            this.target = ItemStack.parseOptional(registries, data.getCompound("target"));
        }
        if (data.contains("ejectSide")) {
            this.ejectSide = Direction.valueOf(data.getString("ejectSide"));
        }
        if (data.contains("mode")) {
            this.mode = CanerMode.values()[data.getByte("mode")];
        }
        if (data.contains("emptyKey")) {
            this.emptyKey = AEKey.fromTagGeneric(registries, data.getCompound("emptyKey"));
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        var tag = input.get(EAESingletons.EXTRA_SETTING);
        if (tag != null && tag.contains("caner_mode")) {
            this.mode = CanerMode.values()[tag.getByte("caner_mode")];
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.MEMORY_CARD) {
            var tag = new CompoundTag();
            tag.putByte("caner_mode", (byte) this.mode.ordinal());
            output.set(EAESingletons.EXTRA_SETTING, tag);
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        var stack = this.stuff.getStack(0);
        if (stack != null) {
            stack.what().addDrops(stack.amount(), drops, level, pos);
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.stuff.clear();
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        var changed = super.readFromStream(data);
        this.container.setItemDirect(0, ItemStack.OPTIONAL_STREAM_CODEC.decode(data));
        return changed;
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(data, this.container.getStackInSlot(0));
    }

    private void fill() {
        var stack = this.container.getStackInSlot(0);
        var obj = this.stuff.getStack(0);
        if (stack.isEmpty() || obj == null) {
            return;
        }
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        var player = Platform.getFakePlayer((ServerLevel) this.level, null);
        player.getInventory().setItem(0, stack);
        player.getInventory().setItem(1, ItemStack.EMPTY);
        var handler = this.getStrategy(obj.what(), player, stack);
        if (handler == null) {
            return;
        }
        if (this.getInternalCurrentPower() >= POWER_USAGE) {
            long added = handler.insert(obj.what(), obj.amount(), Actionable.MODULATE);
            if (added > 0) {
                this.stuff.extract(0, obj.what(), added, Actionable.MODULATE);
                if (!player.getInventory().getItem(0).isEmpty()) {
                    this.container.setItemDirect(0, player.getInventory().getItem(0).copy());
                } else {
                    this.container.setItemDirect(0, player.getInventory().getItem(1).copy());
                }
                this.extractAEPower(POWER_USAGE, Actionable.MODULATE, PowerMultiplier.CONFIG);
            }
        }
    }

    private void empty() {
        var stack = this.container.getStackInSlot(0);
        var obj = this.stuff.getStack(0);
        if (stack.isEmpty()) {
            return;
        }
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        GenericStack contents;
        if (this.emptyKey != null) {
            contents = ContainerItemStrategies.getContainedStack(stack, this.emptyKey.getType());
        } else {
            contents = ContainerItemStrategies.getContainedStack(stack);
        }
        if (contents == null) {
            return;
        }
        if (obj != null && !obj.what().equals(contents.what())) {
            return;
        }
        var player = Platform.getFakePlayer((ServerLevel) this.level, null);
        player.getInventory().setItem(0, stack);
        player.getInventory().setItem(1, ItemStack.EMPTY);
        var handler = this.getStrategy(contents.what(), player, stack);
        if (handler == null) {
            return;
        }
        if (this.getInternalCurrentPower() >= POWER_USAGE) {
            long toAdd = handler.extract(contents.what(), contents.amount(), Actionable.SIMULATE);
            if (toAdd > 0) {
                long canAdd = this.stuff.insert(0, contents.what(), toAdd, Actionable.SIMULATE);
                if (canAdd == toAdd) {
                    handler.extract(contents.what(), canAdd, Actionable.MODULATE);
                    this.stuff.insert(0, contents.what(), canAdd, Actionable.MODULATE);
                    if (!player.getInventory().getItem(0).isEmpty()) {
                        this.container.setItemDirect(0, player.getInventory().getItem(0).copy());
                    } else {
                        this.container.setItemDirect(0, player.getInventory().getItem(1).copy());
                    }
                    this.extractAEPower(POWER_USAGE, Actionable.MODULATE, PowerMultiplier.CONFIG);
                }
            }
        }
    }

    private boolean hasJob() {
        if (this.mode == CanerMode.FILL) {
            return this.stuff.getStack(0) != null && !this.container.getStackInSlot(0).isEmpty();
        } else if (this.mode == CanerMode.EMPTY) {
            return !this.container.getStackInSlot(0).isEmpty();
        }
        return false;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        Component name = this.hasCustomName() ? this.getCustomName() : EAESingletons.CANER.asItem().getDescription();
        return new PatternContainerGroup(AEItemKey.of(EAESingletons.CANER), name, List.of());
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputs, Direction ejectionDirection) {
        if (patternDetails instanceof AEProcessingPattern) {
            if (this.stuff.getStack(0) == null && this.container.getStackInSlot(0).isEmpty()) {
                if (this.mode == CanerMode.FILL) {
                    if (inputs.length == 2) {
                        if (inputs[0].getFirstEntry() != null && inputs[1].getFirstEntry() != null) {
                            var obj = inputs[0].getFirstEntry();
                            var cnt = inputs[1].getFirstEntry();
                            var rst = patternDetails.getPrimaryOutput();
                            if (obj.getKey() instanceof AEItemKey) {
                                obj = inputs[1].getFirstEntry();
                                cnt = inputs[0].getFirstEntry();
                            }
                            // sanity check
                            if (!(cnt.getKey() instanceof AEItemKey) || cnt.getLongValue() != 1) {
                                return false;
                            }
                            if (!(rst.what() instanceof AEItemKey) || rst.amount() != 1) {
                                return false;
                            }
                            // try to fill
                            this.stuff.setStack(0, new GenericStack(obj.getKey(), obj.getLongValue()));
                            this.container.setItemDirect(0, ((AEItemKey) cnt.getKey()).toStack());
                            // check success
                            boolean fail = this.stuff.getStack(0) == null || this.stuff.getStack(0).amount() != obj.getLongValue();
                            if (this.container.getStackInSlot(0).isEmpty()) {
                                fail = true;
                            }
                            // roll back
                            if (fail) {
                                this.stuff.setStack(0, null);
                                this.container.setItemDirect(0, ItemStack.EMPTY);
                                return false;
                            } else {
                                this.target = ((AEItemKey) rst.what()).toStack();
                                this.ejectSide = ejectionDirection;
                                return true;
                            }
                        }
                    }
                } else if (this.mode == CanerMode.EMPTY) {
                    if (inputs.length == 1 && patternDetails.getOutputs().size() == 2) {
                        if (inputs[0].getFirstEntry() != null) {
                            var cnt = inputs[0].getFirstEntry();
                            var obj = patternDetails.getOutputs().get(0);
                            var rst = patternDetails.getOutputs().get(1);
                            if (obj.what() instanceof AEItemKey) {
                                obj = patternDetails.getOutputs().get(1);
                                rst = patternDetails.getOutputs().get(0);
                            }
                            // sanity check
                            if (!(cnt.getKey() instanceof AEItemKey) || cnt.getLongValue() != 1) {
                                return false;
                            }
                            if (!(rst.what() instanceof AEItemKey) || rst.amount() != 1) {
                                return false;
                            }
                            // try to fill
                            this.container.setItemDirect(0, ((AEItemKey) cnt.getKey()).toStack());
                            // check success
                            boolean fail = this.container.getStackInSlot(0).isEmpty() || obj == null;
                            // roll back
                            if (!fail) {
                                this.target = ((AEItemKey) rst.what()).toStack();
                                this.emptyKey = obj.what();
                                this.ejectSide = ejectionDirection;
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean acceptsPlans() {
        return true;
    }

}
