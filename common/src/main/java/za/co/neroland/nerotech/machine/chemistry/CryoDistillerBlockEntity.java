package za.co.neroland.nerotech.machine.chemistry;

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
 * Cryo Distiller ({@code nerotech:cryo_distilling}): ices (items by {@code nerotech:inputs/*} tags) and
 * mixed gases into fractions, gas and/or fluid out. Runs faster in cold ambient and slower in hot
 * ({@link CryoSpeed}, scaled and capped), reading the base's cached {@code ThermalEnvironment.ambientAt}
 * — the factor rides the preset speed factor, so Speed modules and presets still stack on top.
 */
public class CryoDistillerBlockEntity extends ProcessingMachineBlockEntity {

    /** 1 item in, 1 output, a gas tank in, a fluid tank out and a gas tank out. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(1, false, 1, 0L, 4_000L, 4_000L, 4_000L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .height(ProcessingLayout.TALL_HEIGHT)
            .input(0, 26, 30)
            .gasIn(44, 28, 40)
            .arrow(66, 34, 22, 16)
            .output(0, 94, 30)
            .fluidOut(116, 28, 40)
            .gasOut(136, 28, 40)
            .build();

    public CryoDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ChemistryContent.CRYO_DISTILLER_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.CRYO_DISTILLING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.cryo_distiller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ChemistryContent.CRYO_DISTILLER_MENU, id, inv, this, this.data, LAYOUT);
    }

    /** The preset factor times the cold bonus for this spot's ambient heat. */
    @Override
    public double presetSpeedFactor() {
        return super.presetSpeedFactor() * CryoSpeed.factor(ambient());
    }
}
