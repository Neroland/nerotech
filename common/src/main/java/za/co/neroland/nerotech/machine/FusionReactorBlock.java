package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Fusion Reactor block — late-game (crafted from Starsteel); ticks its {@link FusionReactorBlockEntity}. */
public class FusionReactorBlock extends NeroTechMachineBlock {

    public static final MapCodec<FusionReactorBlock> CODEC = simpleCodec(FusionReactorBlock::new);

    public FusionReactorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FusionReactorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionReactorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.FUSION_REACTOR.get();
    }
}
