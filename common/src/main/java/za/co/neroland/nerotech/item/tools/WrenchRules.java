package za.co.neroland.nerotech.item.tools;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import za.co.neroland.nerotech.machine.utility.UtilitySaveKeys;

/**
 * The Arc Wrench's pick-up and place-back rules without Minecraft types (unit-tested), written so a
 * machine can never be duplicated or lost:
 *
 * <ol>
 * <li>A machine is refused when it is not a NeroTech machine, when it already carries its contents on
 * its own dropped item (the Process Tank and the Singularity Vault: a wrench copy would double them),
 * when it is a formed multiblock controller, or when it sits inside a tracked multiblock's box.</li>
 * <li>Pick-up runs in one server tick: snapshot → empty the machine (so its removal spills nothing) →
 * remove the block (no loot). Only when the block is really gone does the wrench take the snapshot; if
 * removal fails the machine gets its contents back and the wrench stays empty.</li>
 * <li>Place-back puts the block down first; only when it stands does the wrench let go of the snapshot
 * and the new machine load it. A wrench that already carries a machine picks nothing else up.</li>
 * </ol>
 */
public final class WrenchRules {

    private WrenchRules() {
    }

    /** Why a pick-up is refused, or {@link #OK}. */
    public enum Refusal {
        OK,
        NOT_A_MACHINE,
        CARRIES_OWN_CONTENTS,
        FORMED_STRUCTURE,
        ALREADY_CARRYING,
        NOT_PERMITTED
    }

    /** What the wrench knows about the target. */
    public record Target(boolean neroTechMachine, boolean carriesOwnContents, boolean formedController,
            boolean insideStructure, boolean permitted) {
    }

    public static Refusal check(Target target, boolean carrying) {
        if (carrying) {
            return Refusal.ALREADY_CARRYING;
        }
        if (!target.permitted()) {
            return Refusal.NOT_PERMITTED;
        }
        if (!target.neroTechMachine()) {
            return Refusal.NOT_A_MACHINE;
        }
        if (target.carriesOwnContents()) {
            return Refusal.CARRIES_OWN_CONTENTS;
        }
        if (target.formedController() || target.insideStructure()) {
            return Refusal.FORMED_STRUCTURE;
        }
        return Refusal.OK;
    }

    /** The world side of a pick-up, in game a block entity and its level. */
    public interface Site<S> {
        /** The machine's full saved state. */
        S snapshot();

        /** Empty the machine so its removal spills nothing. */
        void clear();

        /** Put a snapshot back (removal failed). */
        void restore(S snapshot);

        /** Remove the block without loot; whether it is gone. */
        boolean remove();
    }

    /** Pick the machine up: the snapshot to store on the wrench, or empty when nothing left the world. */
    /**
     * Save keys removed from the copy a wrench carries: the owner (POPIA/GDPR: it never rides on the item)
     * and the {@code /nerotech gallery} exhibit flag, so a wrenched exhibit is placed back as an ordinary
     * machine that credits its new owner normally.
     */
    public static final List<String> STRIPPED_ON_PICKUP = Stream.concat(
            UtilitySaveKeys.STRIPPED_OWNER_KEYS.stream(), Stream.of("GalleryExhibit")).toList();

    /** Remove {@link #STRIPPED_ON_PICKUP} from a carried save through {@code remover}; returns {@code tag}. */
    public static <T> T stripCarried(T tag, BiConsumer<T, String> remover) {
        for (String key : STRIPPED_ON_PICKUP) {
            remover.accept(tag, key);
        }
        return tag;
    }

    public static <S> Optional<S> pickUp(Site<S> site) {
        S snapshot = site.snapshot();
        site.clear();
        if (!site.remove()) {
            site.restore(snapshot);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    /** The world side of a place-back. */
    public interface Placement<S> {
        /**
         * Whether the player may build here (the same checks a normal block placement makes). A denial
         * keeps the machine on the wrench; nothing is placed and nothing is loaded.
         */
        default boolean permitted() {
            return true;
        }

        /**
         * Place the block through the normal placement path, so a protection mod's place hook can still
         * veto it (a vetoed place returns false); whether it now stands.
         */
        boolean place();

        /** Load the carried state into the placed machine. */
        void load(S snapshot);
    }

    /** Place the carried machine: whether the wrench should now let go of {@code carried}. */
    public static <S> boolean placeBack(S carried, Placement<S> placement) {
        if (carried == null || !placement.permitted() || !placement.place()) {
            return false;
        }
        placement.load(carried);
        return true;
    }
}
