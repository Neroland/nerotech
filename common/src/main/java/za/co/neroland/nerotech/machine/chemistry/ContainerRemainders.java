package za.co.neroland.nerotech.machine.chemistry;

import java.util.Optional;

/**
 * Pure rule for what an item leaves behind when a Reaction Vat consumes it: any filled bucket
 * ({@code <namespace>:<something>_bucket}, for example {@code nerospace:brine_bucket}) returns an empty
 * {@code minecraft:bucket}, exactly like a crafting remainder. Ids only, so the rule works whether or
 * not the mod that adds the bucket declares a remainder, and the test needs no game bootstrap.
 */
public final class ContainerRemainders {

    public static final String EMPTY_BUCKET = "minecraft:bucket";

    private ContainerRemainders() {
    }

    /** The remainder item id for consuming one {@code itemId}, or empty when nothing is left. */
    public static Optional<String> remainderFor(String itemId) {
        if (itemId == null || itemId.equals(EMPTY_BUCKET)) {
            return Optional.empty();
        }
        int colon = itemId.indexOf(':');
        String path = colon < 0 ? itemId : itemId.substring(colon + 1);
        if (path.endsWith("_bucket") && path.length() > "_bucket".length()) {
            return Optional.of(EMPTY_BUCKET);
        }
        return Optional.empty();
    }
}
