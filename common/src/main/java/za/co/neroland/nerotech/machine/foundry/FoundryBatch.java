package za.co.neroland.nerotech.machine.foundry;

/**
 * The Blast Foundry's batch rules, pure (no Minecraft types) so they are unit-testable: how many
 * recipe units a cycle runs for a given shell size, what one batch costs, and the batch state a
 * controller carries between ticks.
 *
 * <p><b>Inputs are never taken up front.</b> A batch only <i>commits</i> a unit count when it starts;
 * the inputs stay in their slots until the batch completes, when {@link #settle} pays out for the
 * units whose inputs are <i>still there</i>. Breaking the shell (or swapping the inputs) mid-batch
 * therefore just {@link #abort}s: nothing is consumed and nothing is produced, so an input is either
 * kept or turned into output, never both.
 */
public final class FoundryBatch {

    /** The two shell sizes the foundry forms at. */
    public static final int SMALL = 3;
    public static final int LARGE = 5;

    private int units;
    private int progress;

    /**
     * Recipe units one cycle may run: {@code baseUnits} for a 3³ shell, {@code baseUnits * largeMultiplier}
     * for a 5³, and 0 for anything else (unformed).
     */
    public static int maxUnits(int formedSize, int baseUnits, int largeMultiplier) {
        int base = Math.max(1, baseUnits);
        return switch (formedSize) {
            case SMALL -> base;
            case LARGE -> base * Math.max(1, largeMultiplier);
            default -> 0;
        };
    }

    /**
     * Units a batch runs: no more than the shell allows, the inputs cover, or the outputs have room for.
     * 0 means the batch cannot run.
     */
    public static int unitsFor(int maxUnits, int inputMultiples, int outputMultiples) {
        return Math.max(0, Math.min(maxUnits, Math.min(inputMultiples, outputMultiples)));
    }

    /** Energy per tick for {@code units} units: proportional, so a bigger batch costs its size in energy. */
    public static long energyPerTick(long perUnit, int units) {
        return Math.max(0L, perUnit) * Math.max(0, units);
    }

    /** Whether a batch is in progress. */
    public boolean running() {
        return this.units > 0;
    }

    /** Units committed to the batch in progress (0 when idle). */
    public int units() {
        return this.units;
    }

    /** Ticks worked on the batch in progress. */
    public int progress() {
        return this.progress;
    }

    /** Commit a new batch of {@code units} units. Nothing is consumed here. */
    public void start(int units) {
        this.units = Math.max(0, units);
        this.progress = 0;
    }

    /** Work one tick; true once the batch has had {@code ticksNeeded} ticks. */
    public boolean advance(int ticksNeeded) {
        if (this.units <= 0) {
            return false;
        }
        this.progress++;
        return this.progress >= Math.max(1, ticksNeeded);
    }

    /**
     * Complete the batch: returns how many units to consume <i>and</i> produce, which is the committed
     * count capped by what is still present now (a player may have taken inputs out mid-batch). The
     * batch is cleared either way.
     */
    public int settle(int inputMultiplesNow, int outputMultiplesNow) {
        int paid = unitsFor(this.units, inputMultiplesNow, outputMultiplesNow);
        clear();
        return paid;
    }

    /** Abandon the batch (shell broken, recipe changed): nothing consumed, nothing made. */
    public void abort() {
        clear();
    }

    /** Restore from a save; zero or negative units read as idle. */
    public void restore(int units, int progress) {
        if (units <= 0) {
            clear();
            return;
        }
        this.units = units;
        this.progress = Math.max(0, progress);
    }

    /** Fill of the furnace for the sight glass: committed units over the shell's maximum (0..1). */
    public static float fill(int units, int maxUnits) {
        if (units <= 0 || maxUnits <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) units / maxUnits);
    }

    private void clear() {
        this.units = 0;
        this.progress = 0;
    }
}
