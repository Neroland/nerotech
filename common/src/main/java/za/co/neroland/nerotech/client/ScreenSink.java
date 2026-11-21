package za.co.neroland.nerotech.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * A loader's menu-screen registration entry point, handed to every machine family's
 * {@code <Family>Content.screens(ScreenSink)} by the loader client setup (Fabric
 * {@code MenuScreens.register}, NeoForge {@code RegisterMenuScreensEvent.register}, Forge
 * {@code MenuScreens.register} on the enqueued work). Common stays loader-import-free.
 */
@FunctionalInterface
public interface ScreenSink {

    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
            MenuScreens.ScreenConstructor<M, U> constructor);
}
