package za.co.neroland.nerotech.map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

//? if <1.20.5 {
/*import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapLayerConformance;
import za.co.neroland.nerolandcore.map.MapLayerRegistry;
import za.co.neroland.nerolandcore.map.MapViewer;

import za.co.neroland.nerotech.pollution.PollutionManager;

/** Core's map-layer conformance fixture over the pollution layer, plus dimension isolation and banding. */
class PollutionMapLayerTest {

    //? if <1.20.5 {
    /*static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
    *///?}

    static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "overworld"));
    static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", "the_nether"));

    static final UUID ALICE = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    static final UUID BOB = UUID.fromString("00000000-0000-4000-8000-0000000000b2");
    static final int THRESHOLD = 100;

    /** Readings keyed by dimension and region, exactly like the store. */
    static final class Seeded implements PollutionMapLayer.Source {
        final Map<String, Map<Long, Integer>> regions = new HashMap<>();

        Seeded put(ResourceKey<Level> dim, int rx, int rz, int value) {
            regions.computeIfAbsent(dim.identifier().toString(), d -> new HashMap<>())
                    .put(PollutionManager.regionKey(new BlockPos(rx * 64, 0, rz * 64)), value);
            return this;
        }

        @Override
        public int reading(@Nullable MinecraftServer server, String dimensionId, long regionKey) {
            return regions.getOrDefault(dimensionId, Map.of()).getOrDefault(regionKey, 0);
        }

        @Override
        public int threshold() {
            return THRESHOLD;
        }
    }

    static Seeded seeded() {
        return new Seeded()
                .put(OVERWORLD, 0, 0, 5) // light
                .put(OVERWORLD, 1, 0, 150) // heavy
                .put(OVERWORLD, -1, -1, 500) // severe
                .put(OVERWORLD, 40, 40, 500) // far: never drawn
                .put(OVERWORLD, 0, 1, 0) // clean: never drawn
                .put(NETHER, 0, -7, 500); // other dimension only
    }

    static MapViewer at(UUID who, ResourceKey<Level> dim, int x, int z) {
        return MapViewer.forTest(who, null, dim, new BlockPos(x, 64, z));
    }

    @BeforeAll
    static void register() {
        MapLayerRegistry.register(new PollutionMapLayer(seeded()));
    }

    @Test
    void conforms() {
        MapLayerConformance.create(new PollutionMapLayer(seeded()))
                .viewers(at(ALICE, OVERWORLD, 10, 10), at(BOB, OVERWORLD, 14, 10))
                .dimensions(OVERWORLD, NETHER)
                .bounds(MapBounds.around(0, 0, 512))
                .farBounds(MapBounds.around(20_000, 20_000, 256))
                .expectEntitledSees(PollutionMapLayer.POLLUTION, 3)
                .verify();
    }

    @Test
    void twoViewersNearbySeeTheSameRegionsAndNoMore() {
        var alice = MapLayerRegistry.query("nerotech", at(ALICE, OVERWORLD, 10, 10), OVERWORLD,
                MapBounds.around(0, 0, 4_096));
        var bob = MapLayerRegistry.query("nerotech", at(BOB, OVERWORLD, 14, 10), OVERWORLD,
                MapBounds.around(0, 0, 4_096));
        assertEquals(3, alice.areas().size(), "light, heavy and severe in reach; clean and far skipped");
        assertEquals(alice.areas().size(), bob.areas().size(), "a hazard layer is the same for everyone nearby");
        assertEquals(0, alice.markers().size(), "areas only, no markers");
    }

    @Test
    void aReadingInOneDimensionNeverAppearsInAnother() {
        var overworld = MapLayerRegistry.query("nerotech", at(ALICE, OVERWORLD, 10, -400), OVERWORLD,
                MapBounds.around(0, -400, 4_096));
        var nether = MapLayerRegistry.query("nerotech", at(ALICE, NETHER, 10, -400), NETHER,
                MapBounds.around(0, -400, 4_096));
        assertEquals(1, nether.areas().size(), "the nether's own region");
        assertEquals(0, overworld.areas().size(), "same x/z in the overworld: the nether reading is not drawn");
        assertEquals(0, MapLayerRegistry.query("nerotech", at(ALICE, NETHER, 10, 10), OVERWORLD,
                MapBounds.around(0, 0, 4_096)).areas().size(), "a viewer in another dimension sees nothing here");
    }

    @Test
    void offlineAndFarViewersSeeNothing() {
        assertEquals(0, MapLayerRegistry.query("nerotech", MapViewer.forTest(ALICE, null, null, null), OVERWORLD,
                MapBounds.around(0, 0, 4_096)).areas().size());
        assertEquals(0, MapLayerRegistry.query("nerotech", at(ALICE, OVERWORLD, 5_000, 5_000), OVERWORLD,
                MapBounds.around(0, 0, 4_096)).areas().size());
    }

    @Test
    void bandsAreCoarse() {
        assertEquals(0, PollutionMapLayer.band(0, THRESHOLD));
        assertEquals(1, PollutionMapLayer.band(99, THRESHOLD));
        assertEquals(2, PollutionMapLayer.band(100, THRESHOLD));
        assertEquals(2, PollutionMapLayer.band(199, THRESHOLD));
        assertEquals(3, PollutionMapLayer.band(200, THRESHOLD));
        assertEquals(1, PollutionMapLayer.band(5_000, 0), "threshold off: any pollution is light");
    }
}
