package za.co.neroland.nerotech.machine;

/**
 * The Particle Accelerator's pure maths — no level, no block entity, no Minecraft state, so the whole
 * rule set is unit-testable on a plain JVM (see {@code AcceleratorMathTest}).
 *
 * <p>Three rules govern a circulating particle, and between them they make ring GEOMETRY the
 * progression axis (mechanic inspired by Oritech's particle accelerator; clean-room implementation):
 *
 * <ul>
 *   <li><b>Gap rule</b> — a segment of length {@code L} may only be crossed at a speed {@code s}
 *       satisfying {@code L <= allowance + s * gapPerSpeed}. Slow particles need close guides; a
 *       particle that drops below the requirement (a brownout mid-run) is lost on the long stretch.
 *       Inverted, {@link #speedForGap} gives the <i>injection</i> speed a ring demands.</li>
 *   <li><b>Bend rule</b> — taking a 45° bend at speed {@code s} needs a run-up:
 *       {@code s <= bendSpeedBase * L} where {@code L} is the segment before the bend. The shortest
 *       segment in a loop therefore caps the whole loop's top speed — big rings for big speeds.</li>
 *   <li><b>Energy</b> — a collision carries {@code E = 0.5 * s^2 * scale}, with {@code scale} in
 *       permille. Recipes gate on it, so a recipe's minimum energy is really a minimum ring size.</li>
 * </ul>
 */
public final class AcceleratorMath {

    /** Blocks covered by one step along a 45° (diagonal) heading. */
    public static final double DIAGONAL_STEP = Math.sqrt(2.0D);

    /** Slack on the {@code <=} comparisons so exact-fit geometry never fails on float dust. */
    private static final double EPSILON = 1.0e-6D;

    private AcceleratorMath() {
    }

    /**
     * The eight horizontal headings a beam can travel, in clockwise order so a 45° turn is ±1 index.
     * Each carries a unit offset with components in {@code {-1, 0, 1}} — marching a diagonal is just
     * {@code pos + k * (dx, dz)}, no per-axis alternation needed.
     */
    public enum Heading {
        NORTH(0, -1),
        NORTH_EAST(1, -1),
        EAST(1, 0),
        SOUTH_EAST(1, 1),
        SOUTH(0, 1),
        SOUTH_WEST(-1, 1),
        WEST(-1, 0),
        NORTH_WEST(-1, -1);

        /** Stable clockwise view — never call {@code values()} inside the trace loop. */
        private static final Heading[] CLOCKWISE = values();

        private final int dx;
        private final int dz;

        Heading(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public int dx() {
            return this.dx;
        }

        public int dz() {
            return this.dz;
        }

        public boolean diagonal() {
            return this.dx != 0 && this.dz != 0;
        }

        /** Blocks covered per step along this heading (1 on an axis, √2 on a diagonal). */
        public double stepLength() {
            return diagonal() ? DIAGONAL_STEP : 1.0D;
        }

        /** Turn 45° clockwise (a {@code RIGHT} guide). */
        public Heading right() {
            return CLOCKWISE[(ordinal() + 1) & 7];
        }

        /** Turn 45° anticlockwise (a {@code LEFT} guide). */
        public Heading left() {
            return CLOCKWISE[(ordinal() + 7) & 7];
        }
    }

    /** Length in blocks of a run of {@code steps} whole blocks along {@code heading}. */
    public static double segmentLength(Heading heading, int steps) {
        return Math.max(0, steps) * heading.stepLength();
    }

    /** The longest segment a particle at {@code speed} can cross without being lost. */
    public static double maxGap(double speed, double allowance, double gapPerSpeed) {
        return allowance + Math.max(0.0D, speed) * gapPerSpeed;
    }

    /** Gap rule: whether a segment of {@code segmentLength} was survivable at {@code speed}. */
    public static boolean gapAllowed(double segmentLength, double speed, double allowance, double gapPerSpeed) {
        return segmentLength <= maxGap(speed, allowance, gapPerSpeed) + EPSILON;
    }

    /**
     * The gap rule inverted: the slowest speed that clears a segment of {@code segmentLength}. The
     * controller injects at the maximum of this over the whole loop, which is why a wide ring costs
     * a fast particle to start and a tight ring starts crawling.
     */
    public static double speedForGap(double segmentLength, double allowance, double gapPerSpeed) {
        if (gapPerSpeed <= 0.0D || segmentLength <= allowance) {
            return 0.0D;
        }
        return (segmentLength - allowance) / gapPerSpeed;
    }

    /** The fastest a particle may be when it hits a 45° bend after a run-up of {@code segmentLength}. */
    public static double maxBendSpeed(double segmentLength, double bendSpeedBase) {
        return bendSpeedBase * Math.max(0.0D, segmentLength);
    }

    /** Bend rule: whether the bend at the end of {@code segmentLength} survives {@code speed}. */
    public static boolean bendAllowed(double speed, double segmentLength, double bendSpeedBase) {
        return speed <= maxBendSpeed(segmentLength, bendSpeedBase) + EPSILON;
    }

    /**
     * Collision energy in joules: {@code E = 0.5 * speed^2 * energyScalePermille / 1000}. Clamped to
     * a non-negative int so it can ride the menu's synced gauges.
     */
    public static int collisionEnergy(double speed, int energyScalePermille) {
        if (speed <= 0.0D || energyScalePermille <= 0) {
            return 0;
        }
        double joules = 0.5D * speed * speed * energyScalePermille / 1000.0D;
        return (int) Math.min(Integer.MAX_VALUE, Math.round(joules));
    }

    /** One powered guide: the particle picks up {@code boost}. */
    public static double boosted(double speed, double boost) {
        return Math.max(0.0D, speed + boost);
    }

    /** One unpowered guide: the particle coasts and sheds {@code drag}. */
    public static double coasted(double speed, double drag) {
        return Math.max(0.0D, speed - drag);
    }
}
