package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Scrubber block — directional, ticks its {@link ScrubberBlockEntity}. */
public class ScrubberBlock extends NeroTechMachineBlock {

    public static final MapCodec<ScrubberBlock> CODEC = simpleCodec(ScrubberBlock::new);

    public ScrubberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ScrubberBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScrubberBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.SCRUBBER.get();
    }
}
