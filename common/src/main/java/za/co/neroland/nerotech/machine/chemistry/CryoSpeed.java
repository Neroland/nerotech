package za.co.neroland.nerotech.machine.chemistry;

/**
 * Pure rule for the Cryo Distiller's cold bonus: the colder the ambient heat
 * ({@code ThermalEnvironment.ambientAt}, cached by the machine base), the faster it runs. The factor
 * is {@code 1 - ambient * perHeatPermille / 1000}, clamped to {@code [minPermille, maxPermille] / 1000}.
 * With the defaults (5, 500, 2000): ambient 0 → 1.0, Glacira's −80 → 1.4, −200 or colder → 2.0 (cap),
 * the Nether's 150 → 0.5 (floor).
 */
public final class CryoSpeed {

    private CryoSpeed() {
    }

    /** The speed multiplier for {@code ambient} heat. Never below the floor, never above the cap. */
    public static double factor(int ambient, int perHeatPermille, int minPermille, int maxPermille) {
        double min = Math.max(1, minPermille) / 1000.0D;
        double max = Math.max(min, maxPermille / 1000.0D);
        double raw = 1.0D - (double) ambient * Math.max(0, perHeatPermille) / 1000.0D;
        return Math.max(min, Math.min(max, raw));
    }

    /** {@link #factor(int, int, int, int)} with the current {@link ChemistrySettings}. */
    public static double factor(int ambient) {
        return factor(ambient, ChemistrySettings.cryoColdBonusPerHeatPermille(), ChemistrySettings.cryoSpeedMinPermille(),
                ChemistrySettings.cryoSpeedMaxPermille());
    }
}
