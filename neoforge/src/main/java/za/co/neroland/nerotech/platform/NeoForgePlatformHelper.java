package za.co.neroland.nerotech.platform;

import java.util.List;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

import za.co.neroland.nerotech.NeroTechCommon;

/** NeoForge {@link IPlatformHelper}. Registered via {@code META-INF/services}. */
public final class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //? if >=1.21.9 {
        return !FMLEnvironment.isProduction();
        //?} else {
        /*return !FMLEnvironment.production;
        *///?}
    }

    @Override
    public boolean isClient() {
        //? if >=1.21.9 {
        return FMLEnvironment.getDist() == Dist.CLIENT;
        //?} else {
        /*return FMLEnvironment.dist == Dist.CLIENT;
        *///?}
    }

    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById(NeroTechCommon.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    @Override
    public List<String> getLoadedModIds() {
        return ModList.get().getMods().stream()
                .map(m -> m.getModId() + " " + m.getVersion())
                .sorted()
                .toList();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
