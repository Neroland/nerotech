package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

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
     * Distribute up to {@code perSideBudget} NE to each neighbouring sink. Returns total NE moved.
     */
    public static long pushToNeighbours(Level level, BlockPos pos, EnergyBuffer buffer, long perSideBudget) {
        if (perSideBudget <= 0 || buffer.getAmount() <= 0) {
            return 0L;
        }
        long moved = 0L;
        for (Direction side : Direction.values()) {
            if (buffer.getAmount() <= 0) {
                break;
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
