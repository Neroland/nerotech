package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.menu.ItemSorterMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Item Sorter — routes items by filter into per-face output buffers. The input slot accepts items from
 * the top/bottom; each horizontal face has a filter slot and an output buffer, and items matching a
 * face's filter are moved into that face's buffer, where a pipe/NeroLogistics on that side extracts them
 * (a directional sorter, no neighbour scanning needed). Demand-driven on a batched interval.
 *
 * <p>Slots: 0 = input, 1–4 = filters (N/E/S/W, GUI-only), 5–8 = output buffers (N/E/S/W, face-exposed).
 */
public class ItemSorterBlockEntity extends NeroTechMachineBlockEntity {

    public static final int INPUT_SLOT = 0;
    public static final int FILTER_START = 1;
    public static final int BUFFER_START = 5;
    private static final int SLOTS = 9;
    private static final int MOVE_PER_CYCLE = 16;

    private final int interval = 8 + Math.floorMod(System.identityHashCode(this), 8);

    public ItemSorterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_SORTER.get(), pos, state, SLOTS);
    }

    /** Buffer slot exposed on a horizontal face, or -1 for up/down. */
    private static int bufferForSide(Direction side) {
        return switch (side) {
            case NORTH -> BUFFER_START;
            case EAST -> BUFFER_START + 1;
            case SOUTH -> BUFFER_START + 2;
            case WEST -> BUFFER_START + 3;
            default -> -1;
        };
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % interval != 0) {
            return;
        }
        ItemStack in = this.items.get(INPUT_SLOT);
        if (in.isEmpty()) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            ItemStack filter = this.items.get(FILTER_START + i);
            if (filter.isEmpty() || !ItemStack.isSameItem(in, filter)) {
                continue;
            }
            if (moveInto(BUFFER_START + i, in)) {
                setChanged();
            }
            return; // one routing action per cycle keeps this cheap
        }
    }

    private boolean moveInto(int bufferSlot, ItemStack input) {
        ItemStack buffer = this.items.get(bufferSlot);
        int moved;
        if (buffer.isEmpty()) {
            moved = Math.min(MOVE_PER_CYCLE, Math.min(input.getCount(), input.getMaxStackSize()));
            if (moved <= 0) {
                return false;
            }
            this.items.set(bufferSlot, input.copyWithCount(moved));
        } else if (ItemStack.isSameItemSameComponents(buffer, input)) {
            int space = buffer.getMaxStackSize() - buffer.getCount();
            moved = Math.min(MOVE_PER_CYCLE, Math.min(input.getCount(), space));
            if (moved <= 0) {
                return false;
            }
            buffer.grow(moved);
        } else {
            return false;
        }
        input.shrink(moved);
        return true;
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        // Input + the four filter slots are placeable (filters are GUI config); buffers are output-only.
        return slot == INPUT_SLOT || (slot >= FILTER_START && slot < BUFFER_START);
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot >= BUFFER_START && slot < SLOTS;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int buffer = bufferForSide(side);
        // Horizontal faces expose their own buffer (extract); top/bottom expose the input (insert).
        return buffer >= 0 ? new int[] { buffer } : new int[] { INPUT_SLOT };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= BUFFER_START && slot < SLOTS;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.item_sorter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ItemSorterMenu(containerId, playerInventory, this, this.data);
    }
}
