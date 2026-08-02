package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Grid Controller block (Stage D) — directional, ticks its {@link GridControllerBlockEntity}. */
public class GridControllerBlock extends NeroTechMachineBlock {

    public static final MapCodec<GridControllerBlock> CODEC = simpleCodec(GridControllerBlock::new);

    public GridControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GridControllerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GridControllerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.GRID_CONTROLLER.get();
    }
}
