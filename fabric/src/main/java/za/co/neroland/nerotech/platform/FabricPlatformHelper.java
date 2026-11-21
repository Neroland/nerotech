package za.co.neroland.nerotech.platform;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import za.co.neroland.nerotech.NeroTechCommon;

/** Fabric {@link IPlatformHelper}. Registered via {@code META-INF/services}. */
public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public String getModVersion() {
        return FabricLoader.getInstance().getModContainer(NeroTechCommon.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(m -> m.getMetadata().getId() + " " + m.getMetadata().getVersion().getFriendlyString())
                .sorted()
                .toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean mayBreak(net.minecraft.server.level.ServerPlayer player, net.minecraft.server.level.ServerLevel level,
            net.minecraft.core.BlockPos pos) {
        return net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(level, player, pos, level.getBlockState(pos), level.getBlockEntity(pos));
    }
}
