package za.co.neroland.nerotech.machine.heat;

import java.util.HashMap;
import java.util.Map;

import za.co.neroland.nerotech.heat.ThermalMath;

/**
 * The Heat Exchanger's rule, pure so it is tested without a bootstrap. What the tank holds
 * decides the direction:
 *
 * <ul>
 *   <li><b>COOLANT</b> (cryo coolant, nitrogen): machine to tank only. The coolant absorbs the machine's
 *       heat above ambient and is used up, {@code heatPerMb} heat per mB. The heat is gone.</li>
 *   <li><b>HOT</b> (lava): tank to machine only, while the machine is below the medium's temperature. The
 *       medium is used up, {@code heatPerMb} heat per mB.</li>
 *   <li><b>STORE</b> (water and every other medium): the tank's own heat is the medium's temperature. Heat
 *       conducts from the hotter side to the cooler one, either direction, so a tank column heated by a
 *       reactor's exchanger feeds a Foundry's exchanger on the same column.</li>
 * </ul>
 *
 * <p>Never heat to energy.
 */
public final class ExchangerModel {

    /** Heat moving from the machine into the tank. */
    public static final int TO_TANK = 1;
    /** Heat moving from the tank into the machine. */
    public static final int TO_MACHINE = -1;
    /** Nothing moving. */
    public static final int NONE = 0;

    private ExchangerModel() {
    }

    /** What a tank medium does in an exchanger. */
    public enum Role {
        COOLANT, HOT, STORE
    }

    /** A medium's role and its heat per mB (unused for {@link Role#STORE}). */
    public record Medium(Role role, int heatPerMb) {
    }

    /**
     * One exchange's plan.
     *
     * @param direction {@link #TO_TANK}, {@link #TO_MACHINE} or {@link #NONE}
     * @param heat      heat to move (or to absorb, for coolant)
     * @param mediumMb  mB of medium used up (0 for {@link Role#STORE})
     */
    public record Plan(int direction, int heat, long mediumMb) {

        public static final Plan IDLE = new Plan(NONE, 0, 0L);
    }

    /** Parse a {@code id=heatPerMb} comma list into a map; malformed entries are skipped. */
    public static Map<String, Integer> parseTable(String raw) {
        Map<String, Integer> table = new HashMap<>();
        if (raw == null) {
            return table;
        }
        for (String pair : raw.split(",")) {
            String entry = pair.trim();
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                int value = Integer.parseInt(entry.substring(eq + 1).trim());
                if (value > 0) {
                    table.put(entry.substring(0, eq).trim(), value);
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return table;
    }

    /** The role of medium {@code id} given the coolant and hot tables; anything else stores heat. */
    public static Medium classify(String id, Map<String, Integer> coolants, Map<String, Integer> hot) {
        Integer cool = coolants.get(id);
        if (cool != null) {
            return new Medium(Role.COOLANT, cool);
        }
        Integer warm = hot.get(id);
        if (warm != null) {
            return new Medium(Role.HOT, warm);
        }
        return new Medium(Role.STORE, 0);
    }

    /**
     * Plan one exchange.
     *
     * @param medium         the tank medium's role
     * @param mediumMb       mB of medium in the tank column
     * @param machineHeat    the machine's heat
     * @param tankHeat       the tank's own heat (the stored medium's temperature)
     * @param ambient        ambient heat at the exchanger (coolant never cools below it)
     * @param hotTemperature temperature a HOT medium heats a machine up to
     * @param maxPerOp       most heat one exchange moves
     * @param conductivity   permille of the difference a STORE exchange moves
     */
    public static Plan plan(Medium medium, long mediumMb, int machineHeat, int tankHeat, int ambient,
            int hotTemperature, int maxPerOp, int conductivity) {
        if (medium == null || mediumMb <= 0 || maxPerOp <= 0) {
            return Plan.IDLE;
        }
        switch (medium.role()) {
            case COOLANT: {
                long excess = (long) machineHeat - ambient;
                if (excess <= 0) {
                    return Plan.IDLE;
                }
                long affordable = mediumMb * medium.heatPerMb();
                int heat = (int) Math.min(Math.min(excess, maxPerOp), affordable);
                if (heat <= 0) {
                    return Plan.IDLE;
                }
                return new Plan(TO_TANK, heat, mbFor(heat, medium.heatPerMb()));
            }
            case HOT: {
                long room = (long) hotTemperature - machineHeat;
                if (room <= 0) {
                    return Plan.IDLE;
                }
                long affordable = mediumMb * medium.heatPerMb();
                int heat = (int) Math.min(Math.min(room, maxPerOp), affordable);
                if (heat <= 0) {
                    return Plan.IDLE;
                }
                return new Plan(TO_MACHINE, heat, mbFor(heat, medium.heatPerMb()));
            }
            default: {
                int step = ThermalMath.conductionStep(machineHeat, tankHeat, conductivity);
                if (step > 0) {
                    return new Plan(TO_TANK, Math.min(step, maxPerOp), 0L);
                }
                if (step < 0) {
                    return new Plan(TO_MACHINE, Math.min(-step, maxPerOp), 0L);
                }
                return Plan.IDLE;
            }
        }
    }

    /** Move up to {@code amount} from {@code from} to {@code to}, exactly; whatever {@code to} refuses goes back. */
    public static int transfer(HeatRunMath.Node from, HeatRunMath.Node to, int amount) {
        int taken = from.take(amount);
        if (taken <= 0) {
            return 0;
        }
        int given = to.give(taken);
        if (given < taken) {
            from.give(taken - given);
        }
        return given;
    }

    /** mB used up to move {@code heat} at {@code heatPerMb} (rounded up: never free heat). */
    public static long mbFor(int heat, int heatPerMb) {
        if (heat <= 0 || heatPerMb <= 0) {
            return 0L;
        }
        return ((long) heat + heatPerMb - 1) / heatPerMb;
    }
}
