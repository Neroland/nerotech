package za.co.neroland.nerotech.machine;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import za.co.neroland.nerotech.item.ConfiguratorItem;
import za.co.neroland.nerotech.machine.AcceleratorMath.Heading;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * Accelerator Guide Coil — the free-form building block of the Particle Accelerator. Guides are laid
 * at the SAME Y level as the Accelerator Controller and steer the beam: each one carries a
 * {@link Bend} that turns the heading 45° left, 45° right, or not at all. Chain enough of them into a
 * closed loop and the controller can circulate a particle around it.
 *
 * <p>Nothing about a guide is a multiblock: they need not touch, they need not be square, and they
 * are found by the controller's ray-march along the current heading (see {@link AcceleratorPath}).
 * The only geometry rules are the physics ones in {@link AcceleratorMath} — segments too long for the
 * beam's speed lose it, bends too sharp for its speed crash it.
 *
 * <p>Right-click with an empty hand or with the Configurator to cycle
 * {@code STRAIGHT → LEFT → RIGHT}. The bend is a blockstate, so it survives reloads and reads back
 * from the world with no block entity; the actionbar names the new setting too, because the arrow on
 * the top face only shows once a controller has traced through this guide.
 *
 * <p>That arrow is {@link #HEADING}: a pure DISPLAY blockstate the Accelerator Controller writes
 * after every successful trace, holding the heading the beam LEAVES this guide on (the bend already
 * applied). {@link Indicator#NONE} — the plain top face — means "not part of any closed beam line",
 * which is the first thing to look for when a ring will not close. Nothing reads it: the trace in
 * {@link AcceleratorPath} still keys off {@link #BEND} alone, so a stale arrow can never change the
 * physics. Cycling a bend by hand can therefore leave the arrow briefly wrong; the controller's
 * re-trace cadence (≤5s, or immediately on its own neighbour change) corrects it.
 */
public class AcceleratorGuideBlock extends Block {

    /** Which way the beam leaves this guide, relative to the way it arrived. */
    public enum Bend implements StringRepresentable {
        STRAIGHT,
        LEFT,
        RIGHT;

        /** Stable view for cycling — never call {@code values()} per interaction. */
        private static final Bend[] VALUES = values();

        /** Next setting in the right-click cycle. */
        public Bend next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** {@code gui.nerotech.accelerator_guide.bend.<lowercase name>} (see en_us.json). */
        public String translationKey() {
            return "gui.nerotech.accelerator_guide.bend." + getSerializedName();
        }
    }

    /**
     * The arrow drawn on a guide's top face — the heading the beam leaves it on, or
     * {@link Indicator#NONE} for a guide no closed beam line runs through. Display only; see the
     * class javadoc.
     *
     * <p>Declared in {@link Heading}'s own clockwise order after {@code NONE}, so
     * {@link Indicator#of(Heading)} is an ordinal shift rather than a switch.
     */
    public enum Indicator implements StringRepresentable {
        NONE,
        NORTH,
        NORTH_EAST,
        EAST,
        SOUTH_EAST,
        SOUTH,
        SOUTH_WEST,
        WEST,
        NORTH_WEST;

        /** Stable view — never call {@code values()} inside a trace. */
        private static final Indicator[] VALUES = values();

        /** The indicator for a beam heading (never {@code NONE}). */
        public static Indicator of(Heading heading) {
            return VALUES[heading.ordinal() + 1];
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Bend> BEND = EnumProperty.create("bend", Bend.class);

    public static final EnumProperty<Indicator> HEADING = EnumProperty.create("heading", Indicator.class);

    public static final MapCodec<AcceleratorGuideBlock> CODEC = simpleCodec(AcceleratorGuideBlock::new);

    @SuppressWarnings("this-escape")
    public AcceleratorGuideBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BEND, Bend.STRAIGHT)
                .setValue(HEADING, Indicator.NONE));
    }

    @Override
    protected MapCodec<AcceleratorGuideBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BEND, HEADING);
    }

    /** The Configurator cycles the bend too — the same wrench that tunes machine faces tunes guides. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ConfiguratorItem) {
            return cycle(state, level, pos, player);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return cycle(state, level, pos, player);
    }

    /**
     * Server-side bend cycle: write the new blockstate (a normal block update, so any controller next
     * to it re-traces immediately and every other controller picks it up on its re-trace cadence) and
     * name the new setting on the actionbar. Block state only — no player data (POPIA/GDPR).
     */
    private static InteractionResult cycle(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Bend next = state.getValue(BEND).next();
        level.setBlock(pos, state.setValue(BEND, next), Block.UPDATE_ALL);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("gui.nerotech.accelerator_guide.set",
                    Component.translatable(next.translationKey())), true);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Point the guide at {@code pos} along {@code indicator} — the display-only write an Accelerator
     * Controller makes for every guide on a freshly traced beam line (and, with {@link Indicator#NONE},
     * for every guide that just fell off one).
     *
     * <p>Compare-before-set: an unchanged arrow writes nothing at all, so a re-trace of a settled ring
     * costs zero block updates. The write itself carries {@code UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE} —
     * clients see the new arrow, but no neighbour update fires, so this can never bounce back into the
     * controller's own path invalidation. Unloaded positions are skipped rather than loaded.
     *
     * @return {@code true} when this position holds a guide (whether or not the state changed)
     */
    public static boolean setIndicator(Level level, BlockPos pos, Indicator indicator) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.ACCELERATOR_COIL.get())) {
            return false;
        }
        if (state.getValue(HEADING) != indicator) {
            level.setBlock(pos, state.setValue(HEADING, indicator),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        return true;
    }
}
