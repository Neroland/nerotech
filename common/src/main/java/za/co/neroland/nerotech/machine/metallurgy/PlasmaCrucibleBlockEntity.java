package za.co.neroland.nerotech.machine.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.heat.ThermalMath;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Plasma Crucible ({@code nerotech:alloying}): two or three item inputs plus heat make an alloy. It
 * heats itself: a job that finds the crucible below the recipe's {@code min_heat} fires the arc first,
 * spending energy on heat while progress waits ({@link CrucibleHeat#fire}), and an idle crucible holds
 * its temperature between jobs ({@link CrucibleHeat#holdCorrection}), so a warm start is cheaper than a
 * cold one and a hot ambient keeps it warmer. Machine-scoped state only.
 */
public class PlasmaCrucibleBlockEntity extends ProcessingMachineBlockEntity {

    public static final ProcessingSpec SPEC = ProcessingSpec.items(3, 1);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    public PlasmaCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(MetallurgyContent.PLASMA_CRUCIBLE_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.ALLOYING.get();
    }

    @Override
    protected int supplyHeat(int heatNeeded) {
        int cost = CrucibleHeat.firingCost(MetallurgySettings.plasmaCrucibleFiringEnergy(),
                modifiers().energyMultiplier() * presetEnergyFactor());
        if (!energyBuffer().has(cost)) {
            return -1;
        }
        int next = CrucibleHeat.fire(this.heat, MetallurgySettings.plasmaCrucibleFiringHeat(),
                heatThrottleThreshold(), NeroTechConfig.heatCapacity());
        if (next == this.heat) {
            return 0; // at the firing ceiling: the recipe asks for more heat than the arc may give
        }
        energyBuffer().consume(cost);
        this.heat = next;
        return cost;
    }

    @Override
    protected void thermalTick(Level level, BlockPos pos) {
        if (this.active) {
            super.thermalTick(level, pos);
            return;
        }
        int before = this.heat;
        int ambient = ambient(level, pos);
        super.thermalTick(level, pos);
        int correction = CrucibleHeat.holdCorrection(before, ambient, NeroTechConfig.thermalEnvLossPermille(),
                level.getGameTime(), MetallurgySettings.plasmaCrucibleHoldIntervalTicks());
        if (correction != 0) {
            this.heat = ThermalMath.clampHeat(this.heat + correction, NeroTechConfig.heatCapacity());
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.plasma_crucible");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProcessingMenu(MetallurgyContent.PLASMA_CRUCIBLE_MENU, containerId, playerInventory, this,
                this.data, LAYOUT);
    }
}
