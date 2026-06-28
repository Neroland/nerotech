package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.OreProcessorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Advanced Ore Processor (Tier 2) — same Earth-ore recipes as the Ore Processor, higher dust yield. */
public class AdvancedOreProcessorBlockEntity extends AbstractProcessingBlockEntity {

    public AdvancedOreProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_ORE_PROCESSOR.get(), pos, state);
    }

    @Override
    protected ItemStack resultFor(ItemStack input) {
        ItemStack base = OreProcessorRecipes.resultFor(input);
        if (base.isEmpty()) {
            return base;
        }
        ItemStack boosted = base.copy();
        boosted.grow(NeroTechConfig.advancedOreProcessorYieldBonus());
        return boosted;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.advanced_ore_processor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new OreProcessorMenu(containerId, playerInventory, this, this.data);
    }
}
