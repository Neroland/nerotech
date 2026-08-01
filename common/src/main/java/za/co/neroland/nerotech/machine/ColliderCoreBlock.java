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
 * Accelerator Controller block — the head of the free-form particle accelerator; ticks its
 * {@link ColliderCoreBlockEntity} and, on any neighbour change, tells it to re-trace its beam line
 * (so laying or breaking the first guide right next to the controller registers immediately instead
 * of waiting out the re-trace cadence). Guides further out are picked up on the cadence.
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
            collider.invalidatePath();
        }
    }
}
