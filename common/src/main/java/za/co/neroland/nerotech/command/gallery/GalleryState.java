package za.co.neroland.nerotech.command.gallery;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Where galleries stand ({@code nerotech:gallery}, overworld saved data, through Core's recovery guard): one
 * row per built gallery, its dimension id and origin block. World data only: no player, no owner, no name.
 * It lets {@code gallery clear} and {@code gallery tour} find the gallery the player is standing in, and it
 * marks the drilling rig's chunk as the gallery's demo deposit ({@link #isDemoChunk}), which the deposit
 * service serves from a fixed seed without ever writing depletion. {@code gallery clear} removes the row.
 * Bounded at {@value #MAX_GALLERIES} rows (the oldest is dropped).
 */
public final class GalleryState extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "gallery");

    public static final SavedDataType<GalleryState> TYPE = SavedDataCompat.type(ID, GalleryState::new, codec());

    /** Most galleries remembered per server. */
    public static final int MAX_GALLERIES = 16;

    /** One gallery: dimension id and origin. */
    public record Site(String dimension, int x, int y, int z) {
        static final Codec<Site> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("dim").forGetter(Site::dimension),
                Codec.INT.fieldOf("x").forGetter(Site::x),
                Codec.INT.fieldOf("y").forGetter(Site::y),
                Codec.INT.fieldOf("z").forGetter(Site::z)
        ).apply(inst, Site::new));

        /** Whether an absolute position is inside this gallery's volume. */
        public boolean contains(String dim, int px, int py, int pz) {
            return this.dimension.equals(dim)
                    && GalleryLayout.inVolume(px - this.x, py - this.y, pz - this.z);
        }

        /** Whether a chunk holds this gallery's drilling rig. */
        public boolean demoChunk(String dim, int chunkX, int chunkZ) {
            GalleryLayout.Part drill = GalleryLayout.drillController();
            return this.dimension.equals(dim) && ((this.x + drill.x()) >> 4) == chunkX
                    && ((this.z + drill.z()) >> 4) == chunkZ;
        }
    }

    private final List<Site> sites = new ArrayList<>();

    public GalleryState() {
    }

    /** The store for this server, through Core's crash-safe recovery guard. */
    public static GalleryState get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), TYPE, GalleryState::new, ID.toString());
    }

    /** Remember a gallery (replacing one at the same origin). */
    public void add(Site site) {
        this.sites.remove(site);
        this.sites.add(site);
        while (this.sites.size() > MAX_GALLERIES) {
            this.sites.remove(0);
        }
        setDirty();
    }

    /** Forget a gallery. */
    public void remove(Site site) {
        if (this.sites.remove(site)) {
            setDirty();
        }
    }

    /** The newest gallery whose volume holds the position, or null. */
    @Nullable
    public Site find(String dim, int x, int y, int z) {
        for (int i = this.sites.size() - 1; i >= 0; i--) {
            Site site = this.sites.get(i);
            if (site.contains(dim, x, y, z)) {
                return site;
            }
        }
        return null;
    }

    /** Whether a chunk is a gallery's demo deposit. */
    public boolean isDemoChunk(String dim, int chunkX, int chunkZ) {
        for (Site site : this.sites) {
            if (site.demoChunk(dim, chunkX, chunkZ)) {
                return true;
            }
        }
        return false;
    }

    /** Number of remembered galleries. */
    public int size() {
        return this.sites.size();
    }

    static Codec<GalleryState> codec() {
        return RecordCodecBuilder.create(inst -> inst.group(
                Site.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(s -> List.copyOf(s.sites))
        ).apply(inst, GalleryState::fromData));
    }

    private static GalleryState fromData(List<Site> sites) {
        GalleryState state = new GalleryState();
        state.sites.addAll(sites);
        while (state.sites.size() > MAX_GALLERIES) {
            state.sites.remove(0);
        }
        return state;
    }
}
