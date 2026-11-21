package za.co.neroland.nerotech.machine.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Filament Spooler ({@code nerotech:drawing}): draws ingots into wire, and winds wire round a core
 * (a rod or an ingot) into the coil tiers. Two inputs, one output; a coil recipe always names its core,
 * so wire alone never makes a coil.
 */
public class FilamentSpoolerBlockEntity extends ProcessingMachineBlockEntity {

    public static final ProcessingSpec SPEC = ProcessingSpec.items(2, 1);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    public FilamentSpoolerBlockEntity(BlockPos pos, BlockState state) {
        super(MetallurgyContent.FILAMENT_SPOOLER_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.DRAWING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.filament_spooler");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProcessingMenu(MetallurgyContent.FILAMENT_SPOOLER_MENU, containerId, playerInventory, this,
                this.data, LAYOUT);
    }
}
