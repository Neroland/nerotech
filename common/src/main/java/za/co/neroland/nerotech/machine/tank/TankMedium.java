package za.co.neroland.nerotech.machine.tank;

import java.util.Locale;

/**
 * What a Process Tank holds: nothing, one fluid, or one gas, named by registry id string. Pure data
 * (no Minecraft types), so the column math in {@link TankColumnMath} is testable without a bootstrap.
 */
public record TankMedium(Kind kind, String id) {

    /** The medium family. {@link #NONE} is an empty tank that will adopt whatever arrives first. */
    public enum Kind {
        NONE, FLUID, GAS;

        /** Lower-case name used in NBT and the item component ({@code none}/{@code fluid}/{@code gas}). */
        public String serialized() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Parses {@link #serialized()}; anything unknown reads as {@link #NONE}. */
        public static Kind parse(String value) {
            if (value == null) {
                return NONE;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "fluid" -> FLUID;
                case "gas" -> GAS;
                default -> NONE;
            };
        }
    }

    /** The empty medium. */
    public static final TankMedium EMPTY = new TankMedium(Kind.NONE, "");

    public TankMedium {
        if (kind == null || kind == Kind.NONE || id == null || id.isEmpty()) {
            kind = Kind.NONE;
            id = "";
        }
    }

    public static TankMedium fluid(String id) {
        return new TankMedium(Kind.FLUID, id);
    }

    public static TankMedium gas(String id) {
        return new TankMedium(Kind.GAS, id);
    }

    public boolean isEmpty() {
        return this.kind == Kind.NONE;
    }

    /** Two media can share a column when either is empty or both are the same medium. */
    public boolean compatible(TankMedium other) {
        return isEmpty() || other == null || other.isEmpty() || equals(other);
    }

    /** The non-empty one of two compatible media (this one when both are set). */
    public TankMedium or(TankMedium other) {
        return isEmpty() && other != null ? other : this;
    }
}
