package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Shared base for NeroTech's recipe-driven processing machines (Ore Processor, Fabricator). One input
 * slot, one output slot. Each tick it consumes NE (scaled down by Efficiency modules) to advance a
 * progress timer (shortened by Speed modules); on completion it consumes one input and yields the
 * recipe result. Pure NE sink — it receives power pushed by generators through Core's energy seam.
 */
public abstract class AbstractProcessingBlockEntity extends NeroTechMachineBlockEntity {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    protected AbstractProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2);
    }

    /** The recipe result for an input stack (single output), or EMPTY if there is no recipe. */
    protected abstract ItemStack resultFor(ItemStack input);

    @Override
    protected void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack input = this.items.get(INPUT_SLOT);
        ItemStack result = resultFor(input);

        if (result.isEmpty() || !canOutput(result)) {
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }

        UpgradeModifiers mods = modifiers();
        int effectiveTicks = Math.max(1,
                (int) Math.round(NeroTechConfig.machineBaseProcessTicks() / Math.max(0.01D, mods.speedMultiplier())));
        int cost = (int) Math.max(0, Math.round(NeroTechConfig.machineNePerTick() * mods.energyMultiplier()));
        this.maxProgress = effectiveTicks;

        if (energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            this.progress++;
            if (this.progress >= effectiveTicks) {
                craft(result);
                this.progress = 0;
            }
            setChanged();
        }
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result.copy());
        } else {
            out.grow(result.getCount());
        }
        this.items.get(INPUT_SLOT).shrink(1);
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && !resultFor(stack).isEmpty();
    }
}
