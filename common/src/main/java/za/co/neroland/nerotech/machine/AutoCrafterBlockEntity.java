package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.AutoCrafterMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Auto Crafter — assembles a vanilla crafting recipe from its 3×3 grid into the output slot, powered by
 * NE. Demand-driven: it only attempts a craft on a batched interval and only when inputs are present, so
 * it never per-tick-scans. Its inventory is exposed via the standard item capability, so NeroLogistics
 * (or any pipe) supplies the grid and pulls the output with no NeroTech dependency.
 *
 * <p>Slots: {@code 0..8} = the 3×3 grid (real items), {@code 9} = output, {@code 10} = the recipe
 * <b>preview</b> (server-written ghost of what the current grid would craft; display-only), and
 * {@code 11..19} = the <b>lock template</b> (count-1 ghosts of the grid captured by {@link #toggleLock}).
 * While locked, each grid slot only accepts its template item — via GUI or automation — so pipes and
 * hoppers keep refilling the right ingredients and the recipe can never be scrambled. Only the grid and
 * output are exposed to automation; preview/template slots are internal ghosts synced through the menu.
 * All of this is block/world state — no player data (POPIA/GDPR: nothing to erase).
 */
public class AutoCrafterBlockEntity extends NeroTechMachineBlockEntity {

    public static final int GRID_SIZE = 9;
    public static final int OUTPUT_SLOT = 9;
    /** Server-written ghost of the current grid recipe's result (display-only). */
    public static final int PREVIEW_SLOT = 10;
    /** First of the nine lock-template ghost slots (template slot i gates grid slot i). */
    public static final int TEMPLATE_START = 11;
    private static final int SLOTS = TEMPLATE_START + GRID_SIZE;

    private final int interval = 10 + Math.floorMod(System.identityHashCode(this), 10);

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state, SLOTS);
        // PROCESSOR preset: grid (ITEM input) on every face except BOTTOM (=output buffer), ENERGY input.
        // Preview + template ghosts are deliberately absent from the slot groups: no face ever exposes them.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ITEM,
                        SlotGroup.of("input", 0, 1, 2, 3, 4, 5, 6, 7, 8),
                        SlotGroup.of("output", OUTPUT_SLOT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    /** True while a lock template is stamped (any template ghost present). */
    public boolean locked() {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!this.items.get(TEMPLATE_START + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggle the grid lock. Locking snapshots the current grid as count-1 template ghosts (needs at
     * least one grid item); unlocking clears them. Server-side (routed through the menu's
     * {@code clickMenuButton}).
     */
    public void toggleLock() {
        if (locked()) {
            for (int i = 0; i < GRID_SIZE; i++) {
                this.items.set(TEMPLATE_START + i, ItemStack.EMPTY);
            }
            setChanged();
            return;
        }
        boolean any = false;
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack in = this.items.get(i);
            this.items.set(TEMPLATE_START + i, in.isEmpty() ? ItemStack.EMPTY : in.copyWithCount(1));
            any |= !in.isEmpty();
        }
        if (!any) {
            return; // nothing to lock to — leave templates empty (still unlocked)
        }
        setChanged();
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if ((serverLevel.getGameTime() % interval) != 0) {
            return;
        }

        // Resolve the grid's recipe first (batched, grid-empty early-out) and keep the preview ghost
        // current even when the machine is unpowered — the GUI always shows what WOULD craft.
        Optional<RecipeHolder<CraftingRecipe>> recipe = Optional.empty();
        ItemStack result = ItemStack.EMPTY;
        if (!gridEmpty()) {
            CraftingInput input = craftingInput();
            recipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
            if (recipe.isPresent()) {
                result = recipe.get().value().assemble(input);
            }
        }
        updatePreview(result);

        int cost = NeroTechConfig.machineNePerTick() * interval;
        if (recipe.isEmpty() || result.isEmpty() || !energyBuffer().has(cost) || !canOutput(result)) {
            this.maxProgress = 0;
            return;
        }

        // Craft: spend power, consume one of each grid ingredient, emit the result.
        energyBuffer().consume(cost);
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack in = this.items.get(i);
            if (!in.isEmpty()) {
                in.shrink(1);
            }
        }
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            this.items.set(OUTPUT_SLOT, result.copy());
        } else {
            out.grow(result.getCount());
        }
        this.maxProgress = 1;
        this.progress = 1;
        // BER surface: one press-stamp animation per completed craft (synced pulse counter).
        pulseClient();
        setChanged();
    }

    // --- BER client surface (hologram item + press-stamp pulse) --------------

    /**
     * The hologram icon: the first non-empty grid stack (the "locked template" ghost). The crafted
     * RESULT is not client-computable — vanilla no longer syncs recipes to the client — so the BER
     * shows the leading template ingredient instead. Grid items ride the BE update tag, and
     * {@link #renderSyncDirty} pushes an update whenever this icon's item changes.
     */
    public ItemStack hologramStack() {
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Last hologram item pushed to clients (sync-discipline comparator; null = empty grid). */
    @Nullable
    private Item syncedHologramItem;

    @Override
    protected boolean renderSyncDirty() {
        ItemStack ghost = hologramStack();
        Item item = ghost.isEmpty() ? null : ghost.getItem();
        if (item != this.syncedHologramItem) {
            this.syncedHologramItem = item;
            return true;
        }
        return false;
    }

    private boolean gridEmpty() {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!this.items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Write the recipe-result ghost into the preview slot only when it actually changed. */
    private void updatePreview(ItemStack result) {
        ItemStack current = this.items.get(PREVIEW_SLOT);
        if (current.isEmpty() && result.isEmpty()) {
            return;
        }
        if (!current.isEmpty() && !result.isEmpty()
                && ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() == result.getCount()) {
            return;
        }
        this.items.set(PREVIEW_SLOT, result.isEmpty() ? ItemStack.EMPTY : result.copy());
        setChanged();
    }

    private CraftingInput craftingInput() {
        List<ItemStack> grid = new ArrayList<>(GRID_SIZE);
        for (int i = 0; i < GRID_SIZE; i++) {
            grid.add(this.items.get(i));
        }
        return CraftingInput.of(3, 3, grid);
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = this.items.get(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GRID_SIZE) {
            return false; // output/preview/template slots never accept placement
        }
        if (!locked()) {
            return true;
        }
        // Locked: each grid slot only accepts its template item (empty template = slot stays empty).
        ItemStack template = this.items.get(TEMPLATE_START + slot);
        return !template.isEmpty() && ItemStack.isSameItemSameComponents(template, stack);
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.auto_crafter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AutoCrafterMenu(containerId, playerInventory, this, this.data);
    }
}
