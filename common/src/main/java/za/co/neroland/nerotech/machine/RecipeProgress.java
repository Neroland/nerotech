package za.co.neroland.nerotech.machine;

/**
 * Whether a processing machine's progress carries over to the recipe it now matches. Progress belongs
 * to one recipe: swapping the inputs for another recipe mid-run starts again from zero, so a long,
 * cheap-per-tick run cannot finish as a short, expensive recipe at the cheap recipe's energy. Pure, so
 * it is unit-tested without a bootstrap.
 */
public final class RecipeProgress {

    /** No recipe recorded (a fresh machine, or progress already at zero). */
    public static final String NONE = "";

    private RecipeProgress() {
    }

    /**
     * Whether progress made on {@code progressRecipe} may continue on {@code currentRecipe}. A machine
     * with no recorded recipe keeps its progress (nothing to compare against); otherwise only the same
     * recipe does.
     */
    public static boolean keepsProgress(String progressRecipe, String currentRecipe) {
        if (progressRecipe == null || progressRecipe.isEmpty()) {
            return true;
        }
        return progressRecipe.equals(currentRecipe);
    }
}
