package za.co.neroland.nerotech.platform;

import java.util.List;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import za.co.neroland.nerotech.NeroTechCommon;

/** Forge {@link IPlatformHelper}. Registered via {@code META-INF/services}. */
public final class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public String getModVersion() {
        return ModList.getModContainerById(NeroTechCommon.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public List<String> getLoadedModIds() {
        return ModList.getMods().stream()
                .map(m -> m.getModId() + " " + m.getVersion())
                .sorted()
                .toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }

    @Override
    public boolean mayBreak(net.minecraft.server.level.ServerPlayer player, net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos) {
        // -1 means the break event was cancelled or denied; anything else is the XP it would drop.
        return net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level,
                player.gameMode.getGameModeForPlayer(), player, pos) != -1;
    }
}
