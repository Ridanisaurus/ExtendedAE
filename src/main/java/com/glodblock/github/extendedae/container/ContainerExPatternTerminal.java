package com.glodblock.github.extendedae.container;

import appeng.api.storage.IPatternAccessTermMenuHost;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternAccessTermMenu;
import com.glodblock.github.extendedae.ExtendedAE;
import com.glodblock.github.extendedae.common.parts.PartExPatternAccessTerminal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ContainerExPatternTerminal extends PatternAccessTermMenu {

    public static final MenuType<ContainerExPatternTerminal> TYPE = MenuTypeBuilder
            .create(ContainerExPatternTerminal::new, PartExPatternAccessTerminal.class)
            .build(ExtendedAE.id("ex_pattern_access_terminal"));

    public ContainerExPatternTerminal(int id, Inventory ip, IPatternAccessTermMenuHost host) {
        super(TYPE, id, ip, host, true);
    }

    public ContainerExPatternTerminal(MenuType<?> type, int id, Inventory ip, IPatternAccessTermMenuHost host, boolean bindInventory) {
        super(type, id, ip, host, bindInventory);
    }

}
