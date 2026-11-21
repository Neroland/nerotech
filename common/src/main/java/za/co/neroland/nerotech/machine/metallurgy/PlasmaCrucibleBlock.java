package za.co.neroland.nerotech.machine.metallurgy;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Plasma Crucible block: directional, ticks its {@link PlasmaCrucibleBlockEntity}. */
public class PlasmaCrucibleBlock extends NeroTechMachineBlock {

    public static final MapCodec<PlasmaCrucibleBlock> CODEC = BlockCodecs.simple(PlasmaCrucibleBlock::new);

    public PlasmaCrucibleBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<PlasmaCrucibleBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlasmaCrucibleBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return MetallurgyContent.PLASMA_CRUCIBLE_BE.get();
    }
}
