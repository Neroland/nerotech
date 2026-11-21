package za.co.neroland.nerotech.machine.utility;

/**
 * A charger's output allowance: a token bucket that refills {@code perTick} units every game tick and
 * banks at most {@code burst} (one pass). Player charging and the entity API draw from the same bucket,
 * so a charger never gives out more than its per-tick cap over time, however many callers ask.
 * Transient: never saved.
 */
public final class ChargeBudget {

    private long tokens;
    private long lastTick = Long.MIN_VALUE;

    /** Units available at {@code gameTime} after the refill. */
    public long available(long gameTime, long perTick, long burst) {
        long cap = Math.max(0L, burst);
        if (this.lastTick == Long.MIN_VALUE || gameTime < this.lastTick) {
            this.tokens = cap;
        } else if (gameTime > this.lastTick) {
            long elapsed = gameTime - this.lastTick;
            long per = Math.max(0L, perTick);
            long refill = per == 0 ? 0L : (elapsed >= cap / per + 1 ? cap : elapsed * per);
            this.tokens = Math.min(cap, this.tokens + refill);
        }
        this.tokens = Math.min(this.tokens, cap);
        this.lastTick = gameTime;
        return this.tokens;
    }

    /** Spend {@code amount} units (clamped to what is left). */
    public void spend(long amount) {
        if (amount > 0) {
            this.tokens = Math.max(0L, this.tokens - amount);
        }
    }
}
