package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.ColliderMenu;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Collider Core — the controller of the Particle Collider multiblock (Stage B) and NeroTech's
 * <b>standalone</b> route to space-grade dusts. It transmutes a single catalyst into Starsteel or
 * Void Crystal dust ({@code nerotech:collider} recipes) at a deliberately enormous cost:
 * {@code colliderNePerTick} for {@code colliderOperationTicks} ticks per operation, at triple the
 * base heat rate. The point is that the collider is never the *cheap* source — with Nerospace
 * installed, meteor mining stays faster; the collider is the route that needs no other mod.
 *
 * <p><b>Inert until formed</b>: the core must sit on a horizontal hollow ring of Accelerator Coils
 * (5×5 or 7×7 — see {@link ColliderStructure}), and an unformed core reports {@code UNFORMED} and
 * does no work at all. Ring size is the throughput axis: the 7×7 loop halves the operation time.
 *
 * <p>Structure validation is event-driven plus cadenced — {@link #invalidateStructure()} on a
 * neighbour change and a bounded re-check every second while unformed / every five while formed —
 * so a running collider never sweeps its ring per tick.
 */
public class ColliderCoreBlockEntity extends AbstractProcessingBlockEntity {

    /** Structure re-validation cadence (ticks): eager while unformed, demolition-check while formed. */
    private static final int RECHECK_UNFORMED = 20;
    private static final int RECHECK_FORMED = 100;

    /** The collider runs far hotter than a Tier-1 processor: triple the base heat per working tick. */
    private static final int HEAT_MULTIPLIER = 3;

    /** Formed state (persisted so it survives a reload, and rides the BE update tag). */
    private boolean formed;
    private int ringSize;

    /** Set by a neighbour change so the very next tick re-validates instead of waiting for the cadence. */
    private boolean structureDirty = true;

    public ColliderCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLIDER_CORE.get(), pos, state);
    }

    @Override
    protected RecipeType<MachineRecipe> recipeType() {
        return ModRecipeTypes.COLLIDER.get();
    }

    /** Drop the cached formed state; the next tick re-validates the ring. Called by the block. */
    public void invalidateStructure() {
        this.structureDirty = true;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        revalidateOnCadence(level, pos, state);

        if (!this.formed) {
            // Inert until formed: no beam, no energy draw, no emissions.
            reportStatus(MachineStatus.UNFORMED);
            setActive(false);
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }

        ItemStack input = this.items.get(INPUT_SLOT);
        ItemStack result = resultFor(input);

        if (result.isEmpty() || !canOutput(result)) {
            // Analytics: no usable catalyst reads STARVED; a jammed output slot BLOCKED.
            reportStatus(result.isEmpty() ? MachineStatus.STARVED : MachineStatus.BLOCKED);
            setActive(false);
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }

        UpgradeModifiers mods = modifiers();
        // Ring size, Speed modules and the overclock preset all shorten the operation; the energy
        // cost scales with Efficiency modules and the preset (heat/pollution scale at the base).
        double throughput = ColliderStructure.speedPermille(this.ringSize) / 1000.0D;
        int effectiveTicks = Math.max(1, (int) Math.round(NeroTechConfig.colliderOperationTicks()
                / Math.max(0.01D, throughput * mods.speedMultiplier() * presetSpeedFactor())));
        int cost = (int) Math.max(0,
                Math.round(NeroTechConfig.colliderNePerTick() * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = effectiveTicks;

        // Heat throttle: a collider run too hard stalls until it sheds heat.
        if (overheated()) {
            reportStatus(MachineStatus.THROTTLED);
            setActive(false);
            return;
        }

        setActive(energyBuffer().has(cost));

        if (energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            this.progress++;
            addHeat(NeroTechConfig.heatPerOperation() * HEAT_MULTIPLIER);
            emitPollution(level, pos);
            if (this.progress >= effectiveTicks) {
                craft(result);
                this.progress = 0;
            }
            setChanged();
        } else {
            // Analytics: work is queued but the buffer can't cover the next tick's cost.
            reportStatus(MachineStatus.NO_ENERGY);
        }
    }

    /** Bounded ring re-validation, on a phase-spread cadence or immediately after a neighbour change. */
    private void revalidateOnCadence(Level level, BlockPos pos, BlockState state) {
        int cadence = this.formed ? RECHECK_FORMED : RECHECK_UNFORMED;
        if (!this.structureDirty
                && (level.getGameTime() + Math.floorMod(pos.hashCode(), cadence)) % cadence != 0) {
            return;
        }
        this.structureDirty = false;
        ColliderStructure.Ring ring = ColliderStructure.validate(level, pos);
        // Direct null checks (not via a flag) so ecj's null-flow analysis can track them.
        int nowSize = ring != null ? ring.size() : 0;
        boolean nowFormed = ring != null;
        if (nowFormed == this.formed && nowSize == this.ringSize) {
            return;
        }
        if (this.formed && !nowFormed) {
            // The loop broke mid-run: the beam collapses and the partial operation is lost.
            this.progress = 0;
            this.maxProgress = 0;
        }
        this.formed = nowFormed;
        this.ringSize = nowSize;
        setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    /** Whether the accelerator ring is formed (client-readable via the update tag). */
    public boolean renderFormed() {
        return this.formed;
    }

    /** The formed ring's outer edge size (5/7), or 0 when unformed. */
    public int renderRingSize() {
        return this.ringSize;
    }

    // --- persistence (formed state rides saveAdditional and therefore the update tag) ------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Formed", this.formed);
        output.putInt("RingSize", this.ringSize);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.formed = input.getBooleanOr("Formed", false);
        this.ringSize = input.getIntOr("RingSize", 0);
        this.structureDirty = true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.collider_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ColliderMenu(containerId, playerInventory, this, this.data);
    }
}
