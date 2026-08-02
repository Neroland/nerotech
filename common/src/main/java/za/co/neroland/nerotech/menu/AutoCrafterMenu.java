package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.machine.AutoCrafterBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Auto Crafter menu: 3×3 grid + output + upgrade column + player inventory. The recipe-preview ghost
 * (container slot 10) and the nine lock-template ghosts (11..19) ride along as off-panel
 * {@link GhostSlot}s purely so vanilla slot sync carries them to the client — the screen renders them
 * itself. The Lock toggle routes through {@link #clickMenuButton} (no custom packet).
 */
public class AutoCrafterMenu extends MachineMenu {

    public static final int BUTTON_TOGGLE_LOCK = 0;

    private static final int MACHINE_SLOTS =
            AutoCrafterBlockEntity.TEMPLATE_START + AutoCrafterBlockEntity.GRID_SIZE; // 20

    public AutoCrafterMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(7));
    }

    public AutoCrafterMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.AUTO_CRAFTER.get(), id, container, data, MACHINE_SLOTS);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new GridSlot(container, row * 3 + col, 40 + col * 18, 17 + row * 18));
            }
        }
        this.addSlot(new OutputSlot(container, AutoCrafterBlockEntity.OUTPUT_SLOT, 112, 35));
        // Sync-only ghosts, parked off-panel: recipe preview + the nine lock templates.
        this.addSlot(new GhostSlot(container, AutoCrafterBlockEntity.PREVIEW_SLOT, -1000, 0));
        for (int i = 0; i < AutoCrafterBlockEntity.GRID_SIZE; i++) {
            this.addSlot(new GhostSlot(container, AutoCrafterBlockEntity.TEMPLATE_START + i, -1000, 0));
        }
        addUpgradeAndPlayerSlots(playerInventory);
    }

    /** True while a lock template is stamped (works on both sides — templates are synced slots). */
    public boolean locked() {
        for (int i = 0; i < AutoCrafterBlockEntity.GRID_SIZE; i++) {
            if (!template(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** The lock-template ghost gating grid slot {@code gridIndex} (EMPTY when unlocked). */
    public ItemStack template(int gridIndex) {
        return this.container.getItem(AutoCrafterBlockEntity.TEMPLATE_START + gridIndex);
    }

    /** The server-written recipe-result ghost (EMPTY when the grid matches no recipe). */
    public ItemStack preview() {
        return this.container.getItem(AutoCrafterBlockEntity.PREVIEW_SLOT);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_TOGGLE_LOCK && this.container instanceof AutoCrafterBlockEntity crafter) {
            crafter.toggleLock();
            return true;
        }
        return false;
    }

    /**
     * A 3×3 grid slot that honours the lock template on BOTH sides (the client reads the synced
     * template ghost, so prediction matches the server's {@code canPlaceMachineItem} gating).
     */
    private class GridSlot extends PredicateSlot {
        private final int gridIndex;

        GridSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.gridIndex = index;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (locked()) {
                ItemStack template = template(this.gridIndex);
                return !template.isEmpty() && ItemStack.isSameItemSameComponents(template, stack);
            }
            return super.mayPlace(stack);
        }
    }
}
