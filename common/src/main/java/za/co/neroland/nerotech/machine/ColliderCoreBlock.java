package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Collider Core block — the Particle Collider's controller; ticks its
 * {@link ColliderCoreBlockEntity} and, on any neighbour change, tells it to re-validate the
 * Accelerator Coil ring (so breaking the loop next to the core registers immediately instead of
 * waiting out the re-check cadence).
 */
public class ColliderCoreBlock extends NeroTechMachineBlock {

    public static final MapCodec<ColliderCoreBlock> CODEC = simpleCodec(ColliderCoreBlock::new);

    public ColliderCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ColliderCoreBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColliderCoreBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.COLLIDER_CORE.get();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ColliderCoreBlockEntity collider) {
            collider.invalidateStructure();
        }
    }
}
