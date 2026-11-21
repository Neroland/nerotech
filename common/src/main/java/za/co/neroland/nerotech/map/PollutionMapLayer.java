package za.co.neroland.nerotech.map;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.map.MapArea;
import za.co.neroland.nerolandcore.map.MapAreaStyle;
import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapLabel;
import za.co.neroland.nerolandcore.map.MapLayerInfo;
import za.co.neroland.nerolandcore.map.MapLayerProvider;
import za.co.neroland.nerolandcore.map.MapLayerRegistry;
import za.co.neroland.nerolandcore.map.MapLayerSink;
import za.co.neroland.nerolandcore.map.MapScope;
import za.co.neroland.nerolandcore.map.MapViewer;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.pollution.PollutionState;

/**
 * NeroTech's one layer for Neroland Core's map API: {@code nerotech:pollution}, {@link MapScope#PROXIMITY}
 * (a hazard with no owner), world map only by default. It draws the 64x64 pollution regions around the
 * online viewer, in the viewer's own dimension only, as rectangles in three bands (light, heavy, severe,
 * against {@code pollutionEventThreshold}). A clean region draws nothing.
 *
 * <p>POPIA/GDPR: the region store is keyed by dimension and region, never by a player, and the layer never
 * shows a raw reading or anything about who polluted. A viewer offline, in another dimension or out of
 * reach sees nothing, and a reading recorded in one dimension never appears in another (the store is
 * dimension-keyed). NeroTech draws nothing itself; NeroMaps and the companion app do.
 */
public final class PollutionMapLayer implements MapLayerProvider {

    public static final Identifier POLLUTION = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "pollution");

    /** How far around the viewer regions are drawn (Core's hazard proximity). */
    static final int REACH = MapLayerInfo.HAZARD_PROXIMITY;
    /** Region edge in blocks ({@link PollutionManager#regionKey}: x >> 6, z >> 6). */
    static final int REGION = 64;

    private static final int[] FILL = {0, 0x30E8C547, 0x40E07B24, 0x50C8322A};
    private static final int[] BORDER = {0, 0x90E8C547, 0xB0E07B24, 0xD0C8322A};
    private static final String[] BAND_KEY = {"", "low", "high", "severe"};

    private static final List<MapLayerInfo> LAYERS = List.of(
            MapLayerInfo.proximityHazard(POLLUTION, "map.layer.nerotech.pollution", POLLUTION)
                    .withDefaults(true, false));

    /** Where readings come from: the live store on a server, a seeded map in tests. */
    public interface Source {
        /** The region's reading in {@code dimensionId}, 0 when clean or unknown. */
        int reading(@Nullable MinecraftServer server, String dimensionId, long regionKey);

        /** The band threshold ({@code pollutionEventThreshold}). */
        int threshold();
    }

    /** The live source: Nerotech's dimension-keyed pollution store. */
    static final class ServerSource implements Source {
        @Override
        public int reading(@Nullable MinecraftServer server, String dimensionId, long regionKey) {
            return server == null ? 0 : PollutionState.get(server).region(dimensionId, regionKey);
        }

        @Override
        public int threshold() {
            return NeroTechConfig.pollutionEventThreshold();
        }
    }

    private final Source source;

    public PollutionMapLayer() {
        this(new ServerSource());
    }

    /** For tests: the same rules over a seeded source. */
    public PollutionMapLayer(Source source) {
        this.source = source;
    }

    /** Registers the provider with Core. A failure is logged and never takes the mod down. */
    public static void register() {
        try {
            MapLayerRegistry.register(new PollutionMapLayer());
        } catch (RuntimeException e) {
            NeroTechCommon.LOGGER.warn("[NeroTech] map layer not registered: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    public String modId() {
        return NeroTechCommon.MOD_ID;
    }

    @Override
    public List<MapLayerInfo> layers() {
        return LAYERS;
    }

    @Override
    public void query(MapViewer viewer, ResourceKey<Level> dimension, MapBounds bounds, MapLayerSink sink) {
        BlockPos at = viewer.position().orElse(null);
        if (at == null || !viewer.dimension().map(dimension::equals).orElse(false)) {
            return; // proximity: the viewer must be online, in this dimension
        }
        MinecraftServer server = viewer.server();
        String dimId = dimension.identifier().toString();
        int threshold = this.source.threshold();
        int minRx = (at.getX() - REACH) >> 6;
        int maxRx = (at.getX() + REACH) >> 6;
        int minRz = (at.getZ() - REACH) >> 6;
        int maxRz = (at.getZ() + REACH) >> 6;
        for (int rx = minRx; rx <= maxRx; rx++) {
            for (int rz = minRz; rz <= maxRz; rz++) {
                if (sink.isFull(POLLUTION)) {
                    return;
                }
                int x0 = rx * REGION;
                int z0 = rz * REGION;
                long key = PollutionManager.regionKey(new BlockPos(x0, 0, z0));
                int band = band(this.source.reading(server, dimId, key), threshold);
                if (band == 0) {
                    continue;
                }
                MapArea area = new MapArea.Rect("pollution." + rx + "." + rz, POLLUTION, dimension,
                        MapAreaStyle.of(FILL[band], BORDER[band])
                                .withLabel(MapLabel.translatable("map.area.nerotech.pollution." + BAND_KEY[band])),
                        x0, z0, x0 + REGION - 1, z0 + REGION - 1);
                if (viewer.within(dimension, area.bounds(), REACH)) {
                    sink.area(area);
                }
            }
        }
    }

    /**
     * The band of a reading: 0 clean, 1 light (under the threshold), 2 heavy (under twice it), 3 severe.
     * Same cut as {@code PollutionManager.band}; the reading itself is never drawn.
     */
    public static int band(int value, int threshold) {
        if (value <= 0) {
            return 0;
        }
        if (threshold <= 0 || value < threshold) {
            return 1;
        }
        return value < 2L * threshold ? 2 : 3;
    }
}
