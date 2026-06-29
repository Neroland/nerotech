package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Item Sorter block — ticks its {@link ItemSorterBlockEntity}. */
public class ItemSorterBlock extends NeroTechMachineBlock {

    public static final MapCodec<ItemSorterBlock> CODEC = simpleCodec(ItemSorterBlock::new);

    public ItemSorterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ItemSorterBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemSorterBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ITEM_SORTER.get();
    }
}
