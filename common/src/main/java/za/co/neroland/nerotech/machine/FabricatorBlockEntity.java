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

/** Fabricator — refined material → NeroTech component. Recipes: {@code nerotech:fabricating} datapack. */
public class FabricatorBlockEntity extends AbstractProcessingBlockEntity {

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    @Override
    protected RecipeType<MachineRecipe> recipeType() {
        return ModRecipeTypes.FABRICATING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.fabricator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FabricatorMenu(containerId, playerInventory, this, this.data);
    }
}
