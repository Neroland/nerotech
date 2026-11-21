package za.co.neroland.nerotech.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

/**
 * Base of every 0.8.0 NeroTech tool: Core energy held on the item in the {@code nerotech:tool_charge}
 * component ({@link ToolCharge}), a charge bar, the shared cost/cooldown rules ({@link ToolRules}) and the
 * Induction Charger hook ({@link #acceptCharge}, registered through
 * {@code api.WirelessCharging.registerItemCharger} by {@link ToolsContent}). Messages go to the action bar
 * only and are never logged. No player data is stored on the item.
 */
public abstract class ChargedToolItem extends Item {

    protected ChargedToolItem(Properties properties) {
        super(properties);
    }

    /** NE this tool holds (a config key). */
    public abstract int capacity();

    public static ToolCharge charge(ItemStack stack) {
        ToolCharge charge = stack.get(ToolsContent.TOOL_CHARGE.get());
        return charge == null ? ToolCharge.EMPTY : charge;
    }

    public static void setCharge(ItemStack stack, ToolCharge charge) {
        stack.set(ToolsContent.TOOL_CHARGE.get(), charge);
        // The look flags the item models switch on (derived; never edited on their own).
        if (charge.energy() > 0) {
            stack.set(ToolsContent.TOOL_CHARGED.get(), Boolean.TRUE);
        } else {
            stack.remove(ToolsContent.TOOL_CHARGED.get());
        }
        if (stack.getItem() instanceof PowerDrillItem) {
            String mode = PowerDrillItem.MODES[Math.floorMod(charge.mode(), PowerDrillItem.MODES.length)];
            if ("single".equals(mode)) {
                stack.remove(ToolsContent.DRILL_MODE.get());
            } else {
                stack.set(ToolsContent.DRILL_MODE.get(), mode);
            }
        }
    }

    // --- charge bar ----------------------------------------------------------------------------------

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = Math.max(1, capacity());
        return Math.round(13.0F * Math.min(capacity, charge(stack).energy()) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0B33A; // energy amber, like the Survey Scanner (art direction §9)
    }

    // --- energy ----------------------------------------------------------------------------------------

    /**
     * Fill {@code stack} from an Induction Charger (or any caller): up to {@code maxFe}, never past
     * capacity. Returns FE accepted; changes the stack only when not {@code simulate}.
     */
    public long acceptCharge(ItemStack stack, long maxFe, boolean simulate) {
        ToolCharge charge = charge(stack);
        int accepted = ToolRules.chargeAmount(charge.energy(), capacity(), maxFe);
        if (accepted > 0 && !simulate) {
            setCharge(stack, charge.withEnergy(charge.energy() + accepted));
        }
        return accepted;
    }

    /** Charge from a block exposing Core energy (sneak-use, like the Survey Scanner). */
    protected void chargeFrom(ServerPlayer player, ItemStack stack, NeroEnergyStorage source) {
        ToolCharge charge = charge(stack);
        int want = ToolRules.chargeAmount(charge.energy(), capacity(), source.extract(capacity(), true));
        int got = want <= 0 ? 0 : (int) Math.min(want, source.extract(want, false));
        if (got > 0) {
            setCharge(stack, charge.withEnergy(charge.energy() + got));
        }
        tell(player, Component.translatable("message.nerotech.tool.charged", charge(stack).energy(), capacity()));
    }

    /**
     * Spend {@code cost} and start a {@code cooldown} if the tool may act now. On no energy the player is
     * told once; a cooldown is silent.
     */
    protected boolean spend(ServerPlayer player, ItemStack stack, int cost, int cooldown) {
        ToolCharge charge = charge(stack);
        long now = player.level().getGameTime();
        switch (ToolRules.attempt(charge.energy(), cost, now, charge.readyAt())) {
            case COOLDOWN -> {
                return false;
            }
            case NO_ENERGY -> {
                tell(player, Component.translatable("message.nerotech.tool.no_energy", cost)
                        .withStyle(ChatFormatting.GOLD));
                return false;
            }
            default -> {
            }
        }
        setCharge(stack, ToolRules.afterUse(charge, cost, now, cooldown));
        return true;
    }

    /** Take {@code cost} NE without a cooldown; false (and nothing taken) when the tool holds less. */
    protected static boolean drain(ItemStack stack, int cost) {
        ToolCharge charge = charge(stack);
        if (charge.energy() < Math.max(0, cost)) {
            return false;
        }
        setCharge(stack, charge.withEnergy(charge.energy() - Math.max(0, cost)));
        return true;
    }

    /** Action bar only; nothing is logged. */
    protected static void tell(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }
}
