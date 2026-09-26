package za.co.neroland.nerotech.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.api.MachineTypeRegistry.Surface;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * Locks the add-on registration contract of {@link MachineTypeRegistry}: registration order is
 * preserved, a listener replays everything already registered and then hears every later
 * registration, and the snapshots are unmodifiable. Pure JVM — the registry stores
 * {@link Supplier}s and never resolves a {@link BlockEntityType}, so null-returning lambdas
 * stand in for real types and no game bootstrap is needed.
 *
 * <p>The registry is process-global, so each test registers on its own surface (GAS / FLUID /
 * ITEM) rather than on ENERGY, and only ever asserts on the suppliers it added itself
 * (relative order and tail membership), never on absolute counts.
 */
class MachineTypeRegistryTest {

    private static Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type() {
        return () -> null;
    }

    @Test
    void registrationOrderIsPreserved() {
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> first = type();
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> second = type();
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> third = type();

        MachineTypeRegistry.registerGas(first);
        MachineTypeRegistry.registerGas(second);
        MachineTypeRegistry.registerGas(third);

        List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> gas = MachineTypeRegistry.gasTypes();
        int i = gas.indexOf(first);
        assertTrue(i >= 0, "registered supplier must be in the snapshot");
        assertSame(second, gas.get(i + 1));
        assertSame(third, gas.get(i + 2));
        assertEquals(gas, MachineTypeRegistry.types(Surface.GAS), "the enum accessor is the same list");
    }

    @Test
    void listenerReplaysExistingTypesThenHearsLaterOnes() {
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> before = type();
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> after = type();
        MachineTypeRegistry.registerFluid(before);

        List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> heard = new ArrayList<>();
        MachineTypeRegistry.onRegistered(Surface.FLUID, heard::add);

        assertTrue(heard.contains(before), "subscribing replays every already-registered type");
        assertEquals(MachineTypeRegistry.fluidTypes(), heard, "replay is the whole snapshot, in order");

        MachineTypeRegistry.registerFluid(after);
        assertSame(after, heard.get(heard.size() - 1), "a later registration reaches the listener");
        assertTrue(MachineTypeRegistry.fluidTypes().contains(after),
                "the type is in the snapshot when the listener sees it");
    }

    @Test
    void listenerSeesTheTypeInTheSnapshotWhenNotified() {
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> late = type();
        List<Boolean> visibleAtNotify = new ArrayList<>();
        MachineTypeRegistry.onRegistered(Surface.ITEM, registered -> {
            if (registered == late) {
                visibleAtNotify.add(MachineTypeRegistry.itemTypes().contains(late));
            }
        });

        MachineTypeRegistry.registerItem(late);

        assertEquals(List.of(Boolean.TRUE), visibleAtNotify,
                "a listener may read the snapshot and find the type it was just told about");
    }

    @Test
    void snapshotsAreUnmodifiableAndDetached() {
        Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> one = type();
        MachineTypeRegistry.registerGas(one);
        List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> snapshot = MachineTypeRegistry.gasTypes();
        int sizeBefore = snapshot.size();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(type()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(0));

        MachineTypeRegistry.registerGas(type());
        assertEquals(sizeBefore, snapshot.size(), "an earlier snapshot does not grow with later registrations");
        assertEquals(sizeBefore + 1, MachineTypeRegistry.gasTypes().size(), "a fresh snapshot does");
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> MachineTypeRegistry.registerEnergy(null));
        assertThrows(IllegalArgumentException.class, () -> MachineTypeRegistry.register(null, type()));
        assertThrows(IllegalArgumentException.class, () -> MachineTypeRegistry.onRegistered(Surface.ENERGY, null));
    }
}
