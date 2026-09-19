package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.platform.GasLookup;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;

/**
 * Shared gas-distribution helper — the gas twin of {@link MachineEnergy}. Pushes stored gas from a
 * machine's tank into adjacent {@link NeroGasStorage} sinks discovered through Core's
 * loader-neutral {@link GasLookup} seam, so NeroTech's gas machines talk only to Core's gas
 * surface (Core's own Gas Tank, any Nero mod's tank, any third-party block registered on the same
 * capability) and never to NeroTech-internal classes.
 *
 * <p>There is no gas <i>network</i> here by design: this is direct adjacency handoff on a
 * once-per-second cadence, exactly like the generators' energy push. Pipes are NeroLogistics' job.
 */
public final class MachineGas {

    private MachineGas() {
    }

    /**
     * Offer up to {@code perSideBudget} mB to each neighbouring gas sink on every side. Legacy
     * ungated behaviour — used when a machine has no GAS side configuration.
     *
     * @return total mB moved
     */
    public static long pushToNeighbours(Level level, BlockPos pos, NeroGasStorage source,
            long perSideBudget) {
        return pushToNeighbours(level, pos, source, perSideBudget, null);
    }

    /**
     * Side-config-aware push: gas leaves a face only when the machine's GAS channel resolves to an
     * extractable mode (OUTPUT / IO / PUSH) for that absolute face. When {@code sideConfig} is null
     * or declares no GAS channel every side is eligible, preserving legacy behaviour.
     *
     * @return total mB moved
     */
    public static long pushToNeighbours(Level level, BlockPos pos, NeroGasStorage source,
            long perSideBudget, @Nullable SideConfigComponent sideConfig) {
        if (perSideBudget <= 0 || source.getAmount() <= 0) {
            return 0L;
        }
        Identifier gas = source.getGas();
        if (NeroGases.isEmpty(gas)) {
            return 0L;
        }
        SideConfigComponent gate =
                (sideConfig != null && sideConfig.config().has(Channel.GAS)) ? sideConfig : null;
        long moved = 0L;
        for (Direction side : Direction.values()) {
            if (source.getAmount() <= 0) {
                break;
            }
            if (gate != null && !gate.config()
                    .modeAbsolute(Channel.GAS, gate.facing(), side).canExtract()) {
                continue;
            }
            NeroGasStorage sink = GasLookup.INSTANCE.find(level, pos.relative(side), side.getOpposite());
            if (sink == null || sink == source) {
                continue;
            }
            long offer = source.drain(perSideBudget, true);
            if (offer <= 0) {
                continue;
            }
            long accepted = sink.fill(gas, offer, false);
            if (accepted > 0) {
                source.drain(accepted, false);
                moved += accepted;
            }
        }
        return moved;
    }
}
