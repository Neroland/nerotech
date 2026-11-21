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

/** Plate Press block: directional, ticks its {@link PlatePressBlockEntity}. */
public class PlatePressBlock extends NeroTechMachineBlock {

    public static final MapCodec<PlatePressBlock> CODEC = BlockCodecs.simple(PlatePressBlock::new);

    public PlatePressBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<PlatePressBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlatePressBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return MetallurgyContent.PLATE_PRESS_BE.get();
    }
}
