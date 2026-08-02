package za.co.neroland.nerotech.guide;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.guide.TechGuide.Chapter;
import za.co.neroland.nerotech.guide.TechGuide.Step;

/**
 * Server-side progression queries for the Tech Guide (the Star Guide's recipe): completion is read
 * straight from the player's advancements — the guide never keeps its own completion state, so there
 * is nothing extra to store, sync, or erase. Unresolvable advancement ids (a step whose advancement
 * is missing from the datapack) count as incomplete.
 */
public final class TechGuideProgress {

    private TechGuideProgress() {
    }

    /** Whether {@code step} is complete for {@code player}. */
    public static boolean isComplete(ServerPlayer player, Step step) {
        ServerAdvancementManager manager = player.level().getServer().getAdvancements();
        AdvancementHolder holder = manager.get(step.advancement());
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Per-chapter completion bitmask (bit i = step i complete). */
    public static int chapterMask(ServerPlayer player, int chapterIndex) {
        Chapter chapter = TechGuide.CHAPTERS.get(chapterIndex);
        int mask = 0;
        for (int i = 0; i < chapter.steps().size(); i++) {
            if (isComplete(player, chapter.steps().get(i))) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /**
     * The icon of the player's FIRST incomplete step in chapter order — the pedestal hologram's
     * "you are here" marker. EMPTY once everything is complete (the hologram falls back to the
     * datapad).
     */
    public static ItemStack nextStepIcon(ServerPlayer player) {
        for (Chapter chapter : TechGuide.CHAPTERS) {
            for (Step step : chapter.steps()) {
                if (!isComplete(player, step)) {
                    return step.iconStack();
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
