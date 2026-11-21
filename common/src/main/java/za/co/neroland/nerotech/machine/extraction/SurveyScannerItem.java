package za.co.neroland.nerotech.machine.extraction;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.deposit.DepositService;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModSounds;

/**
 * Survey Scanner — a hand tool that reads the virtual deposit under the chunk you stand in
 * ({@link DepositService#survey}, read-only): what ore, roughly how much is left (a 20 % band) and
 * whether it is untouched, recovering or exhausted. Shown on the action bar only, never logged, and
 * it names no coordinates.
 *
 * <p>Runs on Core energy held on the item ({@link ScannerState}). Sneak-use it on any block that
 * exposes Core energy (a battery bank, a generator, any Nero machine) to charge it from that block.
 * Each scan costs energy and starts a short cooldown. No player data is stored.
 */
public class SurveyScannerItem extends Item {

    public SurveyScannerItem(Properties properties) {
        super(properties);
    }

    public static ScannerState state(ItemStack stack) {
        ScannerState s = stack.get(ExtractionContent.SCANNER_STATE.get());
        return s == null ? ScannerState.EMPTY : s;
    }

    private static void setState(ItemStack stack, ScannerState state) {
        stack.set(ExtractionContent.SCANNER_STATE.get(), state);
    }

    // --- charge bar ----------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, ExtractionSettings.surveyScannerCapacity());
        return Math.round(13.0F * Math.min(capacity, state(stack).energy()) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0B33A; // energy amber (art direction §9)
    }

    // --- use -------------------------------------------------------------------------------------------

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (player.isSecondaryUseActive()) {
            NeroEnergyStorage source = EnergyLookup.INSTANCE.find(level, context.getClickedPos(),
                    context.getClickedFace());
            if (source == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                charge(serverPlayer, context.getItemInHand(), source);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            scan(serverPlayer, context.getItemInHand());
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
            scan(serverPlayer, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    private static void charge(ServerPlayer player, ItemStack stack, NeroEnergyStorage source) {
        ScannerState state = state(stack);
        int capacity = ExtractionSettings.surveyScannerCapacity();
        int want = ScannerRules.chargeAmount(state.energy(), capacity, source.extract(capacity, true));
        int got = want <= 0 ? 0 : (int) Math.min(want, source.extract(want, false));
        if (got > 0) {
            setState(stack, state.withEnergy(state.energy() + got));
        }
        tell(player, Component.translatable("message.nerotech.survey_scanner.charged",
                state(stack).energy(), capacity));
    }

    private static void scan(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ScannerState state = state(stack);
        long now = level.getGameTime();
        int cost = ExtractionSettings.surveyScannerScanCost();
        switch (ScannerRules.attempt(state.energy(), cost, now, state.readyAt())) {
            case COOLDOWN -> {
                return;
            }
            case NO_ENERGY -> {
                tell(player, Component.translatable("message.nerotech.survey_scanner.no_energy", cost)
                        .withStyle(ChatFormatting.GOLD));
                return;
            }
            default -> {
            }
        }
        setState(stack, ScannerRules.afterScan(state, cost, now, ExtractionSettings.surveyScannerCooldownTicks()));
        // The sonar ping (008 §8 tool.scan), heard around the player; nothing about them is kept.
        level.playSound(null, player.blockPosition(), ModSounds.TOOL_SCAN.get(), SoundSource.PLAYERS, 0.7F, 1.0F);

        if (DepositService.disabled(PollutionManager.dimensionId(level), NeroTechConfig.depositDisabledDimensions())) {
            tell(player, Component.translatable("message.nerotech.survey_scanner.disabled"));
            return;
        }
        BlockPos at = player.blockPosition();
        DepositService.Survey survey = DepositService.survey(level, at.getX() >> 4, at.getZ() >> 4);
        if (survey == null) {
            tell(player, Component.translatable("message.nerotech.survey_scanner.none"));
            return;
        }
        int band = ScannerRules.band(survey.remaining(), survey.capacity());
        ScannerRules.Recovery recovery = ScannerRules.recovery(survey.remaining(), survey.capacity());
        tell(player, Component.translatable("message.nerotech.survey_scanner.result",
                new ItemStack(survey.output()).getHoverName(),
                Component.translatable("message.nerotech.survey_scanner.band." + band),
                Component.translatable("message.nerotech.survey_scanner.recovery."
                        + recovery.name().toLowerCase(Locale.ROOT))));
    }

    /** Action bar only; nothing is logged (a survey result could locate a base). */
    private static void tell(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }
}
