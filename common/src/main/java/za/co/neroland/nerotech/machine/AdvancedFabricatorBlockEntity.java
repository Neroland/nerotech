package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.menu.FabricatorMenu;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Advanced Fabricator (Tier 2) — turns space materials (Void Crystal) into Fusion Cells (reactor
 * fuel). Recipes: {@code nerotech:advanced_fabricating} datapack (tag-matched inputs keep the
 * Nerospace coupling soft).
 */
public class AdvancedFabricatorBlockEntity extends AbstractProcessingBlockEntity {

    public AdvancedFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_FABRICATOR.get(), pos, state);
    }

    @Override
    protected RecipeType<MachineRecipe> recipeType() {
        return ModRecipeTypes.ADVANCED_FABRICATING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.advanced_fabricator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FabricatorMenu(containerId, playerInventory, this, this.data);
    }
}
