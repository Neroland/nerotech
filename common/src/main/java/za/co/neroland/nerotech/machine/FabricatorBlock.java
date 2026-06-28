package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Fabricator block — directional, ticks its {@link FabricatorBlockEntity}. */
public class FabricatorBlock extends NeroTechMachineBlock {

    public static final MapCodec<FabricatorBlock> CODEC = simpleCodec(FabricatorBlock::new);

    public FabricatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FabricatorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricatorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.FABRICATOR.get();
    }
}
