package za.co.neroland.nerotech.machine.heat;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Heat Exchanger block: machine port on the front, tank port on the back (see {@link HeatExchangerBlockEntity}). */
public class HeatExchangerBlock extends NeroTechMachineBlock {

    public static final MapCodec<HeatExchangerBlock> CODEC = BlockCodecs.simple(HeatExchangerBlock::new);

    public HeatExchangerBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<HeatExchangerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return HeatContent.HEAT_EXCHANGER_BE.get();
    }

    @Override
    //? if >=1.21.2 {
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    //?} else {
    /*protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    *///?}
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger) {
            exchanger.invalidatePorts();
        }
    }
}
