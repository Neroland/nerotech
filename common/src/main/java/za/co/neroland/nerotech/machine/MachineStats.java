package za.co.neroland.nerotech.machine;

/**
 * The Stage G per-machine analytics window: a fixed 60-slot ring buffer sampled once per second
 * (every 20 ticks on the owning BE's phase-spread pattern) holding heat permille, stored-energy
 * permille and the work-ops delta since the previous sample, plus the current
 * {@link MachineStatus}. One transient instance per {@link NeroTechMachineBlockEntity} —
 * <b>never persisted, never synced to watchers</b>; it only leaves the server inside
 * {@code network.MachineStatsPayload} while that machine's menu is open.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> machine-scoped numbers only (rates, heat, statuses) — no
 * player names/UUIDs are ever stored here, and nothing routes through the erasure hook.
 */
public final class MachineStats {

    /** Sixty one-second samples = the 60-second sparkline history (Stage G decision). */
    public static final int WINDOW = 60;

    private final short[] heat = new short[WINDOW];
    private final short[] energy = new short[WINDOW];
    private final short[] ops = new short[WINDOW];

    /** Next write index into the ring. */
    private int head;
    /** Valid samples so far (caps at {@link #WINDOW}). */
    private int size;

    /** Running work-ops counter (bumped by the BE per working tick / one-shot pulse)... */
    private int workOps;
    /** ...and its value at the previous sample, so each slot stores a per-second delta. */
    private int sampledOps;

    private MachineStatus status = MachineStatus.IDLE;

    /** Count work done this tick (working tick or one-shot op) toward the next sample's delta. */
    public void countOps(int amount) {
        this.workOps += amount;
    }

    /** Record one per-second sample (permille values clamp to the short-safe 0..1000 range). */
    public void sample(int heatPermille, int energyPermille) {
        this.heat[this.head] = clampPermille(heatPermille);
        this.energy[this.head] = clampPermille(energyPermille);
        this.ops[this.head] = (short) Math.max(0, Math.min(Short.MAX_VALUE, this.workOps - this.sampledOps));
        this.sampledOps = this.workOps;
        this.head = (this.head + 1) % WINDOW;
        this.size = Math.min(WINDOW, this.size + 1);
    }

    private static short clampPermille(int value) {
        return (short) Math.max(0, Math.min(1000, value));
    }

    public MachineStatus status() {
        return this.status;
    }

    public void status(MachineStatus status) {
        this.status = status;
    }

    /** Work-ops recorded in the most recent full sample (the "current rate", per second). */
    public int currentOpsRate() {
        return this.size == 0 ? 0 : this.ops[Math.floorMod(this.head - 1, WINDOW)] & 0xFFFF;
    }

    /** Heat history, oldest → newest (length = samples so far, at most {@link #WINDOW}). */
    public short[] heatHistory() {
        return export(this.heat);
    }

    /** Stored-energy history, oldest → newest. */
    public short[] energyHistory() {
        return export(this.energy);
    }

    /** Work-ops deltas, oldest → newest. */
    public short[] opsHistory() {
        return export(this.ops);
    }

    private short[] export(short[] ring) {
        short[] out = new short[this.size];
        int start = Math.floorMod(this.head - this.size, WINDOW);
        for (int i = 0; i < this.size; i++) {
            out[i] = ring[(start + i) % WINDOW];
        }
        return out;
    }
}
