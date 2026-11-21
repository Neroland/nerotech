package za.co.neroland.nerotech.item.tools;

/**
 * The Filter Mask's cartridge cycle without Minecraft types (unit-tested). A fitted Filter Cartridge
 * blocks a fixed number of exposure pulses; the pulse that uses its last one fouls it into a Dirty Filter
 * (the existing Scrubber cycle: cartridge → dirty filter → Ore Processor refund). A mask with no
 * cartridge or no charge blocks nothing.
 */
public final class FilterRules {

    private FilterRules() {
    }

    /** What one exposure pulse does to a worn mask. */
    public record Pulse(boolean blocked, int usesLeft, boolean fouled) {
    }

    public static Pulse pulse(int usesLeft, int energy, int energyPerPulse) {
        if (usesLeft <= 0 || energy < Math.max(0, energyPerPulse)) {
            return new Pulse(false, Math.max(0, usesLeft), false);
        }
        int left = usesLeft - 1;
        return new Pulse(true, left, left == 0);
    }
}
