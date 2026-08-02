package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.RoboticArmMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Robotic Arm — a one-block item mover: every {@value #MOVE_INTERVAL} ticks it lifts up to
 * {@code roboticArmStackPerMove} items out of the container <b>behind</b> it (opposite
 * {@link NeroTechMachineBlock#FACING}) and puts them into the container <b>in front</b> of it,
 * spending {@code roboticArmNePerMove} NE per item moved.
 *
 * <p>Both ends are addressed through the vanilla {@link Container} / {@link WorldlyContainer}
 * surface, exactly as a hopper does: the source is read through the face pointing at the arm and
 * the target written through the face pointing back — so machine side configs, furnace sidedness
 * and NeroTech's own per-face routing are all honoured, on every loader, with no loader-specific
 * code (the same handoff surface the Item Sorter's buffers ride).
 *
 * <p>One <b>filter</b> slot (GUI-only, never consumed and never exposed to automation): while it
 * holds an item, only matching items are moved.
 *
 * <p>Cost discipline: two direct neighbour lookups on a 20-tick cadence with a per-arm phase —
 * never a world scan, never a per-tick pass.
 */
public class RoboticArmBlockEntity extends NeroTechMachineBlockEntity {

    /** The GUI-only filter slot (never extracted, never exposed through a face). */
    public static final int FILTER_SLOT = 0;

    /** One transfer pass per second. */
    private static final int MOVE_INTERVAL = 20;

    /** No face ever exposes the filter slot — the arm's inventory is not a buffer. */
    private static final int[] NO_SLOTS = new int[0];

    /** Spreads transfer passes across ticks so arms never all fire on the same tick. */
    private final int movePhase = Math.floorMod(System.identityHashCode(this), MOVE_INTERVAL);

    public RoboticArmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROBOTIC_ARM.get(), pos, state, 1);
        // Pure NE sink as far as side config is concerned: the items it moves are never *its* items.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel) || (level.getGameTime() + this.movePhase) % MOVE_INTERVAL != 0) {
            return;
        }
        Direction facing = state.getValue(NeroTechMachineBlock.FACING);
        // Source sits behind the arm and is read through ITS face that points at us (facing);
        // the target sits in front and is written through the face pointing back (facing.opposite).
        Container source = HopperBlockEntity.getContainerAt(level, pos.relative(facing.getOpposite()));
        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
        if (source == null || target == null) {
            reportStatus(MachineStatus.STARVED);
            parked();
            return;
        }

        UpgradeModifiers mods = modifiers();
        int perItem = (int) Math.max(0, Math.round(NeroTechConfig.roboticArmNePerMove()
                * mods.energyMultiplier() * presetEnergyFactor()));
        int budget = (int) Math.max(1, Math.round(NeroTechConfig.roboticArmStackPerMove()
                * mods.speedMultiplier() * presetSpeedFactor()));
        if (perItem > 0) {
            // Never start a pass the buffer cannot pay for; move only as many items as it can afford.
            long affordable = getEnergy().getAmount() / perItem;
            if (affordable <= 0) {
                reportStatus(MachineStatus.NO_ENERGY);
                parked();
                return;
            }
            budget = (int) Math.min(budget, affordable);
        }

        int moved = transfer(source, target, facing, budget);
        if (moved <= 0) {
            // Nothing matched the filter, the source was empty, or the target had no room.
            reportStatus(source.isEmpty() ? MachineStatus.STARVED : MachineStatus.BLOCKED);
            parked();
            return;
        }

        energyBuffer().consume(perItem * moved);
        addHeat(NeroTechConfig.heatPerOperation());
        source.setChanged();
        target.setChanged();
        setChanged();
        setActive(true);
        this.progress = 1;
        this.maxProgress = 1;
        // BER surface: one swing per completed transfer pass (synced pulse counter).
        pulseClient();
    }

    /** Idle state: no work bar, no arm swing. */
    private void parked() {
        setActive(false);
        if (this.progress != 0 || this.maxProgress != 0) {
            this.progress = 0;
            this.maxProgress = 0;
        }
    }

    /**
     * Move up to {@code budget} filtered items from {@code source} to {@code target}, honouring both
     * ends' {@link WorldlyContainer} sidedness.
     *
     * @param facing the arm's facing — the source's exposed face, and the reverse of the target's
     * @return the number of items actually moved
     */
    private int transfer(Container source, Container target, Direction facing, int budget) {
        ItemStack filter = this.items.get(FILTER_SLOT);
        int moved = 0;
        for (int slot : slotsFor(source, facing)) {
            if (moved >= budget) {
                break;
            }
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty() || (!filter.isEmpty() && !ItemStack.isSameItem(stack, filter))) {
                continue;
            }
            if (source instanceof WorldlyContainer worldly
                    && !worldly.canTakeItemThroughFace(slot, stack, facing)) {
                continue;
            }
            int wanted = Math.min(budget - moved, stack.getCount());
            int accepted = insert(target, stack, wanted, facing.getOpposite());
            if (accepted > 0) {
                stack.shrink(accepted);
                if (stack.isEmpty()) {
                    source.setItem(slot, ItemStack.EMPTY);
                }
                moved += accepted;
            }
        }
        return moved;
    }

    /** Insert up to {@code count} of {@code stack} into {@code target} through {@code side}. */
    private static int insert(Container target, ItemStack stack, int count, Direction side) {
        int inserted = 0;
        for (int slot : slotsFor(target, side)) {
            if (inserted >= count) {
                break;
            }
            ItemStack existing = target.getItem(slot);
            int limit = Math.min(target.getMaxStackSize(), stack.getMaxStackSize());
            if (existing.isEmpty()) {
                if (!accepts(target, slot, stack, side)) {
                    continue;
                }
                int put = Math.min(count - inserted, limit);
                target.setItem(slot, stack.copyWithCount(put));
                inserted += put;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < limit) {
                if (!accepts(target, slot, stack, side)) {
                    continue;
                }
                int put = Math.min(count - inserted, limit - existing.getCount());
                existing.grow(put);
                inserted += put;
            }
        }
        return inserted;
    }

    private static boolean accepts(Container target, int slot, ItemStack stack, Direction side) {
        if (!target.canPlaceItem(slot, stack)) {
            return false;
        }
        return !(target instanceof WorldlyContainer worldly)
                || worldly.canPlaceItemThroughFace(slot, stack, side);
    }

    /** The slot indices {@code container} exposes on {@code side} (all of them when unsided). */
    private static int[] slotsFor(Container container, Direction side) {
        if (container instanceof WorldlyContainer worldly) {
            return worldly.getSlotsForFace(side);
        }
        int[] all = new int[container.getContainerSize()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return all;
    }

    // --- container surface: the filter is GUI-only, never automatable ---------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FILTER_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.robotic_arm");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RoboticArmMenu(containerId, playerInventory, this, this.data);
    }
}
