package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.menu.OreProcessorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Ore Processor — raw ore → 2 dust, the core of the material economy. Recipes in {@link OreProcessorRecipes}. */
public class OreProcessorBlockEntity extends AbstractProcessingBlockEntity {

    public OreProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_PROCESSOR.get(), pos, state);
    }

    @Override
    protected ItemStack resultFor(ItemStack input) {
        return OreProcessorRecipes.resultFor(input);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.ore_processor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OreProcessorMenu(containerId, playerInventory, this, this.data);
    }
}
