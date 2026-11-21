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

/** Crystal Grower block: directional, ticks its {@link CrystalGrowerBlockEntity}. */
public class CrystalGrowerBlock extends NeroTechMachineBlock {

    public static final MapCodec<CrystalGrowerBlock> CODEC = BlockCodecs.simple(CrystalGrowerBlock::new);

    public CrystalGrowerBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<CrystalGrowerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalGrowerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ComponentsContent.CRYSTAL_GROWER_BE.get();
    }
}
