package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/** Advanced Fabricator block — late-game (crafted from Starsteel); ticks its {@link AdvancedFabricatorBlockEntity}. */
public class AdvancedFabricatorBlock extends NeroTechMachineBlock {

    public static final MapCodec<AdvancedFabricatorBlock> CODEC = simpleCodec(AdvancedFabricatorBlock::new);

    public AdvancedFabricatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AdvancedFabricatorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedFabricatorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ADVANCED_FABRICATOR.get();
    }
}
