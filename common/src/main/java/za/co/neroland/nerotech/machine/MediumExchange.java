package za.co.neroland.nerotech.machine;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.platform.FluidLookup;
import za.co.neroland.nerolandcore.platform.GasLookup;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

/**
 * The world side of {@link MediumTransfer}: hands a machine's fluid or gas to, or draws it from, the
 * blocks touching it through Core's {@link FluidLookup} and {@link GasLookup} seams (a Process Tank,
 * Core's Gas Tank, another machine, any mod's tank on the same capability). Faces are chosen by the
 * machine's own FLUID or GAS side configuration; a machine with no such channel exchanges nothing.
 * Callers run it every {@link MediumTransfer#INTERVAL_TICKS} ticks, never per tick. Only unloaded-safe
 * neighbours are queried. Moves medium only; no player data.
 */
public final class MediumExchange {

    /** Put back medium that left a store but did not land (bypasses input filters). */
    @FunctionalInterface
    public interface FluidRefund {
        void refund(Fluid fluid, long amount);
    }

    /** The gas twin of {@link FluidRefund}. */
    @FunctionalInterface
    public interface GasRefund {
        void refund(Identifier gas, long amount);
    }

    private MediumExchange() {
    }

    @Nullable
    private static SideMode mode(@Nullable SideConfigComponent config, Channel channel, Direction side) {
        if (config == null || !config.config().has(channel)) {
            return null;
        }
        return config.config().modeAbsolute(channel, config.facing(), side);
    }

    /**
     * Push {@code own}'s fluid out of every face whose mode passes {@code rule}, skipping neighbours
     * {@code skip} names (their own pull decides what they take).
     *
     * @return mB moved
     */
    public static long pushFluid(Level level, BlockPos pos, @Nullable SideConfigComponent config,
            NeroFluidStorage own, FluidRefund refund, Predicate<SideMode> rule,
            Predicate<BlockEntity> skip) {
        long total = 0L;
        for (Direction side : Direction.values()) {
            if (own.getAmount() <= 0L) {
                break;
            }
            SideMode mode = mode(config, Channel.FLUID, side);
            if (mode == null || !rule.test(mode)) {
                continue;
            }
            BlockPos at = pos.relative(side);
            if (!level.isLoaded(at) || skip.test(level.getBlockEntity(at))) {
                continue;
            }
            NeroFluidStorage sink = FluidLookup.INSTANCE.find(level, at, side.getOpposite());
            Fluid fluid = own.getFluid();
            if (sink == null || sink == own || fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            total += MediumTransfer.move(fluidStore(own, fluid, refund), fluidStore(sink, fluid, null),
                    MediumTransfer.PER_FACE_BUDGET_MB);
        }
        return total;
    }

    /**
     * Pull fluid into {@code own} through every face whose mode passes {@code rule}, taking only a
     * fluid {@code wants} accepts.
     *
     * @return mB moved
     */
    public static long pullFluid(Level level, BlockPos pos, @Nullable SideConfigComponent config,
            NeroFluidStorage own, Predicate<SideMode> rule, Predicate<Fluid> wants,
            Predicate<BlockEntity> skip) {
        long total = 0L;
        for (Direction side : Direction.values()) {
            if (own.getAmount() >= own.getCapacity()) {
                break;
            }
            SideMode mode = mode(config, Channel.FLUID, side);
            if (mode == null || !rule.test(mode)) {
                continue;
            }
            BlockPos at = pos.relative(side);
            if (!level.isLoaded(at) || skip.test(level.getBlockEntity(at))) {
                continue;
            }
            NeroFluidStorage source = FluidLookup.INSTANCE.find(level, at, side.getOpposite());
            if (source == null || source == own || source.getAmount() <= 0L) {
                continue;
            }
            Fluid fluid = source.getFluid();
            if (fluid == null || fluid == Fluids.EMPTY || !wants.test(fluid)) {
                continue;
            }
            total += MediumTransfer.move(fluidStore(source, fluid, null), fluidStore(own, fluid, null),
                    MediumTransfer.PER_FACE_BUDGET_MB);
        }
        return total;
    }

    /** The gas twin of {@link #pushFluid}. */
    public static long pushGas(Level level, BlockPos pos, @Nullable SideConfigComponent config,
            NeroGasStorage own, GasRefund refund, Predicate<SideMode> rule,
            Predicate<BlockEntity> skip) {
        long total = 0L;
        for (Direction side : Direction.values()) {
            if (own.getAmount() <= 0L) {
                break;
            }
            SideMode mode = mode(config, Channel.GAS, side);
            if (mode == null || !rule.test(mode)) {
                continue;
            }
            BlockPos at = pos.relative(side);
            if (!level.isLoaded(at) || skip.test(level.getBlockEntity(at))) {
                continue;
            }
            NeroGasStorage sink = GasLookup.INSTANCE.find(level, at, side.getOpposite());
            Identifier gas = own.getGas();
            if (sink == null || sink == own || gas == null || NeroGases.isEmpty(gas)) {
                continue;
            }
            total += MediumTransfer.move(gasStore(own, gas, refund), gasStore(sink, gas, null),
                    MediumTransfer.PER_FACE_BUDGET_MB);
        }
        return total;
    }

    /** The gas twin of {@link #pullFluid}. */
    public static long pullGas(Level level, BlockPos pos, @Nullable SideConfigComponent config,
            NeroGasStorage own, Predicate<SideMode> rule, Predicate<Identifier> wants,
            Predicate<BlockEntity> skip) {
        long total = 0L;
        for (Direction side : Direction.values()) {
            if (own.getAmount() >= own.getCapacity()) {
                break;
            }
            SideMode mode = mode(config, Channel.GAS, side);
            if (mode == null || !rule.test(mode)) {
                continue;
            }
            BlockPos at = pos.relative(side);
            if (!level.isLoaded(at) || skip.test(level.getBlockEntity(at))) {
                continue;
            }
            NeroGasStorage source = GasLookup.INSTANCE.find(level, at, side.getOpposite());
            if (source == null || source == own || source.getAmount() <= 0L) {
                continue;
            }
            Identifier gas = source.getGas();
            if (gas == null || NeroGases.isEmpty(gas) || !wants.test(gas)) {
                continue;
            }
            total += MediumTransfer.move(gasStore(source, gas, null), gasStore(own, gas, null),
                    MediumTransfer.PER_FACE_BUDGET_MB);
        }
        return total;
    }

    private static MediumTransfer.Store fluidStore(NeroFluidStorage storage, Fluid fluid,
            @Nullable FluidRefund refund) {
        return new MediumTransfer.Store() {
            @Override
            public long drain(long max, boolean simulate) {
                return storage.drain(max, simulate);
            }

            @Override
            public long fill(long amount, boolean simulate) {
                return storage.fill(fluid, amount, simulate);
            }

            @Override
            public void refund(long amount) {
                if (refund != null) {
                    refund.refund(fluid, amount);
                } else {
                    storage.fill(fluid, amount, false);
                }
            }
        };
    }

    private static MediumTransfer.Store gasStore(NeroGasStorage storage, Identifier gas,
            @Nullable GasRefund refund) {
        return new MediumTransfer.Store() {
            @Override
            public long drain(long max, boolean simulate) {
                return storage.drain(max, simulate);
            }

            @Override
            public long fill(long amount, boolean simulate) {
                return storage.fill(gas, amount, simulate);
            }

            @Override
            public void refund(long amount) {
                if (refund != null) {
                    refund.refund(gas, amount);
                } else {
                    storage.fill(gas, amount, false);
                }
            }
        };
    }
}
