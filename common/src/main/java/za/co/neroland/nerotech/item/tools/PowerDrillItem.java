package za.co.neroland.nerotech.item.tools;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Power Drill — a charged pickaxe-and-shovel with three modes, cycled by sneak-using it in the air:
 * <b>single</b>, <b>area</b> (the 3×3 plane facing you) and <b>vein</b> (up to
 * {@code powerDrillVeinMaxBlocks} connected blocks of the same kind, hard cap 64, and only from a block in
 * {@link #VEIN_BLOCKS}; anything else breaks alone). Every block costs
 * {@code powerDrillEnergyPerBlock}; with no charge it mines like a bare hand.
 *
 * <p><b>Protection.</b> Each extra block goes through the player's real break path,
 * {@code ServerPlayerGameMode.destroyBlock}, which fires the loader's break event (NeoForge and Forge
 * {@code BlockEvent.BreakEvent}, Fabric {@code PlayerBlockBreakEvents.BEFORE}), so a claim or protection
 * mod can veto any one of them. Before that, a block is skipped unless the player may interact there
 * ({@code Level.mayInteract}, spawn protection) and is not restricted ({@code blockActionRestricted},
 * adventure mode, world border), and blocks with a block entity, unloaded blocks and blocks this tier
 * cannot mine are never taken. The planning is pure ({@link BreakPlanner}) and tested with a denying
 * predicate. Nothing about the player is stored.
 */
public class PowerDrillItem extends ChargedToolItem {

    /** The three modes, in cycle order (stored as {@link ToolCharge#mode()}). */
    public static final String[] MODES = {"single", "area", "vein"};

    /** Blocks vein mode walks: ores, gravel, clay and glowstone by default. */
    public static final TagKey<Block> VEIN_BLOCKS = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath("nerotech", ToolRules.DRILL_VEIN_TAG_PATH));

    /**
     * Re-entry guard: the extra breaks call {@link #mineBlock} again through destroyBlock. Per thread, so
     * an integrated server and its client never see each other's flag.
     */
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public PowerDrillItem(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.powerDrillCapacity();
    }

    static boolean mineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    static boolean diamondCanHarvest(BlockState state) {
        //? if >=1.20.5 {
        return !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        //?} else {
        /*var vanillaTool = state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                ? net.minecraft.world.item.Items.DIAMOND_PICKAXE : net.minecraft.world.item.Items.DIAMOND_SHOVEL;
        return new ItemStack(vanillaTool).isCorrectToolForDrops(state);
        *///?}
    }

    private static boolean charged(ItemStack stack) {
        return charge(stack).energy() >= Math.max(1, NeroTechConfig.powerDrillEnergyPerBlock());
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return charged(stack) && mineable(state) ? 10.0F : 1.0F;
    }

    //? if >=1.20.5 {
    @Override
    //?}
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return charged(stack) && mineable(state) && diamondCanHarvest(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (BREAKING.get() || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return true;
        }
        int cost = NeroTechConfig.powerDrillEnergyPerBlock();
        if (state.getDestroySpeed(level, pos) == 0.0F || !drain(stack, cost)) {
            return true;
        }
        serverLevel.playSound(null, pos, za.co.neroland.nerotech.registry.ModSounds.TOOL_POWER_DRILL.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
        int mode = charge(stack).mode() % MODES.length;
        if (mode == 0 || !mineable(state) || (mode == 2 && !ToolRules.drillWalksVein(state.is(VEIN_BLOCKS)))) {
            return true;
        }
        BreakPlanner.Cell origin = cell(pos);
        List<BreakPlanner.Cell> plan = mode == 1
                ? BreakPlanner.area(origin, axis(Direction.orderedByNearest(player)[0]))
                : BreakPlanner.vein(origin, c -> {
                    BlockPos at = pos(c);
                    return serverLevel.isLoaded(at) && serverLevel.getBlockState(at).is(state.getBlock());
                }, NeroTechConfig.powerDrillVeinMaxBlocks());
        int budget = ToolRules.affordable(charge(stack).energy(), cost, plan.size());
        int broken;
        BREAKING.set(Boolean.TRUE);
        try {
            broken = BreakPlanner.execute(plan, c -> mayBreak(serverLevel, player, pos(c)),
                    c -> player.gameMode.destroyBlock(pos(c)), budget);
        } finally {
            BREAKING.set(Boolean.FALSE);
        }
        drain(stack, broken * Math.max(0, cost));
        return true;
    }

    /**
     * Whether the drill may offer {@code at} to the break path: loaded, solid, no block entity, mineable at
     * this tier, and the player may interact and is not restricted there. The break path's own loader event
     * still decides last.
     */
    static boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos at) {
        if (!level.isLoaded(at)) {
            return false;
        }
        BlockState state = level.getBlockState(at);
        return !state.isAir() && state.getDestroySpeed(level, at) >= 0.0F && level.getBlockEntity(at) == null
                && mineable(state) && diamondCanHarvest(state)
                && level.mayInteract(player, at)
                && !player.blockActionRestricted(level, at, player.gameMode.getGameModeForPlayer());
    }

    static BreakPlanner.Cell cell(BlockPos pos) {
        return new BreakPlanner.Cell(pos.getX(), pos.getY(), pos.getZ());
    }

    static BlockPos pos(BreakPlanner.Cell cell) {
        return new BlockPos(cell.x(), cell.y(), cell.z());
    }

    static BreakPlanner.Axis axis(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> BreakPlanner.Axis.X;
            case Y -> BreakPlanner.Axis.Y;
            case Z -> BreakPlanner.Axis.Z;
        };
    }

    // --- use: sneak in the air cycles the mode; sneak on a powered block charges ----------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        NeroEnergyStorage source = EnergyLookup.INSTANCE.find(context.getLevel(), context.getClickedPos(),
                context.getClickedFace());
        if (source == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            chargeFrom(serverPlayer, context.getItemInHand(), source);
        }
        return InteractionResult.SUCCESS;
    }

    //? if >=1.21.2 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
    //?} else {
    /*@Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        InteractionResult result = useResult(level, player, hand);
        return result == InteractionResult.PASS ? net.minecraft.world.InteractionResultHolder.pass(held)
                : net.minecraft.world.InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    private InteractionResult useResult(Level level, Player player, InteractionHand hand) {
    *///?}
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            int next = (charge(stack).mode() + 1) % MODES.length;
            setCharge(stack, charge(stack).withMode(next));
            tell(serverPlayer, Component.translatable("message.nerotech.power_drill.mode",
                    Component.translatable("message.nerotech.power_drill.mode." + MODES[next])));
        }
        return InteractionResult.SUCCESS;
    }
}
