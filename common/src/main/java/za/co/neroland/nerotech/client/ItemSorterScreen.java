package za.co.neroland.nerotech.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.nerotech.menu.ItemSorterMenu;

/** Concrete screen for the Item Sorter. */
public class ItemSorterScreen extends MachineScreen<ItemSorterMenu> {

    public ItemSorterScreen(ItemSorterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
