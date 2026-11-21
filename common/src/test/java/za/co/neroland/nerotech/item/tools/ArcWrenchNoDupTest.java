package za.co.neroland.nerotech.item.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The Arc Wrench cannot duplicate or lose a machine: a fake world counts the items in the machine, on
 * the ground and on the wrench, and the total never changes across pick-up, failed removal and place-back.
 * Pure JVM, on {@link WrenchRules}.
 */
class ArcWrenchNoDupTest {

    /** A machine holding {@code items}; removal spills whatever is still inside onto {@code ground}. */
    static final class FakeWorld implements WrenchRules.Site<Integer>, WrenchRules.Placement<Integer> {
        int inMachine;
        int ground;
        boolean standing = true;
        boolean removable = true;
        boolean placeable = true;
        /** The permission predicate a protection mod answers (claims, spawn protection, adventure mode). */
        java.util.function.BooleanSupplier allowed = () -> true;
        int placeAttempts;

        FakeWorld(int items) {
            this.inMachine = items;
        }

        @Override
        public Integer snapshot() {
            return this.inMachine;
        }

        @Override
        public void clear() {
            this.inMachine = 0;
        }

        @Override
        public void restore(Integer snapshot) {
            this.inMachine = snapshot;
        }

        @Override
        public boolean remove() {
            if (!this.removable) {
                return false;
            }
            this.ground += this.inMachine; // what a removal spills
            this.inMachine = 0;
            this.standing = false;
            return true;
        }

        @Override
        public boolean permitted() {
            return this.allowed.getAsBoolean();
        }

        @Override
        public boolean place() {
            this.placeAttempts++;
            if (!this.placeable || this.standing) {
                return false;
            }
            this.standing = true;
            return true;
        }

        @Override
        public void load(Integer snapshot) {
            this.inMachine = snapshot;
        }
    }

    @Test
    void pickUpMovesEverythingOntoTheWrenchAndSpillsNothing() {
        FakeWorld world = new FakeWorld(40);
        Optional<Integer> wrench = WrenchRules.pickUp(world);
        assertTrue(wrench.isPresent());
        assertEquals(40, wrench.get());
        assertEquals(0, world.ground, "the machine was emptied before removal");
        assertEquals(0, world.inMachine);
        assertFalse(world.standing);
        assertEquals(40, world.ground + world.inMachine + wrench.get());
    }

    @Test
    void aFailedRemovalLeavesTheWrenchEmptyAndTheMachineWhole() {
        FakeWorld world = new FakeWorld(40);
        world.removable = false;
        Optional<Integer> wrench = WrenchRules.pickUp(world);
        assertTrue(wrench.isEmpty());
        assertEquals(40, world.inMachine);
        assertTrue(world.standing);
    }

    @Test
    void placeBackHandsTheContentsOverExactlyOnce() {
        FakeWorld world = new FakeWorld(40);
        Integer carried = WrenchRules.pickUp(world).orElseThrow();
        world.placeable = false;
        assertFalse(WrenchRules.placeBack(carried, world), "a blocked spot keeps the machine on the wrench");
        assertEquals(0, world.inMachine);
        world.placeable = true;
        assertTrue(WrenchRules.placeBack(carried, world));
        assertEquals(40, world.inMachine);
        // The wrench lets go after a successful place; placing again onto the standing machine fails.
        assertFalse(WrenchRules.placeBack(carried, world));
        assertEquals(40, world.inMachine + world.ground);
        assertFalse(WrenchRules.placeBack(null, world));
    }

    @Test
    void aDeniedPlacementKeepsTheMachineOnTheWrenchAndTouchesNothing() {
        FakeWorld world = new FakeWorld(40);
        Integer carried = WrenchRules.pickUp(world).orElseThrow();
        world.allowed = () -> false; // a claim mod says no
        assertFalse(WrenchRules.placeBack(carried, world), "a denied spot keeps the machine on the wrench");
        assertEquals(0, world.placeAttempts, "a denial is checked before any block is placed");
        assertFalse(world.standing);
        assertEquals(0, world.inMachine);
        assertEquals(40, carried + world.inMachine + world.ground, "nothing lost, nothing duplicated");

        // The place hook itself vetoes (the loader's place event is cancelled): still kept on the wrench.
        world.allowed = () -> true;
        world.placeable = false;
        assertFalse(WrenchRules.placeBack(carried, world));
        assertEquals(0, world.inMachine);

        world.placeable = true;
        assertTrue(WrenchRules.placeBack(carried, world), "once allowed, it goes down exactly once");
        assertEquals(40, world.inMachine);
        assertEquals(40, world.inMachine + world.ground);
    }

    @Test
    void refusalsCoverEveryDuplicationRoute() {
        WrenchRules.Target ok = new WrenchRules.Target(true, false, false, false, true);
        assertEquals(WrenchRules.Refusal.OK, WrenchRules.check(ok, false));
        assertEquals(WrenchRules.Refusal.ALREADY_CARRYING, WrenchRules.check(ok, true));
        assertEquals(WrenchRules.Refusal.NOT_A_MACHINE,
                WrenchRules.check(new WrenchRules.Target(false, false, false, false, true), false));
        assertEquals(WrenchRules.Refusal.CARRIES_OWN_CONTENTS,
                WrenchRules.check(new WrenchRules.Target(true, true, false, false, true), false));
        assertEquals(WrenchRules.Refusal.FORMED_STRUCTURE,
                WrenchRules.check(new WrenchRules.Target(true, false, true, false, true), false));
        assertEquals(WrenchRules.Refusal.FORMED_STRUCTURE,
                WrenchRules.check(new WrenchRules.Target(true, false, false, true, true), false));
        assertEquals(WrenchRules.Refusal.NOT_PERMITTED,
                WrenchRules.check(new WrenchRules.Target(true, false, false, false, false), false));
    }
}
