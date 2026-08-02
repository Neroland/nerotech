package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Electrolyzer block — directional, ticks its {@link ElectrolyzerBlockEntity}, and accepts a water
 * bucket on right-click.
 *
 * <p>Core's fluid layer ships no bucket interop of its own (its own Fluid Tank is a plain buffer
 * exposed on the fluid capability), so the bucket handling lives here: hand the machine a water
 * bucket and it swaps you an empty one, exactly like a cauldron. Automated filling goes through
 * Core's fluid capability instead — the tank is exposed on every face.
 */
public class ElectrolyzerBlock extends NeroTechMachineBlock {

    public static final MapCodec<ElectrolyzerBlock> CODEC = simpleCodec(ElectrolyzerBlock::new);

    public ElectrolyzerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ElectrolyzerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyzerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.ELECTROLYZER.get();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof ElectrolyzerBlockEntity electrolyzer)
                    || !electrolyzer.fillFromBucket()) {
                return InteractionResult.CONSUME;   // tank full — no bucket eaten
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, empty);
                } else if (!player.getInventory().add(empty)) {
                    player.drop(empty, false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
