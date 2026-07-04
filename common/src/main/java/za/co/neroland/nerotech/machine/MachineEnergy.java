package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;

/**
 * Shared energy-distribution helper for NeroTech generators/machines. Pushes stored NE from a
 * machine's {@link EnergyBuffer} into adjacent {@link NeroEnergyStorage} sinks discovered through
 * Core's loader-neutral {@link EnergyLookup} seam — so generation stays thin and talks only to
 * Core's energy surface (never to NeroTech-internal machine classes), keeping a future NeroPower
 * extraction a package move rather than a rewrite.
 */
public final class MachineEnergy {

    private MachineEnergy() {
    }

    /**
     * Distribute up to {@code perSideBudget} NE to each neighbouring sink on every side. Returns total
     * NE moved. Legacy ungated behaviour — used when a machine has no ENERGY side configuration.
     */
    public static long pushToNeighbours(Level level, BlockPos pos, EnergyBuffer buffer, long perSideBudget) {
        return pushToNeighbours(level, pos, buffer, perSideBudget, null);
    }

    /**
     * Side-config-aware push: power leaves a side only when the machine's ENERGY channel resolves to an
     * extractable mode (OUTPUT / IO / PUSH) for that absolute face. When {@code sideConfig} is null (no
     * ENERGY side configuration) every side is eligible, preserving legacy behaviour. Returns total NE
     * moved.
     */
    public static long pushToNeighbours(Level level, BlockPos pos, EnergyBuffer buffer, long perSideBudget,
            @Nullable SideConfigComponent sideConfig) {
        if (perSideBudget <= 0 || buffer.getAmount() <= 0) {
            return 0L;
        }
        boolean gated = sideConfig != null && sideConfig.config().has(Channel.ENERGY);
        long moved = 0L;
        for (Direction side : Direction.values()) {
            if (buffer.getAmount() <= 0) {
                break;
            }
            if (gated && !sideConfig.config()
                    .modeAbsolute(Channel.ENERGY, sideConfig.facing(), side).canExtract()) {
                continue;
            }
            NeroEnergyStorage neighbour = EnergyLookup.INSTANCE.find(level, pos.relative(side), side.getOpposite());
            if (neighbour == null || !neighbour.canReceive()) {
                continue;
            }
            long offer = buffer.extract(perSideBudget, true);
            if (offer <= 0) {
                continue;
            }
            long accepted = neighbour.insert(offer, false);
            if (accepted > 0) {
                buffer.extract(accepted, false);
                moved += accepted;
            }
        }
        return moved;
    }
}
