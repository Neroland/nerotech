package za.co.neroland.nerotech.machine.multiblock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Revalidation helper a multiblock controller's block entity owns (a helper object, not a superclass, so
 * controllers keep their existing base class). It decides <i>when</i> to rescan:
 * <ul>
 *   <li>on a cadence, every {@link #UNFORMED_INTERVAL} ticks while unformed and every
 *       {@link #FORMED_INTERVAL} while formed, phase-spread by the controller's position hash;</li>
 *   <li>at once, on the controller's next tick, after a shell block inside its box called
 *       {@link #requestRevalidate(Level, BlockPos)} from {@code neighborChanged} (validation on change,
 *       never per tick).</li>
 * </ul>
 * Only the formed flag and size are worth persisting; the structure itself is re-derived by scanning.
 * The tracking table holds controller positions and shapes only (world data, no player data).
 */
public final class MultiblockController {

    public static final int UNFORMED_INTERVAL = 20;
    public static final int FORMED_INTERVAL = 100;

    /** Live controllers per level, so a shell change can find the controller(s) whose box contains it. */
    private static final Map<Level, Map<BlockPos, MultiblockController>> TRACKED = new WeakHashMap<>();

    private final StructureShape shape;
    private boolean requested;
    @Nullable
    private Direction facing;

    public MultiblockController(StructureShape shape) {
        this.shape = shape;
    }

    public StructureShape shape() {
        return this.shape;
    }

    /**
     * Whether the controller should rescan this tick: the cadence for its current form state came round,
     * or a shell change asked for it. Also (re)registers the controller so shell changes can reach it.
     */
    public boolean due(Level level, BlockPos pos, Direction facing, boolean formed) {
        track(level, pos, facing);
        if (this.requested) {
            this.requested = false;
            return true;
        }
        return onCadence(level.getGameTime(), pos.hashCode(), formed);
    }

    /** The cadence rule on its own (pure): formed 100 t, unformed 20 t, phase from the position hash. */
    public static boolean onCadence(long gameTime, int posHash, boolean formed) {
        int cadence = formed ? FORMED_INTERVAL : UNFORMED_INTERVAL;
        return (gameTime + Math.floorMod(posHash, cadence)) % cadence == 0;
    }

    /** Run the bounded scan now. */
    public Optional<MultiblockValidator.Formed> validate(Level level, BlockPos pos, Direction facing) {
        return MultiblockValidator.validate(level, pos, facing, this.shape);
    }

    /** Stop tracking (call from the block entity's {@code setRemoved}). */
    public void release(Level level, BlockPos pos) {
        synchronized (TRACKED) {
            Map<BlockPos, MultiblockController> byPos = TRACKED.get(level);
            if (byPos != null && byPos.get(pos) == this) {
                byPos.remove(pos);
                if (byPos.isEmpty()) {
                    TRACKED.remove(level);
                }
            }
        }
    }

    private void track(Level level, BlockPos pos, Direction facing) {
        this.facing = facing;
        synchronized (TRACKED) {
            Map<BlockPos, MultiblockController> byPos = TRACKED.computeIfAbsent(level, k -> new HashMap<>());
            if (byPos.get(pos) != this) {
                byPos.put(pos.immutable(), this);
            }
        }
    }

    /**
     * A block at {@code changed} changed: every tracked controller in this level whose largest possible box
     * contains it rescans on its next tick. Shell blocks call this from {@code neighborChanged}; a no-op on
     * the client and when no controller is near.
     */
    public static void requestRevalidate(Level level, BlockPos changed) {
        if (level.isClientSide()) {
            return;
        }
        synchronized (TRACKED) {
            Map<BlockPos, MultiblockController> byPos = TRACKED.get(level);
            if (byPos == null) {
                return;
            }
            for (Map.Entry<BlockPos, MultiblockController> e : byPos.entrySet()) {
                MultiblockController c = e.getValue();
                if (c.covers(e.getKey(), changed)) {
                    c.requested = true;
                }
            }
        }
    }

    /**
     * Whether {@code pos} lies in the candidate box of any tracked controller in {@code level} other than a
     * controller standing at {@code pos} itself (the Arc Wrench refuses to lift a part out of a structure).
     * Positions only; no player data.
     */
    public static boolean insideTrackedBox(Level level, BlockPos pos) {
        synchronized (TRACKED) {
            Map<BlockPos, MultiblockController> byPos = TRACKED.get(level);
            if (byPos == null) {
                return false;
            }
            for (Map.Entry<BlockPos, MultiblockController> e : byPos.entrySet()) {
                if (!e.getKey().equals(pos) && e.getValue().covers(e.getKey(), pos)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether {@code pos} lies in any candidate box of this controller (pure geometry). */
    boolean covers(BlockPos controller, BlockPos pos) {
        Direction f = this.facing;
        if (f == null) {
            return false;
        }
        for (int size : this.shape.sizes()) {
            BlockPos min = this.shape.min(controller, f, size);
            BlockPos max = this.shape.max(controller, f, size);
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                return true;
            }
        }
        return false;
    }

    /** Whether a revalidation request is pending (tests and debugging). */
    public boolean revalidationRequested() {
        return this.requested;
    }
}
