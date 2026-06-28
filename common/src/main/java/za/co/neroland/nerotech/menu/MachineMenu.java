package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shared base for NeroTech machine menus. Lays out the upgrade-module slots and the player inventory,
 * exposes the synced {@link ContainerData} (energy + work progress as permille for short-safe sync),
 * and implements a standard shift-click transfer between the machine block and the player inventory.
 * Subclasses add their machine-specific I/O slots in index order before calling {@link #addUpgradeAndPlayerSlots}.
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected final Container container;
    protected final ContainerData data;
    protected final int machineSlots;
    protected final int totalNonPlayer;

    protected MachineMenu(MenuType<?> type, int id, Container container, ContainerData data, int machineSlots) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.machineSlots = machineSlots;
        this.totalNonPlayer = container.getContainerSize();
    }

    /** Add the upgrade-module slots (right column) then the player inventory + hotbar. */
    protected void addUpgradeAndPlayerSlots(Inventory playerInventory) {
        int upgrades = this.totalNonPlayer - this.machineSlots;
        // Upgrade modules: a tidy 2×2 block in the top-right of the machine area (clear of the gauges).
        for (int i = 0; i < upgrades; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new PredicateSlot(this.container, this.machineSlots + i, 138 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // --- synced data (permille; see NeroTechMachineBlockEntity) --------------

    public int energyPermille() {
        return this.data.get(0);
    }

    public float energyFraction() {
        return this.data.get(1) <= 0 ? 0f : (float) this.data.get(0) / this.data.get(1);
    }

    public float workFraction() {
        return this.data.get(3) <= 0 ? 0f : this.data.get(2) / 1000f;
    }

    public boolean working() {
        return this.data.get(3) > 0;
    }

    public float heatFraction() {
        return this.data.get(5) <= 0 ? 0f : this.data.get(4) / (float) this.data.get(5);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = this.totalNonPlayer;
            int playerEnd = playerStart + 36;
            if (index < playerStart) {
                if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, playerStart, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** A slot that defers placement validity to the backing container ({@code canPlaceItem}). */
    protected static class PredicateSlot extends Slot {
        public PredicateSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }

    /** An output slot: never accepts manual placement. */
    protected static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
