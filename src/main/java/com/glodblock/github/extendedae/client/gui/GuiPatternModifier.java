package com.glodblock.github.extendedae.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AE2Button;
import appeng.core.AppEng;
import com.glodblock.github.extendedae.client.button.ActionEPPButton;
import com.glodblock.github.extendedae.config.EAEConfig;
import com.glodblock.github.extendedae.container.ContainerPatternModifier;
import com.glodblock.github.extendedae.network.EAENetworkHandler;
import com.glodblock.github.extendedae.network.packet.CEAEGenericPacket;
import com.glodblock.github.extendedae.network.packet.CUpdatePage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class GuiPatternModifier extends AEBaseScreen<ContainerPatternModifier> {

    private final ActionEPPButton clone;
    private final Button replace;
    private final List<Button> multiBtns = new ArrayList<>();

    public GuiPatternModifier(ContainerPatternModifier menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        ActionEPPButton changeMode = new ActionEPPButton(b -> EAENetworkHandler.INSTANCE.sendToServer(new CUpdatePage(() -> (this.menu.page + 1) % 3)), Icon.SCHEDULING_DEFAULT.getBlitter());
        this.clone = new ActionEPPButton(b -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("clone")), Icon.ARROW_RIGHT.getBlitter());
        changeMode.setMessage(Component.translatable("gui.extendedae.pattern_modifier.change"));
        this.clone.setMessage(Component.translatable("gui.extendedae.pattern_modifier.clone.desc"));
        addToLeftToolbar(changeMode);
        this.replace = new AE2Button(
                0, 0, 46, 20,
                Component.translatable("gui.extendedae.pattern_modifier.replace_button"),
                b -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("replace"))
        );
        this.multiBtns.add(getButton(0, 23, 20, false));
        this.multiBtns.add(getButton(1, 23, 20, false));
        this.multiBtns.add(getButton(2, 23, 20, false));
        this.multiBtns.add(getButton(3, 23, 20, false));
        this.multiBtns.add(getButton(0, 23, 20, true));
        this.multiBtns.add(getButton(1, 23, 20, true));
        this.multiBtns.add(getButton(2, 23, 20, true));
        this.multiBtns.add(getButton(3, 23, 20, true));
        var clearBtn = new AE2Button(
                0, 0, 36, 20,
                Component.translatable("gui.extendedae.pattern_modifier.clear"),
                b -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("clear"))
        );
        clearBtn.setTooltip(Tooltip.create(Component.translatable("gui.extendedae.pattern_modifier.clear.desc")));
        this.multiBtns.add(clearBtn);
        this.imageHeight = 210;
    }

    private Button getButton(int index, int width, int height, boolean isDiv) {
        var btn = new AE2Button(getDisplayNumber(EAEConfig.getPatternModifierNumber(index), isDiv), b -> EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("modify", EAEConfig.getPatternModifierNumber(index), isDiv)));
        btn.setSize(width, height);
        if (!isDiv) {
            btn.setTooltip(Tooltip.create(Component.translatable("gui.extendedae.pattern_modifier.multi.desc", EAEConfig.getPatternModifierNumber(index))));
        } else {
            btn.setTooltip(Tooltip.create(Component.translatable("gui.extendedae.pattern_modifier.div.desc", EAEConfig.getPatternModifierNumber(index))));
        }
        return btn;
    }

    private Component getDisplayNumber(int number, boolean isDiv) {
        return isDiv ? Component.literal("÷" + number) :  Component.literal("x" + number);
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        guiGraphics.drawString(
                this.font,
                Component.translatable("gui.extendedae.pattern_modifier", this.getModeName()),
                8,
                6,
                style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(),
                false
        );
        if (this.menu.page == 2) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("gui.extendedae.pattern_modifier.blank"),
                    52,
                    62,
                    style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(),
                    false
            );
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("gui.extendedae.pattern_modifier.target"),
                    51,
                    25,
                    style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(),
                    false
            );
        }
    }

    @Override
    public void init() {
        super.init();
        this.multiBtns.get(0).setPosition(this.leftPos + 7, this.topPos + 16);
        this.multiBtns.get(1).setPosition(this.leftPos + 37, this.topPos + 16);
        this.multiBtns.get(2).setPosition(this.leftPos + 67, this.topPos + 16);
        this.multiBtns.get(3).setPosition(this.leftPos + 97, this.topPos + 16);
        this.multiBtns.get(4).setPosition(this.leftPos + 7, this.topPos + 38);
        this.multiBtns.get(5).setPosition(this.leftPos + 37, this.topPos + 38);
        this.multiBtns.get(6).setPosition(this.leftPos + 67, this.topPos + 38);
        this.multiBtns.get(7).setPosition(this.leftPos + 97, this.topPos + 38);
        this.multiBtns.get(8).setPosition(this.leftPos + 133, this.topPos + 26);
        this.multiBtns.forEach(this::addRenderableWidget);
        this.clone.setPosition(this.leftPos + 80, this.topPos + 36);
        this.addRenderableWidget(this.clone);
        this.replace.setPosition(this.leftPos + 123, this.topPos + 26);
        this.addRenderableWidget(this.replace);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        EAENetworkHandler.INSTANCE.sendToServer(new CEAEGenericPacket("show"));
        this.menu.showPage();
        if (this.menu.page == 0) {
            this.clone.setVisibility(false);
            this.multiBtns.forEach(b -> b.visible = true);
            this.replace.visible = false;
        } else if (this.menu.page == 1) {
            this.clone.setVisibility(false);
            this.multiBtns.forEach(b -> b.visible = false);
            this.replace.visible = true;
        } else if (this.menu.page == 2) {
            this.clone.setVisibility(true);
            this.multiBtns.forEach(b -> b.visible = false);
            this.replace.visible = false;
        }
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        if (this.menu.page == 0) {
            guiGraphics.blit(AppEng.makeId("textures/guis/pattern_editor_1.png"), offsetX, offsetY, 0, 0, 176, 212);
        } else if (this.menu.page == 1) {
            guiGraphics.blit(AppEng.makeId("textures/guis/pattern_editor_3.png"), offsetX, offsetY, 0, 0, 176, 212);
        } else if (this.menu.page == 2) {
            guiGraphics.blit(AppEng.makeId("textures/guis/pattern_editor_2.png"), offsetX, offsetY, 0, 0, 176, 212);
        }
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);
    }

    private Component getModeName() {
        return switch (this.menu.page) {
            case 0 -> Component.translatable("gui.extendedae.pattern_modifier.multiply");
            case 1 -> Component.translatable("gui.extendedae.pattern_modifier.replace");
            case 2 -> Component.translatable("gui.extendedae.pattern_modifier.clone");
            default -> Component.empty();
        };
    }
}