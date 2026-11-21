package za.co.neroland.nerotech.machine.extraction;

/**
 * The three drill-head tiers the Deep Core Drill consumes. Pure data (no Minecraft types) so the wear
 * and yield rules test without a bootstrap: a better head lasts more operations, takes more deposit
 * units per operation and yields more per unit.
 */
public enum DrillHeadTier {
    IRON("nerotech:iron_drill_head", 64, 4, 1000),
    CINDERSTEEL("nerotech:cindersteel_drill_head", 192, 6, 1150),
    GLACITE("nerotech:glacite_drill_head", 512, 8, 1300);

    private final String itemId;
    private final int operations;
    private final int unitsPerOperation;
    private final int yieldPermille;

    DrillHeadTier(String itemId, int operations, int unitsPerOperation, int yieldPermille) {
        this.itemId = itemId;
        this.operations = operations;
        this.unitsPerOperation = unitsPerOperation;
        this.yieldPermille = yieldPermille;
    }

    /** The head item's registry id. */
    public String itemId() {
        return this.itemId;
    }

    /** Operations one head lasts before it is used up. */
    public int operations() {
        return this.operations;
    }

    /** Deposit units one operation asks for. */
    public int unitsPerOperation() {
        return this.unitsPerOperation;
    }

    /** Yield multiplier on top of the deposit's own yield, permille. */
    public int yieldPermille() {
        return this.yieldPermille;
    }

    /** The tier for a head item id, or {@code null} when the id is not a drill head. */
    public static DrillHeadTier byItemId(String id) {
        for (DrillHeadTier tier : values()) {
            if (tier.itemId.equals(id)) {
                return tier;
            }
        }
        return null;
    }

    /** Wire/save ordinal decode; {@code null} for out of range (no head). */
    public static DrillHeadTier byOrdinal(int ordinal) {
        DrillHeadTier[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }
}
