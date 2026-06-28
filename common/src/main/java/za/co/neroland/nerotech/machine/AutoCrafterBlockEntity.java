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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.AutoCrafterMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Auto Crafter — assembles a vanilla crafting recipe from its 3×3 grid into the output slot, powered by
 * NE. Demand-driven: it only attempts a craft on a batched interval and only when inputs are present, so
 * it never per-tick-scans. Its inventory is exposed via the standard item capability, so NeroLogistics
 * (or any pipe) supplies the grid and pulls the output with no NeroTech dependency.
 */
public class AutoCrafterBlockEntity extends NeroTechMachineBlockEntity {

    public static final int GRID_SIZE = 9;
    public static final int OUTPUT_SLOT = 9;
    private static final int SLOTS = 10;

    private final int interval = 10 + Math.floorMod(System.identityHashCode(this), 10);

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state, SLOTS);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if ((serverLevel.getGameTime() % interval) != 0) {
            return;
        }
        int cost = NeroTechConfig.machineNePerTick() * interval;
        if (!energyBuffer().has(cost)) {
            this.maxProgress = 0;
            return;
        }

        CraftingInput input = craftingInput();
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                serverLevel.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
        if (recipe.isEmpty()) {
            this.maxProgress = 0;
            return;
        }

        ItemStack result = recipe.get().value().assemble(input);
        if (result.isEmpty() || !canOutput(result)) {
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
        return slot >= 0 && slot < GRID_SIZE; // the 3x3 grid accepts ingredients; output does not
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
