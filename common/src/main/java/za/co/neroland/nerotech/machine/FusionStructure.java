package za.co.neroland.nerotech.machine;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * Fusion Reactor multiblock validation (Stage E, 2026-07-10). The reactor is a hollow cubic
 * shell of {@code fusion_casing} / {@code fusion_containment_glass} in one of three sizes —
 * 3³, 5³ or 7³ — with the controller block sitting at the <b>centre of one vertical wall face,
 * facing outward</b>, and strictly air inside. Bigger shells unlock higher fuel tiers and larger
 * output multipliers ({@code fusionSizeOutputPermille} config).
 *
 * <p>Validation is a bounded scan (≤ 7³ = 343 positions) run by the controller on a cooldown —
 * every second while unformed, every five while formed (cheap demolition detection that also
 * catches shell blocks broken out of the controller's neighbour reach). Chunks are never loaded
 * by validation: an unloaded corner simply reports "unformed" until the area is back.
 */
public final class FusionStructure {

    /** Candidate shell sizes, largest first so a big shell is never mis-detected as its core. */
    private static final int[] SIZES = {7, 5, 3};

    private static volatile String parsedFrom;
    private static volatile Map<Integer, Integer> multipliers = Map.of();

    private FusionStructure() {
    }

    /** A formed shell: its edge size and interior centre (torus anchor, meltdown epicentre). */
    public record Shell(int size, BlockPos center) {

        /** 1-based tier index (3³ → 1, 5³ → 2, 7³ → 3): the max fuel tier this shell can burn. */
        public int tier() {
            return (this.size - 1) / 2;
        }
    }

    /** Validate the largest formed shell for a controller, or {@code null} when unformed. */
    @Nullable
    public static Shell validate(Level level, BlockPos controller, Direction facing) {
        for (int size : SIZES) {
            Shell shell = check(level, controller, facing, size);
            if (shell != null) {
                return shell;
            }
        }
        return null;
    }

    @Nullable
    private static Shell check(Level level, BlockPos controller, Direction facing, int size) {
        int half = (size - 1) / 2;
        BlockPos center = controller.relative(facing.getOpposite(), half);
        BlockPos min = center.offset(-half, -half, -half);
        BlockPos max = center.offset(half, half, half);
        if (!level.hasChunksAt(min, max)) {
            return null; // never force-load chunks for validation
        }
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            boolean surface = pos.getX() == min.getX() || pos.getX() == max.getX()
                    || pos.getY() == min.getY() || pos.getY() == max.getY()
                    || pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
            if (pos.equals(controller)) {
                continue; // the controller occupies its wall-centre slot
            }
            BlockState state = level.getBlockState(pos);
            if (surface) {
                if (!state.is(ModBlocks.FUSION_CASING.get())
                        && !state.is(ModBlocks.FUSION_CONTAINMENT_GLASS.get())) {
                    return null;
                }
            } else if (!state.isAir()) {
                return null;
            }
        }
        return new Shell(size, center.immutable());
    }

    /** Output multiplier (permille) for a shell size, from {@code fusionSizeOutputPermille}. */
    public static int outputPermille(int size) {
        return table().getOrDefault(size, 1_000);
    }

    private static Map<Integer, Integer> table() {
        String raw = NeroTechConfig.fusionSizeOutputPermille();
        if (raw.equals(parsedFrom)) {
            return multipliers;
        }
        Map<Integer, Integer> parsed = new HashMap<>();
        for (String pair : raw.split(",")) {
            String entry = pair.trim();
            int eq = entry.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                parsed.put(Integer.parseInt(entry.substring(0, eq).trim()),
                        Integer.parseInt(entry.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // Malformed entries are skipped; the rest of the list still applies.
            }
        }
        multipliers = Map.copyOf(parsed);
        parsedFrom = raw;
        return multipliers;
    }
}
