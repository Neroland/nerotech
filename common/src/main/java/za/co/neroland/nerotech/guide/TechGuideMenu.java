package za.co.neroland.nerotech.guide;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Tech Guide menu (the Star Guide's recipe): no slots — just synced per-chapter data. Slots
 * {@code [0..N)} = completion masks read live from the player's advancements (bit i = step i of that
 * chapter); slots {@code [N..2N)} = "seen" masks from {@link TechGuideSeenState} (a completed-but-
 * unseen step pulses in the GUI until clicked). Clicking a step sends a menu button
 * ({@code chapter * 16 + step}) that marks it seen server-side. Data slots sync as shorts, so masks
 * are safe while chapters stay ≤ 16 steps (enforced by {@link TechGuide}'s table shape).
 *
 * <p>Adaptation note: Nerospace reaches its "seen" masks through a per-loader player attachment;
 * NeroTech stores them in the UUID-keyed {@link TechGuideSeenState} SavedData instead (same POPIA
 * treatment — see that class), so this menu stays loader-free with no platform seam.</p>
 */
public class TechGuideMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = TechGuide.CHAPTER_COUNT * 2;

    private final ContainerData data;

    /** Client constructor (referenced by the {@code MenuType}). */
    public TechGuideMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, playerInventory.player);
    }

    /** Server constructor: data reads live from the player's advancements + seen store. */
    @SuppressWarnings("this-escape") // idiomatic Minecraft constructor wiring
    public TechGuideMenu(int containerId, Inventory playerInventory, Player player) {
        super(ModMenuTypes.TECH_GUIDE.get(), containerId);
        this.data = player instanceof ServerPlayer serverPlayer
                ? new ProgressData(serverPlayer)
                : new SimpleContainerData(DATA_COUNT);
        checkContainerDataCount(this.data, DATA_COUNT);
        this.addDataSlots(this.data);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots
    }

    /** Step click → mark seen (button id = chapter * 16 + step). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        int chapter = id / 16;
        int step = id % 16;
        if (chapter < 0 || chapter >= TechGuide.CHAPTER_COUNT
                || step >= TechGuide.CHAPTERS.get(chapter).steps().size()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            TechGuideSeenState.get(serverPlayer.level().getServer())
                    .markSeen(serverPlayer.getUUID(), chapter, step);
        }
        return true;
    }

    // --- Screen helpers ------------------------------------------------------------------------

    public int completionMask(int chapter) {
        return this.data.get(chapter);
    }

    public int seenMask(int chapter) {
        return this.data.get(TechGuide.CHAPTER_COUNT + chapter);
    }

    public boolean isStepComplete(int chapter, int step) {
        return (completionMask(chapter) & (1 << step)) != 0;
    }

    public boolean isStepSeen(int chapter, int step) {
        return (seenMask(chapter) & (1 << step)) != 0;
    }

    /** Live server-side view: advancements (completion) + the seen store. */
    private static final class ProgressData implements ContainerData {

        private final ServerPlayer player;

        ProgressData(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public int get(int index) {
            if (index < TechGuide.CHAPTER_COUNT) {
                return TechGuideProgress.chapterMask(this.player, index);
            }
            return TechGuideSeenState.get(this.player.level().getServer())
                    .mask(this.player.getUUID(), index - TechGuide.CHAPTER_COUNT);
        }

        @Override
        public void set(int index, int value) {
            // Read-only from the client.
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
