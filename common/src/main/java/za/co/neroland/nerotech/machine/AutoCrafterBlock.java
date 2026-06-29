package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Auto Crafter block — ticks its {@link AutoCrafterBlockEntity}. */
public class AutoCrafterBlock extends NeroTechMachineBlock {

    public static final MapCodec<AutoCrafterBlock> CODEC = simpleCodec(AutoCrafterBlock::new);

    public AutoCrafterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AutoCrafterBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoCrafterBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.AUTO_CRAFTER.get();
    }
}
