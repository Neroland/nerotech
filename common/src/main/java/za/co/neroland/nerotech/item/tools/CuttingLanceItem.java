package za.co.neroland.nerotech.item.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

import za.co.neroland.nerotech.config.NeroTechConfig;

/**
 * Cutting Lance — ranged single-block mining: use it to cut the block you look at, up to
 * {@code cuttingLanceRange} blocks away (never past {@value LanceLens#MAX_RANGE}). The fitted focus lens
 * ({@link LanceLens}, held in the {@code nerotech:fitted_lens} component) sets what a cut does; sneak-use
 * in the air with a focus lens in the off hand fits it and hands the old one back. Sneak-use on a powered
 * block charges the lance.
 *
 * <p><b>Protection.</b> The cut goes through the same gate as the Power Drill
 * ({@link PowerDrillItem#mayBreak}) and then the player's real break path
 * ({@code ServerPlayerGameMode.destroyBlock}, which fires the loader's break event), so a claim or
 * protection mod can veto it. Nothing about the player is stored or logged.
 *
 * <p><b>Main hand only.</b> A cut fires only from the main hand ({@link #firesFrom}): the break path takes
 * drops, enchantments and {@code mineBlock} from the main-hand item, so an off-hand lance would cut with
 * whatever the main hand holds (a Silk Touch pickaxe doubled Glacite cuts; a Power Drill area-broke
 * around a far block). Fitting a lens still works from either hand. While a cut runs, the lance is the
 * correct tool for any pickaxe- or shovel-mineable block below the diamond tier, so a cut drops what a
 * pickaxe would, once, through the normal break path; outside a cut it is not a mining tool.
 */
public class CuttingLanceItem extends ChargedToolItem {

    /** Blocks the Glacite lens cuts away whole: ones that drop nothing without silk (curated list). */
    public static final TagKey<Block> COLD_PRESERVED = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath("nerotech", "cutting_lance/cold_preserved"));

    /** Set only while this thread runs a lance cut's break path (see {@link #isCorrectToolForDrops}). */
    private static final ThreadLocal<Boolean> CUTTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public CuttingLanceItem(Properties properties) {
        super(properties);
    }

    /** Whether a cut fires from {@code hand}: the main hand only ({@link ToolRules#lanceFiresFrom}). */
    static boolean firesFrom(InteractionHand hand) {
        return ToolRules.lanceFiresFrom(hand == InteractionHand.MAIN_HAND);
    }

    /** Correct for drops only inside a lance cut, for what a diamond pickaxe or shovel could take. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return CUTTING.get() && PowerDrillItem.mineable(state) && !state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.cuttingLanceCapacity();
    }

    public static LanceLens lens(ItemStack stack) {
        String fitted = stack.get(ToolsContent.FITTED_LENS.get());
        return LanceLens.parse(fitted);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            NeroEnergyStorage source = EnergyLookup.INSTANCE.find(context.getLevel(), context.getClickedPos(),
                    context.getClickedFace());
            if (source != null) {
                if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                    chargeFrom(serverPlayer, context.getItemInHand(), source);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (player != null && !context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            act(serverPlayer, context.getHand());
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
        useResult(level, player, hand);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    private InteractionResult useResult(Level level, Player player, InteractionHand hand) {
    *///?}
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            act(serverPlayer, hand);
        }
        return InteractionResult.SUCCESS;
    }

    private void act(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            swapLens(player, stack, hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            return;
        }
        if (!firesFrom(hand) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        LanceLens lens = lens(stack);
        int range = lens.range(NeroTechConfig.cuttingLanceRange());
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos at = hit.getBlockPos();
        int cost = lens.energy(NeroTechConfig.cuttingLanceEnergyPerShot());
        int cooldown = lens.cooldown(NeroTechConfig.cuttingLanceCooldownTicks());
        BlockState before = level.getBlockState(at);
        AABB dropBox = new AABB(at).inflate(0.25D);
        Set<Integer> already = new HashSet<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, dropBox)) {
            already.add(e.getId());
        }
        // The same pure path as the Power Drill: the target is offered to the permission gate, then to the
        // player's real break path; charge is taken only when the block really broke (a veto costs nothing).
        int broken = BreakPlanner.execute(List.of(PowerDrillItem.cell(at)),
                c -> PowerDrillItem.mayBreak(level, player, at) && ready(player, stack, cost),
                c -> cut(player, at), 1);
        if (broken == 0) {
            return;
        }
        List<ItemEntity> drops = new ArrayList<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, dropBox)) {
            if (!already.contains(e.getId()) && !e.isRemoved()) {
                drops.add(e);
            }
        }
        setCharge(stack, ToolRules.afterUse(charge(stack), cost, level.getGameTime(), cooldown));
        level.playSound(null, at, za.co.neroland.nerotech.registry.ModSounds.TOOL_CUTTING_LANCE.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        if (lens == LanceLens.GLACITE && before.is(COLD_PRESERVED)) {
            if (before.is(Blocks.ICE) && level.getFluidState(at).is(Fluids.WATER)) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (player.isCreative()) {
                return;
            }
            List<Item> dropped = new ArrayList<>();
            for (ItemEntity e : drops) {
                dropped.add(e.getItem().getItem());
            }
            if (ToolRules.needsPreservedDrop(dropped, before.getBlock().asItem())) {
                Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
                        new ItemStack(before.getBlock()));
            }
        } else if (lens == LanceLens.EMBER) {
            smeltDrops(level, drops);
        }
    }

    /** The player's real break path, with the lance counted as the correct tool for this one break. */
    private static boolean cut(ServerPlayer player, BlockPos at) {
        CUTTING.set(Boolean.TRUE);
        try {
            return player.gameMode.destroyBlock(at);
        } finally {
            CUTTING.set(Boolean.FALSE);
        }
    }

    /** Whether the lance may cut now (cooldown over, charge enough); tells the player when it is out of charge. */
    private static boolean ready(ServerPlayer player, ItemStack stack, int cost) {
        ToolCharge charge = charge(stack);
        ToolRules.Outcome outcome = ToolRules.attempt(charge.energy(), cost, player.level().getGameTime(), charge.readyAt());
        if (outcome == ToolRules.Outcome.NO_ENERGY) {
            tell(player, Component.translatable("message.nerotech.tool.no_energy", cost));
        }
        return outcome == ToolRules.Outcome.OK;
    }

    /** Swap the fitted lens with a focus lens in {@code other}; the old lens goes back to the player. */
    private static void swapLens(ServerPlayer player, ItemStack lance, InteractionHand other) {
        ItemStack offered = player.getItemInHand(other);
        LanceLens incoming = offered.isEmpty() ? LanceLens.NONE
                : LanceLens.ofItemPath(BuiltInRegistries.ITEM.getKey(offered.getItem()).getPath());
        boolean ours = !offered.isEmpty()
                && "nerotech".equals(BuiltInRegistries.ITEM.getKey(offered.getItem()).getNamespace());
        if (!offered.isEmpty() && (!ours || incoming == LanceLens.NONE)) {
            tell(player, Component.translatable("message.nerotech.cutting_lance.not_a_lens"));
            return;
        }
        LanceLens current = lens(lance);
        if (!offered.isEmpty()) {
            offered.shrink(1); // the lens moves into the lance
        }
        if (incoming == LanceLens.NONE) {
            lance.remove(ToolsContent.FITTED_LENS.get());
        } else {
            lance.set(ToolsContent.FITTED_LENS.get(), incoming.serialized());
        }
        if (current != LanceLens.NONE) {
            ItemStack back = lensItem(current);
            if (!back.isEmpty() && !player.getInventory().add(back)) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), back);
            }
        }
        tell(player, Component.translatable("message.nerotech.cutting_lance.lens",
                Component.translatable("message.nerotech.cutting_lance.lens." + lens(lance).serialized())));
        player.containerMenu.broadcastChanges();
    }

    /** One focus-lens item for {@code lens} (the item the lance hands back). */
    static ItemStack lensItem(LanceLens lens) {
        return switch (lens) {
            case QUARTZ -> new ItemStack(za.co.neroland.nerotech.registry.ModItems.QUARTZ_FOCUS_LENS.get());
            case GLACITE -> new ItemStack(za.co.neroland.nerotech.registry.ModItems.GLACITE_FOCUS_LENS.get());
            case EMBER -> new ItemStack(za.co.neroland.nerotech.registry.ModItems.EMBER_FOCUS_LENS.get());
            case VOID -> new ItemStack(za.co.neroland.nerotech.registry.ModItems.VOID_FOCUS_LENS.get());
            case NONE -> ItemStack.EMPTY;
        };
    }

    /** Ember lens: the items this cut dropped (captured around the break, nothing else) come out smelted. */
    private static void smeltDrops(ServerLevel level, List<ItemEntity> drops) {
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe =
                    level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            //? if >=26.1 {
            ItemStack result = recipe.get().value().assemble(input);
            //?} else {
            /*ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
            *///?}
            if (result.isEmpty()) {
                continue;
            }
            result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * stack.getCount()));
            drop.setItem(result);
        }
    }
}
