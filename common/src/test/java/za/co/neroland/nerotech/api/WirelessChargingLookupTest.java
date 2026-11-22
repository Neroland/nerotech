package za.co.neroland.nerotech.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.energy.ItemEnergyLookup;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * A charger fills a generic item that registered with Neroland Core's {@link ItemEnergyLookup}, and a
 * registered adapter still wins. Plain JVM: {@code find} refuses an empty stack before asking providers,
 * and a bootstrapped item is not available here, so resolution uses the same package-private first match
 * Core's own lookup tests use. {@link WirelessCharging#chargeItem} passes that result into
 * {@link WirelessCharging#chargeHeld}.
 */
class WirelessChargingLookupTest {

    private final Cell cell = new Cell();

    @BeforeEach
    void isolate() {
        reset();
    }

    @AfterEach
    void cleanup() {
        reset();
    }

    private static void reset() {
        WirelessCharging.clearChargersForTests();
        ItemEnergyLookup.clearForTests();
    }

    @Test
    void lookupRegisteredItemIsChargedWhenNoAdapterAcceptsIt() throws Exception {
        ItemEnergyLookup.register(stack -> null);
        ItemEnergyLookup.register(stack -> this.cell);
        NeroEnergyStorage found = firstMatch(null);
        assertSame(this.cell, found);

        WirelessCharging.registerItemCharger((stack, maxFe, simulate) -> 0L);
        assertEquals(40L, WirelessCharging.chargeHeld(null, found, 40L, false, 1.0));
        assertEquals(40L, this.cell.amount);

        assertEquals(20L, WirelessCharging.chargeHeld(null, found, 20L, true, 1.0));
        assertEquals(40L, this.cell.amount, "simulate must not add energy");
    }

    @Test
    void registeredAdapterWinsAndIsCappedAtTheOffer() {
        assertFalse(WirelessCharging.hasItemChargers());
        WirelessCharging.registerItemCharger((stack, maxFe, simulate) -> 80L);
        assertTrue(WirelessCharging.hasItemChargers());
        assertEquals(40L, WirelessCharging.chargeHeld(null, this.cell, 40L, false, 1.0));
        assertEquals(0L, this.cell.amount);
    }

    @Test
    void fullItemUnknownItemAndEmptyOfferAcceptNothing() {
        this.cell.amount = this.cell.capacity;
        assertEquals(0L, WirelessCharging.chargeHeld(null, this.cell, 40L, false, 1.0));
        assertEquals(this.cell.capacity, this.cell.amount);
        assertEquals(0L, WirelessCharging.chargeHeld(null, null, 40L, false, 1.0));
        assertEquals(0L, WirelessCharging.chargeHeld(null, this.cell, 0L, false, 1.0));
        assertEquals(0L, WirelessCharging.chargeItem(null, 40L, false));
    }

    @Test
    void offerConvertsToCoreEnergyAndNeverPaysMoreThanOffered() {
        assertEquals(10L, WirelessCharging.chargeHeld(null, this.cell, 10L, false, 2.0));
        assertEquals(5L, this.cell.amount, "10 FE at 2 FE per NE is 5 NE");

        NeroEnergyStorage greedy = new NeroEnergyStorage() {
            @Override
            public long getAmount() {
                return 0;
            }

            @Override
            public long getCapacity() {
                return 1_000;
            }

            @Override
            public long insert(long maxAmount, boolean simulate) {
                return maxAmount + 1_000;
            }

            @Override
            public long extract(long maxAmount, boolean simulate) {
                return 0;
            }
        };
        assertEquals(10L, WirelessCharging.chargeHeld(null, greedy, 10L, false, 1.0));
    }

    @Test
    void roomLeftLimitsTheFill() {
        this.cell.amount = 990;
        assertEquals(10L, WirelessCharging.chargeHeld(null, this.cell, 40L, false, 1.0));
        assertEquals(1_000L, this.cell.amount);
    }

    @Test
    void chargeItemAsksTheLookupThenTheTestedFill() throws Exception {
        String code = stripComments(Files.readString(source()));
        assertTrue(code.contains("ItemEnergyLookup.find(stack)"),
                "chargeItem must ask Core's item-energy lookup");
        assertTrue(code.contains("chargeHeld("), "chargeItem must use the tested fill path");
    }

    /** The lookup's own resolution, skipping the empty-stack check that needs a real item. */
    private static NeroEnergyStorage firstMatch(ItemStack stack) throws Exception {
        Method method = ItemEnergyLookup.class.getDeclaredMethod("firstMatch", ItemStack.class);
        method.setAccessible(true);
        return (NeroEnergyStorage) method.invoke(null, new Object[] {stack});
    }

    private static Path source() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(
                    "common/src/main/java/za/co/neroland/nerotech/api/WirelessCharging.java");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("WirelessCharging.java not found above " + dir);
    }

    private static String stripComments(String src) {
        return src.replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "");
    }

    /** A generic Core energy item: the value lives on the item, and the item knows no player. */
    private static final class Cell implements NeroEnergyStorage {
        long amount;
        final long capacity = 1_000;

        @Override
        public long getAmount() {
            return this.amount;
        }

        @Override
        public long getCapacity() {
            return this.capacity;
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            long moved = Math.min(Math.max(0L, maxAmount), Math.max(0L, this.capacity - this.amount));
            if (!simulate && moved > 0) {
                this.amount += moved;
            }
            return moved;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            return 0L;
        }
    }
}
