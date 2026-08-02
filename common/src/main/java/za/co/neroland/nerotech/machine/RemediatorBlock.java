package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Remediator block — directional, ticks its {@link RemediatorBlockEntity}. */
public class RemediatorBlock extends NeroTechMachineBlock {

    public static final MapCodec<RemediatorBlock> CODEC = simpleCodec(RemediatorBlock::new);

    public RemediatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RemediatorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RemediatorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.REMEDIATOR.get();
    }
}
