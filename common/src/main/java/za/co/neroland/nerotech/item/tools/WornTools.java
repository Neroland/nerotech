package za.co.neroland.nerotech.item.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * The server side of worn tools and of what the Filter Mask protects against, run from the per-loader
 * server tick ({@code PollutionManager.tick}) once every {@code pollutionExposureIntervalTicks}:
 *
 * <ul>
 * <li>worn Thermal Goggles drain their charge ({@code thermalGogglesDrainPerSecond});</li>
 * <li>a player standing in a region at the <b>severe</b> pollution band (twice {@code pollutionEventThreshold})
 * gets brief Weakness and Hunger — unless a charged Filter Mask with a cartridge blocks the pulse
 * ({@link FilterRules}). {@code pollutionExposureEffects=false} switches the hazard off.</li>
 * </ul>
 *
 * One region lookup per online player per pulse; nothing is stored about anyone and nothing is logged.
 */
public final class WornTools {

    private WornTools() {
    }

    public static void tick(MinecraftServer server) {
        int interval = Math.max(20, NeroTechConfig.pollutionExposureIntervalTicks());
        if (server.getTickCount() % interval != 0) {
            return;
        }
        boolean exposure = NeroTechConfig.pollutionExposureEffects();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (head.getItem() instanceof ThermalGogglesItem) {
                int drain = NeroTechConfig.thermalGogglesDrainPerSecond() * interval / 20;
                ToolCharge charge = ChargedToolItem.charge(head);
                if (charge.energy() > 0 && drain > 0) {
                    ChargedToolItem.setCharge(head, charge.withEnergy(ToolRules.drained(charge.energy(), drain)));
                }
            }
            if (!exposure || player.isSpectator() || player.isCreative()
                    || !(player.level() instanceof ServerLevel level)) {
                continue;
            }
            String band = PollutionManager.band(PollutionManager.regionPollution(level, player.blockPosition()));
            if (!"severe".equals(band)) {
                continue;
            }
            if (head.getItem() instanceof FilterMaskItem && blocked(player, head)) {
                continue;
            }
            int duration = interval + 40;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, 0));
        }
    }

    /** One pulse against a worn mask: true when the mask blocked it (charge and a cartridge use spent). */
    private static boolean blocked(ServerPlayer player, ItemStack mask) {
        int cost = NeroTechConfig.filterMaskEnergyPerPulse();
        ToolCharge charge = ChargedToolItem.charge(mask);
        FilterRules.Pulse pulse = FilterRules.pulse(FilterMaskItem.usesLeft(mask), charge.energy(), cost);
        if (!pulse.blocked()) {
            return false;
        }
        ChargedToolItem.setCharge(mask, charge.withEnergy(charge.energy() - Math.max(0, cost)));
        if (pulse.fouled()) {
            mask.remove(ToolsContent.FITTED_FILTER.get());
            ItemStack dirty = new ItemStack(ModItems.DIRTY_FILTER.get());
            if (!player.getInventory().add(dirty)) {
                Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), dirty);
            }
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    Component.translatable("message.nerotech.filter_mask.fouled")));
        } else {
            mask.set(ToolsContent.FITTED_FILTER.get(), pulse.usesLeft());
        }
        return true;
    }
}
