package com.glodblock.github.extendedae.client.gui;

import appeng.api.config.AccessRestriction;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.YesNo;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.GuiText;
import com.glodblock.github.extendedae.container.ContainerModStorageBus;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CEAEGenericPacket;
import com.glodblock.github.extendedae.util.FCClientUtil;
import com.glodblock.github.glodium.network.packet.sync.ActionMap;
import com.glodblock.github.glodium.network.packet.sync.IActionHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class GuiModStorageBus extends UpgradeableScreen<ContainerModStorageBus> implements IActionHolder {

    private final ActionMap actions = ActionMap.create();
    private final SettingToggleButton<AccessRestriction> rwMode;
    private final SettingToggleButton<StorageFilter> storageFilter;
    private final SettingToggleButton<YesNo> filterOnExtract;
    private final AETextField filterInputs;

    public GuiModStorageBus(ContainerModStorageBus menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.widgets.addOpenPriorityButton();
        addToLeftToolbar(new ActionButton(ActionItems.COG, btn -> menu.partition()));
        this.rwMode = new ServerSettingToggleButton<>(Settings.ACCESS, AccessRestriction.READ_WRITE);
        this.storageFilter = new ServerSettingToggleButton<>(Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
        this.filterOnExtract = new ServerSettingToggleButton<>(Settings.FILTER_ON_EXTRACT, YesNo.YES);
        this.addToLeftToolbar(this.storageFilter);
        this.addToLeftToolbar(this.filterOnExtract);
        this.addToLeftToolbar(this.rwMode);
        this.filterInputs = widgets.addTextField("filter_input");
        this.filterInputs.setMaxLength(512);
        this.filterInputs.setPlaceholder(Component.translatable("gui.extendedae.mod_storage_bus.tooltip"));
        this.filterInputs.setResponder(s -> {
            this.filterInputs.setSuggestion(FCClientUtil.getModName(s));
            EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("set", s));
        });
        this.actions.put("init", o -> this.filterInputs.setValue(o.get(0)));
        EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("update"));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int keyPressed) {
        if (keyCode == GLFW.GLFW_KEY_TAB && this.filterInputs.isFocused()) {
            var suggest = FCClientUtil.getModName(this.filterInputs.getValue());
            if (!suggest.isEmpty()) {
                this.filterInputs.setValue(this.filterInputs.getValue() + suggest);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, keyPressed);
    }

    @Override
    public boolean mouseClicked(double xCoord, double yCoord, int btn) {
        if (btn == 1 && this.filterInputs.isMouseOver(xCoord, yCoord)) {
            this.filterInputs.setValue("");
        }
        return super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.storageFilter.set(this.menu.getStorageFilter());
        this.rwMode.set(this.menu.getReadWriteMode());
        this.filterOnExtract.set(this.menu.getFilterOnExtract());
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(10, 17, 0);
        poseStack.scale(0.6f, 0.6f, 1);
        var color = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR);
        if (menu.getConnectedTo() != null) {
            guiGraphics.drawString(font, GuiText.AttachedTo.text(menu.getConnectedTo()), 0, 0, color.toARGB(), false);
        } else {
            guiGraphics.drawString(font, GuiText.Unattached.text(), 0, 0, color.toARGB(), false);
        }
        poseStack.popPose();
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(this.filterInputs);
    }

    @NotNull
    @Override
    public ActionMap getActionMap() {
        return this.actions;
    }
}