package com.glodblock.github.extendedae.container;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixBase;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixCrafter;
import com.glodblock.github.extendedae.common.tileentities.matrix.TileAssemblerMatrixPattern;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.SAssemblerMatrixUpdate;
import com.glodblock.github.glodium.network.packet.sync.ActionMap;
import com.glodblock.github.glodium.network.packet.sync.IActionHolder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ContainerAssemblerMatrix extends AEBaseMenu implements IActionHolder {

    public static final MenuType<ContainerAssemblerMatrix> TYPE = MenuTypeBuilder
            .create(ContainerAssemblerMatrix::new, TileAssemblerMatrixBase.class)
            .build(ExtendedAE.id("assembler_matrix"));

    private final ActionMap actions = ActionMap.create();
    private final List<PatternSlotTracker> trackers = new ArrayList<>();
    private final Int2ReferenceMap<PatternSlotTracker> trackerMap = new Int2ReferenceOpenHashMap<>();
    private final TileAssemblerMatrixBase host;

    public ContainerAssemblerMatrix(int id, Inventory playerInventory, TileAssemblerMatrixBase host) {
        super(TYPE, id, playerInventory, host);
        this.actions.put("cancel", o -> cancel());
        this.host = host;
        this.setupPatternInventory();
        this.createPlayerInventorySlots(playerInventory);
    }

    private void cancel() {
        var cluster = this.host.getCluster();
        if (cluster != null && !cluster.isDestroyed()) {
            cluster.getBlockEntities().forEachRemaining(te -> {
                if (te instanceof TileAssemblerMatrixCrafter crafter) {
                    crafter.stop();
                }
            });
        }
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        var inv = this.trackerMap.get((int) id);
        if (inv == null) {
            return;
        }
        if (slot < 0 || slot >= inv.server.size()) {
            return;
        }

        final ItemStack is = inv.server.getStackInSlot(slot);

        var patternSlot = new FilteredInternalInventory(inv.server.getSlotInv(slot), new TileAssemblerMatrixPattern.Filter(() -> this.getHost().getLevel()));

        var carried = getCarried();
        switch (action) {
            case PICKUP_OR_SET_DOWN -> {
                if (!carried.isEmpty()) {
                    ItemStack inSlot = patternSlot.getStackInSlot(0);
                    if (inSlot.isEmpty()) {
                        setCarried(patternSlot.addItems(carried));
                    } else {
                        inSlot = inSlot.copy();
                        final ItemStack inHand = carried.copy();

                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                        setCarried(ItemStack.EMPTY);

                        setCarried(patternSlot.addItems(inHand.copy()));

                        if (getCarried().isEmpty()) {
                            setCarried(inSlot);
                        } else {
                            setCarried(inHand);
                            patternSlot.setItemDirect(0, inSlot);
                        }
                    }
                } else {
                    setCarried(patternSlot.getStackInSlot(0));
                    patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }
            }
            case SPLIT_OR_PLACE_SINGLE -> {
                if (!carried.isEmpty()) {
                    ItemStack extra = carried.split(1);
                    if (!extra.isEmpty()) {
                        extra = patternSlot.addItems(extra);
                    }
                    if (!extra.isEmpty()) {
                        carried.grow(extra.getCount());
                    }
                } else if (!is.isEmpty()) {
                    setCarried(patternSlot.extractItem(0, (is.getCount() + 1) / 2, false));
                }
            }
            case SHIFT_CLICK -> {
                var stack = patternSlot.getStackInSlot(0).copy();
                if (!player.getInventory().add(stack)) {
                    patternSlot.setItemDirect(0, stack);
                } else {
                    patternSlot.setItemDirect(0, ItemStack.EMPTY);
                }
            }
            case MOVE_REGION -> {
                for (int x = 0; x < inv.server.size(); x++) {
                    var stack = inv.server.getStackInSlot(x);
                    if (!player.getInventory().add(stack)) {
                        patternSlot.setItemDirect(0, stack);
                    } else {
                        patternSlot.setItemDirect(0, ItemStack.EMPTY);
                    }
                }
            }
            case CREATIVE_DUPLICATE -> {
                if (player.getAbilities().instabuild && carried.isEmpty()) {
                    setCarried(is.isEmpty() ? ItemStack.EMPTY : is.copy());
                }
            }
        }
    }

    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }
        super.broadcastChanges();
        if (this.getPlayer() instanceof ServerPlayer player) {
            for (var tracker : this.trackers) {
                if (tracker.init) {
                    var ptk = tracker.createPacket();
                    if (ptk != null) {
                        EAENetworkHandler.INSTANCE.sendTo(ptk, player);
                    }
                } else {
                    tracker.init = true;
                    EAENetworkHandler.INSTANCE.sendTo(tracker.fullPacket(), player);
                }
            }
        }
    }

    private void setupPatternInventory() {
        if (isClientSide()) {
            return;
        }
        this.trackers.clear();
        this.trackerMap.clear();
        var cluster = this.host.getCluster();
        if (cluster != null && !cluster.isDestroyed()) {
            for (var pattern : cluster.getPatterns()) {
                var tracker = new PatternSlotTracker(pattern);
                this.trackers.add(tracker);
                this.trackerMap.put(pattern.getLocateID(), tracker);
            }
        }
    }

    public TileAssemblerMatrixBase getHost() {
        return this.host;
    }

    @NotNull
    @Override
    public ActionMap getActionMap() {
        return this.actions;
    }

    private static class PatternSlotTracker {

        private final TileAssemblerMatrixPattern invHost;
        private final InternalInventory client;
        private final InternalInventory server;
        private final Int2ObjectMap<ItemStack> changedMap = new Int2ObjectOpenHashMap<>();
        private boolean init = false;

        public PatternSlotTracker(TileAssemblerMatrixPattern host) {
            this.invHost = host;
            this.client = new AppEngInternalInventory(TileAssemblerMatrixPattern.INV_SIZE);
            this.server = host.getPatternInventory();
        }

        private Int2ObjectMap<ItemStack> getChangedMap() {
            this.changedMap.clear();
            for (int x = 0; x < server.size(); x ++) {
                var ss = server.getStackInSlot(x);
                var cs = client.getStackInSlot(x);
                if (!ItemStack.isSameItemSameComponents(ss, cs)) {
                    this.changedMap.put(x, ss.copy());
                    client.setItemDirect(x, ss.copy());
                }
            }
            return this.changedMap;
        }

        private Int2ObjectMap<ItemStack> getFullMap() {
            this.changedMap.clear();
            for (int x = 0; x < server.size(); x ++) {
                this.changedMap.put(x, server.getStackInSlot(x).copy());
            }
            return this.changedMap;
        }

        @Nullable
        public SAssemblerMatrixUpdate createPacket() {
            var map = this.getChangedMap();
            if (map.isEmpty()) {
                return null;
            }
            return new SAssemblerMatrixUpdate(invHost.getLocateID(), map);
        }

        public SAssemblerMatrixUpdate fullPacket() {
            return new SAssemblerMatrixUpdate(invHost.getLocateID(), this.getFullMap());
        }

    }

}
