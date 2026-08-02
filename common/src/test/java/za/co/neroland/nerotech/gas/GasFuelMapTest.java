package za.co.neroland.nerotech.gas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Locks the {@code turbineGasBurn} parse rules (Stage C): the shipped default is understood, extra
 * fuels are additive, malformed entries never take the rest of the list down with them, and a
 * non-positive multiplier is not a fuel. Pure JVM — no game bootstrap.
 */
class GasFuelMapTest {

    @Test
    void parsesTheShippedDefault() {
        Map<String, Integer> table = GasFuelMap.parse("nerotech:hydrogen=2");
        assertEquals(1, table.size());
        assertEquals(2, table.get("nerotech:hydrogen"));
    }

    @Test
    void parsesMultipleFuelsAndTrimsWhitespace() {
        Map<String, Integer> table = GasFuelMap.parse(" nerotech:hydrogen = 2 , nerospace:oxygen=1 ");
        assertEquals(2, table.size());
        assertEquals(2, table.get("nerotech:hydrogen"));
        assertEquals(1, table.get("nerospace:oxygen"));
    }

    @Test
    void skipsMalformedEntriesWithoutLosingTheRest() {
        Map<String, Integer> table = GasFuelMap.parse("garbage,=5,nerotech:hydrogen=2,foo=bar,=");
        assertEquals(1, table.size());
        assertEquals(2, table.get("nerotech:hydrogen"));
    }

    @Test
    void dropsNonPositiveMultipliers() {
        Map<String, Integer> table = GasFuelMap.parse("a:zero=0,b:negative=-3,c:good=1");
        assertFalse(table.containsKey("a:zero"), "a fuel that yields nothing is not a fuel");
        assertFalse(table.containsKey("b:negative"));
        assertEquals(1, table.get("c:good"));
    }

    @Test
    void emptyAndNullYieldNoFuels() {
        assertTrue(GasFuelMap.parse("").isEmpty());
        assertTrue(GasFuelMap.parse("   ").isEmpty());
        assertTrue(GasFuelMap.parse(null).isEmpty());
    }
}
