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

/** Component Forge block: directional, ticks its {@link ComponentForgeBlockEntity}. */
public class ComponentForgeBlock extends NeroTechMachineBlock {

    public static final MapCodec<ComponentForgeBlock> CODEC = BlockCodecs.simple(ComponentForgeBlock::new);

    public ComponentForgeBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<ComponentForgeBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComponentForgeBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ComponentsContent.COMPONENT_FORGE_BE.get();
    }
}
