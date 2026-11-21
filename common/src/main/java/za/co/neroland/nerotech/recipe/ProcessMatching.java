package za.co.neroland.nerotech.recipe;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Item matching for {@link ProcessRecipe}, written against a generic stack type so it is testable
 * without Minecraft: each requirement is a predicate plus a count, and the input stacks' counts are
 * spent greedily, requirement by requirement, so one stack can serve two requirements only if it has
 * enough for both.
 */
public final class ProcessMatching {

    private ProcessMatching() {
    }

    /** One counted requirement. */
    public record Requirement<S>(Predicate<S> test, int count) {
    }

    /** Whether {@code stacks} hold every requirement's count. */
    public static <S> boolean itemsSatisfied(List<Requirement<S>> requirements, List<S> stacks,
            ToIntFunction<S> countOf) {
        int[] left = new int[stacks.size()];
        for (int i = 0; i < left.length; i++) {
            S stack = stacks.get(i);
            left[i] = stack == null ? 0 : Math.max(0, countOf.applyAsInt(stack));
        }
        for (Requirement<S> requirement : requirements) {
            int need = requirement.count();
            for (int i = 0; i < left.length && need > 0; i++) {
                if (left[i] > 0 && requirement.test().test(stacks.get(i))) {
                    int take = Math.min(left[i], need);
                    left[i] -= take;
                    need -= take;
                }
            }
            if (need > 0) {
                return false;
            }
        }
        return true;
    }

    /** A permille roll: {@code chancePermille} of 1000, with {@code roll} uniform in [0, 1000). */
    public static boolean rolls(int chancePermille, int roll) {
        return chancePermille >= 1000 || roll < chancePermille;
    }
}
