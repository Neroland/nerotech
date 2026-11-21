package za.co.neroland.nerotech.machine.components;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Laser Etcher block: directional, ticks its {@link LaserEtcherBlockEntity}. */
public class LaserEtcherBlock extends NeroTechMachineBlock {

    public static final MapCodec<LaserEtcherBlock> CODEC = BlockCodecs.simple(LaserEtcherBlock::new);

    public LaserEtcherBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<LaserEtcherBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserEtcherBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ComponentsContent.LASER_ETCHER_BE.get();
    }
}
