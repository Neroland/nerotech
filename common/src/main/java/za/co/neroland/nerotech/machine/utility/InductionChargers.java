package za.co.neroland.nerotech.machine.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Per-level registry of loaded Induction Chargers, so {@code api.WirelessCharging.offer} finds the
 * chargers near a point without scanning the world. A charger registers on its first server tick after
 * load and is dropped in {@code setRemoved} (unload or break). Dropping is a map edit only and touches no
 * world state, as {@code setRemoved} must on 26.x. Positions are world data; no player data is held.
 */
public final class InductionChargers {

    private static final Map<Level, Map<Long, InductionChargerBlockEntity>> BY_LEVEL = new WeakHashMap<>();

    private InductionChargers() {
    }

    static void register(Level level, BlockPos pos, InductionChargerBlockEntity charger) {
        synchronized (BY_LEVEL) {
            BY_LEVEL.computeIfAbsent(level, l -> new HashMap<>()).put(pos.asLong(), charger);
        }
    }

    static void unregister(Level level, BlockPos pos, InductionChargerBlockEntity charger) {
        synchronized (BY_LEVEL) {
            Map<Long, InductionChargerBlockEntity> byPos = BY_LEVEL.get(level);
            if (byPos != null && byPos.get(pos.asLong()) == charger) {
                byPos.remove(pos.asLong());
                if (byPos.isEmpty()) {
                    BY_LEVEL.remove(level);
                }
            }
        }
    }

    /** The live chargers in {@code level} whose reach covers {@code near}, nearest first. */
    public static List<InductionChargerBlockEntity> reaching(Level level, BlockPos near) {
        List<InductionChargerBlockEntity> out = new ArrayList<>();
        int radius = UtilitySettings.inductionChargerRadius();
        synchronized (BY_LEVEL) {
            Map<Long, InductionChargerBlockEntity> byPos = BY_LEVEL.get(level);
            if (byPos == null) {
                return out;
            }
            for (InductionChargerBlockEntity charger : byPos.values()) {
                BlockPos at = charger.getBlockPos();
                if (!charger.isRemoved() && ChargeMath.inBox(at.getX(), at.getY(), at.getZ(),
                        near.getX(), near.getY(), near.getZ(), radius)) {
                    out.add(charger);
                }
            }
        }
        out.sort((a, b) -> Double.compare(a.getBlockPos().distSqr(near), b.getBlockPos().distSqr(near)));
        return out;
    }

    /** Loaded chargers in {@code level} (diagnostics and tests of the wiring). */
    public static int count(Level level) {
        synchronized (BY_LEVEL) {
            Map<Long, InductionChargerBlockEntity> byPos = BY_LEVEL.get(level);
            return byPos == null ? 0 : byPos.size();
        }
    }
}
