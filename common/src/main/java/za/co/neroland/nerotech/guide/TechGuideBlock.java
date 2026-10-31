package za.co.neroland.nerotech.guide;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * The Tech Guide pedestal block (Nerospace's Star Guide recipe, lectern-style): install a Tech Guide
 * Datapad to load it (right-click opens the progression tree); sneak-right-click returns the datapad;
 * breaking a loaded pedestal drops both. Comparator reads 15 while loaded.
 */
public class TechGuideBlock extends BaseEntityBlock {

    public static final MapCodec<TechGuideBlock> CODEC = BlockCodecs.simple(TechGuideBlock::new);

    public TechGuideBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<TechGuideBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechGuideBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TECH_GUIDE.get(),
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        // Install a Tech Guide Datapad on the bare pedestal.
        if (stack.is(ModItems.TECH_GUIDE_DATAPAD.get())
                && level.getBlockEntity(pos) instanceof TechGuideBlockEntity guide && !guide.hasDatapad()) {
            if (!level.isClientSide() && guide.installDatapad(stack)) {
                level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            //? if >=1.21.2 {
            return InteractionResult.SUCCESS;
            //?} else {
            /*return net.minecraft.world.ItemInteractionResult.SUCCESS;
            *///?}
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TechGuideBlockEntity guide)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!guide.hasDatapad()) {
            //? if >=26.1 {
            player.sendSystemMessage(Component.translatable("message.nerotech.tech_guide.empty"));
            //?} else {
            /*player.displayClientMessage(Component.translatable("message.nerotech.tech_guide.empty"), false);
            *///?}
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            // Return the installed datapad.
            ItemStack datapad = guide.removeDatapad();
            if (!datapad.isEmpty() && !player.addItem(datapad)) {
                //? if >=26.3 {
                /*player.drop(datapad, false, net.minecraft.util.Prediction.SERVER_ONLY);
                *///?} else {
                player.drop(datapad, false);
                //?}
            }
            level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 0.8F);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(guide);
        }
        return InteractionResult.SUCCESS;
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 removal side effects run from the block (block entities get preRemoveSideEffects later).
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TechGuideBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    *///?}
    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    //? if >=1.21.9 {
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
    //?} else {
    /*protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    *///?}
        return level.getBlockEntity(pos) instanceof TechGuideBlockEntity guide ? guide.comparatorSignal() : 0;
    }
}
