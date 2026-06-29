package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.menu.FabricatorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Fabricator — refined material → NeroTech component. Recipes in {@link FabricatorRecipes}. */
public class FabricatorBlockEntity extends AbstractProcessingBlockEntity {

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    @Override
    protected ItemStack resultFor(ItemStack input) {
        return FabricatorRecipes.resultFor(input);
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
