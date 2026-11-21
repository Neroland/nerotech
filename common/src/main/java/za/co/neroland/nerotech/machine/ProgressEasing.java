package za.co.neroland.nerotech.machine;

/**
 * Client-side progress extrapolation for BERs (machine contract C1, {@code progressFraction}). The
 * server syncs progress only on change of shape (a new operation, a new rate, a stall), never per
 * tick; between syncs the client advances {@code progress + step × elapsed ticks}, keeps the previous
 * tick's value and lerps between the two by partial tick, so motion is smooth and FPS-independent.
 * Pure arithmetic: no Minecraft types, testable on the bare JVM.
 */
public final class ProgressEasing {

    private int syncedProgress;
    private int syncedMax;
    private int step;
    private boolean running;
    /** Game time the last sync arrived, or {@link Long#MIN_VALUE} until the first tick sees it. */
    private long syncTime = Long.MIN_VALUE;
    private long lastTick = Long.MIN_VALUE;
    private float prev;
    private float curr;

    /**
     * Record a sync from the server.
     *
     * @param progress progress ticks at the sync
     * @param max      ticks per operation (0 = no operation)
     * @param step     progress added per game tick while running
     * @param running  whether progress is advancing (WORK); otherwise the value holds (STALL freezes
     *                 mid-stroke, REST parks at 0)
     */
    public void sync(int progress, int max, int step, boolean running) {
        this.syncedProgress = Math.max(0, progress);
        this.syncedMax = Math.max(0, max);
        this.step = Math.max(0, step);
        this.running = running;
        this.syncTime = Long.MIN_VALUE;
        // A new operation (progress went backwards) must not lerp across the wrap.
        float target = target(0L);
        if (target < this.curr) {
            this.prev = target;
            this.curr = target;
        }
    }

    /** Advance to {@code gameTime} (at most once per tick) and return the eased fraction at {@code partialTick}. */
    public float fraction(long gameTime, float partialTick) {
        if (this.syncTime == Long.MIN_VALUE) {
            this.syncTime = gameTime;
            this.lastTick = gameTime;
            this.curr = target(0L);
            this.prev = this.curr;
        } else if (gameTime != this.lastTick) {
            this.prev = gameTime - this.lastTick == 1L ? this.curr : target(gameTime - 1L - this.syncTime);
            this.curr = target(gameTime - this.syncTime);
            this.lastTick = gameTime;
        }
        float t = Math.max(0f, Math.min(1f, partialTick));
        return this.prev + (this.curr - this.prev) * t;
    }

    /** The un-eased fraction {@code elapsed} ticks after the sync, clamped to [0, 1]. */
    float target(long elapsed) {
        if (this.syncedMax <= 0) {
            return 0f;
        }
        long p = this.syncedProgress + (this.running ? this.step * Math.max(0L, elapsed) : 0L);
        return Math.min(1f, (float) p / this.syncedMax);
    }
}
