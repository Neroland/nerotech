package za.co.neroland.nerotech.machine.chemistry;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * Pure rules for Centrifuge cascades. Centrifuges standing side by side and facing the same way form
 * a chain: gas flows from each one to the neighbour on its clockwise side (its right as you face its
 * front) and each one's <b>position</b> is 1 + the number of linked centrifuges behind it, capped at
 * {@link #MAX}. A centrifuge offers its position to {@code nerotech:centrifuging} recipes as the
 * {@code atmosphere} value {@code cascade_<n>}, so a stage recipe ({@code "atmosphere": "cascade_3"})
 * only runs at that place in a chain; recipes without the field run anywhere. The machine pushes its
 * output gas into the next centrifuge's input, so each stage's input is the previous stage's output gas.
 */
public final class CentrifugeCascade {

    /** Longest chain; a centrifuge with this many in line behind it (or more) is the last stage. */
    public static final int MAX = 4;

    /** One stage recipe reduced to what cascade matching reads (for the rule test and docs). */
    public record Stage(String stageKey, String gasIn, String gasOut) {
    }

    private CentrifugeCascade() {
    }

    /**
     * How many linked centrifuges stand behind this one, walking at most {@code MAX - 1} steps;
     * {@code linkedAt.test(i)} says whether the block {@code i} steps upstream is a centrifuge facing
     * the same way.
     */
    public static int upstreamCount(IntPredicate linkedAt) {
        int count = 0;
        for (int i = 1; i < MAX && linkedAt.test(i); i++) {
            count++;
        }
        return count;
    }

    /** Chain position (1 = head) for {@code upstream} linked centrifuges behind. */
    public static int position(int upstream) {
        return 1 + Math.max(0, Math.min(MAX - 1, upstream));
    }

    /** Whether a centrifuge at {@code position} pushes its gas on to the next one. */
    public static boolean pushesDownstream(int position) {
        return position < MAX;
    }

    /** The {@code atmosphere} value a centrifuge at {@code position} offers recipes. */
    public static String stageKey(int position) {
        return "cascade_" + Math.max(1, Math.min(MAX, position));
    }

    /** The output gas a centrifuge at {@code position} makes from {@code gas}, or empty when no stage runs. */
    public static Optional<String> run(List<Stage> stages, int position, String gas) {
        String key = stageKey(position);
        for (Stage stage : stages) {
            if (stage.stageKey().equalsIgnoreCase(key) && stage.gasIn().equals(gas)) {
                return Optional.of(stage.gasOut());
            }
        }
        return Optional.empty();
    }

    /**
     * Feed {@code gas} into the head of a chain of {@code length} centrifuges and return what leaves the
     * last one (the gas as it stands where no stage ran).
     */
    public static String runChain(List<Stage> stages, int length, String gas) {
        String current = gas;
        for (int position = 1; position <= Math.min(length, MAX); position++) {
            Optional<String> out = run(stages, position, current);
            if (out.isEmpty()) {
                break;
            }
            current = out.get();
        }
        return current;
    }
}
