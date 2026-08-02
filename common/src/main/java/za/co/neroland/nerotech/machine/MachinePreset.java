package za.co.neroland.nerotech.machine;

/**
 * Stage H overclock presets — a free, per-machine GUI selector (no item cost, no gate) with the
 * <b>standard trade curve</b>: pushing speed past 1× costs MORE than linear in energy, heat and
 * pollution, while easing off saves less than linear. Permille integers so every consumer stays in
 * the codebase's int/permille arithmetic:
 *
 * <ul>
 *   <li>{@link #ECO} — speed 750‰, energy 500‰, heat 500‰, pollution 500‰ (25% slower for half
 *       the running cost);</li>
 *   <li>{@link #BALANCED} — all 1000‰ (the exact pre-Stage-H behaviour; the default);</li>
 *   <li>{@link #OVERDRIVE} — speed 1500‰, energy 2000‰, heat 2000‰, pollution 2000‰ (50% faster
 *       for double the running cost).</li>
 * </ul>
 *
 * <p>Speed and energy are applied at each machine's work site (next to its Core
 * {@code UpgradeModifiers} reads); heat and pollution are scaled once at the base, inside
 * {@code NeroTechMachineBlockEntity.addHeat}/{@code emitPollution}. One-shot machines (Auto
 * Crafter, Item Sorter) deliberately take no speed/energy scaling — their single-tick ops have no
 * rate to trade — though their base heat/pollution still scales like everyone else's.
 *
 * <p><b>BER tie-in:</b> the visual telegraph for Overdrive is heat itself — an overdriven machine
 * runs hotter, and the existing heat-fraction glow lerp in every BER shows it. No extra synced
 * render state.
 *
 * <p><b>Wire/persist format:</b> the {@link #ordinal()} — index 6 of the machine
 * {@code ContainerData}, the serverbound {@code MachinePresetPayload} byte, and the saved
 * {@code "Preset"} int. Only append new presets; never reorder.
 */
public enum MachinePreset {

    /** 25% slower, half the energy/heat/pollution. */
    ECO(750, 500, 500, 500),
    /** The 1×-everything default — identical to pre-preset behaviour. */
    BALANCED(1000, 1000, 1000, 1000),
    /** 50% faster for double the energy/heat/pollution (runs visibly hotter). */
    OVERDRIVE(1500, 2000, 2000, 2000);

    /** Stable ordinal-indexed view for clamped wire/persist decoding. */
    public static final MachinePreset[] VALUES = values();

    private final int speedPermille;
    private final int energyPermille;
    private final int heatPermille;
    private final int pollutionPermille;

    MachinePreset(int speedPermille, int energyPermille, int heatPermille, int pollutionPermille) {
        this.speedPermille = speedPermille;
        this.energyPermille = energyPermille;
        this.heatPermille = heatPermille;
        this.pollutionPermille = pollutionPermille;
    }

    /** Work-rate scale (generation output, processing speed, scrub rate) in permille. */
    public int speedPermille() {
        return this.speedPermille;
    }

    /** Energy-cost scale (NE per tick / per op) in permille. */
    public int energyPermille() {
        return this.energyPermille;
    }

    /** Heat-per-operation scale in permille (applied inside {@code addHeat}). */
    public int heatPermille() {
        return this.heatPermille;
    }

    /** Pollution-per-contribution scale in permille (applied inside {@code emitPollution}). */
    public int pollutionPermille() {
        return this.pollutionPermille;
    }

    /** Work-rate scale as a multiplier (multiply outputs / divide durations by this). */
    public double speedFactor() {
        return this.speedPermille / 1000.0D;
    }

    /** Energy-cost scale as a multiplier. */
    public double energyFactor() {
        return this.energyPermille / 1000.0D;
    }

    /** The next preset in the GUI cycle (Eco → Balanced → Overdrive → Eco). */
    public MachinePreset next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    /** Clamped ordinal decode for wire/persist reads (out-of-range falls back to BALANCED). */
    public static MachinePreset byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : BALANCED;
    }

    /** {@code nerotech.preset.<name>} — the full display name ("Eco", "Balanced", "Overdrive"). */
    public String translationKey() {
        return "nerotech.preset." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** {@code nerotech.preset.short.<name>} — the 3-letter GUI button label ("ECO"/"BAL"/"OVR"). */
    public String shortTranslationKey() {
        return "nerotech.preset.short." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
