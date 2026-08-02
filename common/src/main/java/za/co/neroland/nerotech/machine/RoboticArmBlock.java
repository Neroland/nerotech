package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Robotic Arm block — ticks its {@link RoboticArmBlockEntity}. */
public class RoboticArmBlock extends NeroTechMachineBlock {

    public static final MapCodec<RoboticArmBlock> CODEC = simpleCodec(RoboticArmBlock::new);

    public RoboticArmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RoboticArmBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RoboticArmBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ROBOTIC_ARM.get();
    }
}
