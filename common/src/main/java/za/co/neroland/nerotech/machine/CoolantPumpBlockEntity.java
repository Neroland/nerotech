package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * Coolant Pump — the <b>active</b> half of the Stage C coolant loop. On each thermal exchange
 * interval it spends NE to pull heat out of every adjacent machine that is above ambient and
 * <b>delete</b> it: that deletion is the loop, and the {@link ModBlocks#RADIATOR Radiator} blocks
 * feeding it are what make it strong. Each Radiator found within {@value #RADIATOR_REACH} blocks in
 * a straight line on any of the six axes adds one multiple of {@code coolantPumpHeatPerOp} to the
 * pull rate.
 *
 * <p>This is what gives Fusion Reactors and Particle Colliders <i>active</i> cooling instead of the
 * passive ice-block walls the base thermal model offers.
 *
 * <p>Cost discipline: the radiator scan runs only after a neighbour change (cached otherwise), and
 * the pump itself only wakes on its phase of {@code thermalExchangeIntervalTicks} — never a per-tick
 * scan. Its NE bill is billed in one batch per exchange ({@code coolantPumpNePerTick x interval}).
 *
 * <p>Slotless and menu-less by design: there is nothing to insert, nothing to configure beyond the
 * config file, and nothing to look at. Break it to see what it was doing.
 */
public class CoolantPumpBlockEntity extends NeroTechMachineBlockEntity {

    /** Straight-line radiator scan reach on each of the six axes (blocks). */
    public static final int RADIATOR_REACH = 3;

    /** Cached radiator count; null means "rescan on the next pump pass" (neighbour change / load). */
    @Nullable
    private Integer radiators;

    /** Spreads pump passes across ticks so pumps never all fire on the same tick. */
    private final int pumpPhase = Math.floorMod(System.identityHashCode(this) * 43, 1024);

    public CoolantPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLANT_PUMP.get(), pos, state, 0);
        // Pure NE sink with no item channel: PROCESSOR preset gives ENERGY input on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    /** Drop the cached radiator count; the next pump pass rescans. Called by the block. */
    public void invalidateRadiators() {
        this.radiators = null;
    }

    /** The cached radiator count (0 when not yet scanned) — the read surface for tooling/tests. */
    public int radiatorCount() {
        Integer cached = this.radiators;
        return cached == null ? 0 : cached.intValue();
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        int interval = Math.max(1, NeroTechConfig.thermalExchangeIntervalTicks());
        if ((level.getGameTime() + this.pumpPhase) % interval != 0) {
            // Between passes the gauges simply hold their last state (the Remediator's recipe).
            return;
        }
        // Local copy so the null-flow stays provable (the field is the cache, not the value).
        Integer cached = this.radiators;
        if (cached == null) {
            cached = Integer.valueOf(scanRadiators(level, pos));
            this.radiators = cached;
        }

        UpgradeModifiers mods = modifiers();
        // The per-tick config rate is billed once per exchange interval — no per-tick bookkeeping.
        int cost = (int) Math.max(0, Math.round((double) NeroTechConfig.coolantPumpNePerTick() * interval
                * mods.energyMultiplier() * presetEnergyFactor()));
        if (!energyBuffer().has(cost)) {
            reportStatus(MachineStatus.NO_ENERGY);
            parked();
            return;
        }

        int perMachine = (int) Math.max(1, Math.round((double) NeroTechConfig.coolantPumpHeatPerOp()
                * (1 + cached.intValue()) * mods.speedMultiplier() * presetSpeedFactor()));
        int floor = ambient(level, pos);

        int removed = 0;
        for (Direction side : Direction.values()) {
            if (level.getBlockEntity(pos.relative(side)) instanceof NeroTechMachineBlockEntity machine
                    && machine != this) {
                removed += machine.extractHeat(perMachine, floor);
            }
        }

        if (removed <= 0) {
            // Nothing hot next door: no energy spent, nothing to show.
            parked();
            return;
        }
        energyBuffer().consume(cost);
        setChanged();
        setActive(true);
        this.progress = 1;
        this.maxProgress = 1;
    }

    /** Idle state: no work bar, no BER animation. */
    private void parked() {
        setActive(false);
        if (this.progress != 0 || this.maxProgress != 0) {
            this.progress = 0;
            this.maxProgress = 0;
        }
    }

    /**
     * Count Radiators in a straight line out to {@value #RADIATOR_REACH} blocks on each of the six
     * axes, stopping at the first non-Radiator. At most 18 block reads, and only after invalidation.
     */
    private static int scanRadiators(Level level, BlockPos pos) {
        int count = 0;
        for (Direction side : Direction.values()) {
            BlockPos.MutableBlockPos cursor = pos.mutable();
            for (int step = 0; step < RADIATOR_REACH; step++) {
                cursor.move(side);
                if (!level.getBlockState(cursor).is(ModBlocks.RADIATOR.get())) {
                    break;
                }
                count++;
            }
        }
        return count;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerotech.coolant_pump");
    }

    /**
     * No GUI: the pump has no slots and nothing to configure in-world. Returning null makes
     * {@code ServerPlayer#openMenu} a no-op; {@code CoolantPumpBlock} passes the interaction through
     * so right-clicking behaves like a plain block.
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
