package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Bio Generator block (Stage D) — directional, ticks its {@link BioGeneratorBlockEntity}. */
public class BioGeneratorBlock extends NeroTechMachineBlock {

    public static final MapCodec<BioGeneratorBlock> CODEC = simpleCodec(BioGeneratorBlock::new);

    public BioGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<BioGeneratorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioGeneratorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.BIO_GENERATOR.get();
    }
}
