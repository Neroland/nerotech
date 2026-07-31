package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Wireless Power Node block (Stage D) — directional, ticks its {@link WirelessNodeBlockEntity}.
 * Pairing lives on the Configurator (crouch-use one node, then the other); the node itself only
 * validates and services the link.
 */
public class WirelessNodeBlock extends NeroTechMachineBlock {

    public static final MapCodec<WirelessNodeBlock> CODEC = simpleCodec(WirelessNodeBlock::new);

    public WirelessNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<WirelessNodeBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessNodeBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.WIRELESS_NODE.get();
    }
}
