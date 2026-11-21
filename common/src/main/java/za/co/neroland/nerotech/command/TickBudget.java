package za.co.neroland.nerotech.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.jetbrains.annotations.Nullable;

/**
 * {@code /nerotech budget}: how much server tick time NeroTech machines cost, per machine type.
 * {@code NeroTechMachineBlockEntity.serverTick} reads the clock only while a sampling window is open, so
 * the cost outside a window is one volatile read per machine tick. The report names machine types only:
 * no players, no positions.
 */
public final class TickBudget {

    /** Window length in server ticks. */
    public static final int WINDOW_TICKS = 200;

    private static volatile boolean sampling;

    /** Accumulator for one window, keyed by machine type (block-entity type or any key). Pure. */
    public static final class Window {
        private final Map<Object, long[]> perType = new HashMap<>();

        /** Add one machine tick of {@code nanos} for {@code type}. */
        public synchronized void add(Object type, long nanos) {
            long[] a = this.perType.computeIfAbsent(type, k -> new long[2]);
            a[0] += Math.max(0L, nanos);
            a[1]++;
        }

        /** Rows sorted by total time, most expensive first; {@code ticks} is the window length. */
        public synchronized List<Row> rows(java.util.function.Function<Object, String> name, int ticks) {
            List<Row> out = new ArrayList<>();
            int t = Math.max(1, ticks);
            this.perType.forEach((type, a) -> out.add(new Row(name.apply(type), a[1], a[0], a[0] / (double) t / 1_000_000D)));
            out.sort(Comparator.comparingLong(Row::totalNanos).reversed().thenComparing(Row::type));
            return out;
        }
    }

    /** One report row: type id, machine-ticks sampled, total nanoseconds, mean milliseconds per server tick. */
    public record Row(String type, long machineTicks, long totalNanos, double meanMsPerTick) {
    }

    @Nullable
    private static Window window;
    private static long endTick;
    @Nullable
    private static CommandSourceStack requester;

    private TickBudget() {
    }

    /** Whether a window is open (the only cost on the machine tick when not sampling). */
    public static boolean sampling() {
        return sampling;
    }

    /** Record one machine tick (called only when {@link #sampling()} was true at the tick's start). */
    public static void record(BlockEntityType<?> type, long nanos) {
        Window w = window;
        if (w != null) {
            w.add(type, nanos);
        }
    }

    /** Open a window; false when one is already running. */
    static synchronized boolean start(MinecraftServer server, CommandSourceStack source) {
        if (sampling) {
            return false;
        }
        window = new Window();
        endTick = server.getTickCount() + (long) WINDOW_TICKS;
        requester = source;
        sampling = true;
        return true;
    }

    /** Server tick hook: closes the window after {@link #WINDOW_TICKS} ticks and prints the table. */
    public static void serverTick(MinecraftServer server) {
        if (!sampling || server.getTickCount() < endTick) {
            return;
        }
        Window w;
        CommandSourceStack src;
        synchronized (TickBudget.class) {
            sampling = false;
            w = window;
            src = requester;
            window = null;
            requester = null;
        }
        if (w == null || src == null) {
            return;
        }
        report(src, w.rows(TickBudget::typeName, WINDOW_TICKS));
    }

    private static void report(CommandSourceStack source, List<Row> rows) {
        source.sendSuccess(() -> Component.translatable("command.nerotech.budget.header", WINDOW_TICKS), false);
        if (rows.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.nerotech.budget.none"), false);
            return;
        }
        long total = 0L;
        for (Row r : rows) {
            total += r.totalNanos();
            String mean = String.format(java.util.Locale.ROOT, "%.4f", r.meanMsPerTick());
            String sum = String.format(java.util.Locale.ROOT, "%.2f", r.totalNanos() / 1_000_000D);
            source.sendSuccess(() -> Component.translatable("command.nerotech.budget.row",
                    r.type(), mean, sum, r.machineTicks()), false);
        }
        String totalMean = String.format(java.util.Locale.ROOT, "%.4f", total / (double) WINDOW_TICKS / 1_000_000D);
        String totalSum = String.format(java.util.Locale.ROOT, "%.2f", total / 1_000_000D);
        source.sendSuccess(() -> Component.translatable("command.nerotech.budget.total", totalMean, totalSum), false);
    }

    private static String typeName(Object type) {
        if (type instanceof BlockEntityType<?> beType) {
            Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(beType);
            if (id != null) {
                return id.toString();
            }
        }
        return String.valueOf(type);
    }
}
