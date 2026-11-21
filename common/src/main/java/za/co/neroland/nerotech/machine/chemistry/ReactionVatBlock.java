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

/** The reaction vat block — directional, ticks its {@link ReactionVatBlockEntity}. */
public class ReactionVatBlock extends NeroTechMachineBlock {

    public static final MapCodec<ReactionVatBlock> CODEC = BlockCodecs.simple(ReactionVatBlock::new);

    public ReactionVatBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<ReactionVatBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactionVatBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ChemistryContent.REACTION_VAT_BE.get();
    }
}
