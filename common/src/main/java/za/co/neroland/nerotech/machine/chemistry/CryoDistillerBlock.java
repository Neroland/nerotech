package za.co.neroland.nerotech.machine.chemistry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** The cryo distiller block — directional, ticks its {@link CryoDistillerBlockEntity}. */
public class CryoDistillerBlock extends NeroTechMachineBlock {

    public static final MapCodec<CryoDistillerBlock> CODEC = BlockCodecs.simple(CryoDistillerBlock::new);

    public CryoDistillerBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<CryoDistillerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CryoDistillerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ChemistryContent.CRYO_DISTILLER_BE.get();
    }
}
