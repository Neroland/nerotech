package za.co.neroland.nerotech.registry;

/**
 * Aggregates NeroTech's cross-loader content registries. Called once from
 * {@link za.co.neroland.nerotech.NeroTechCommon#init()}.
 *
 * <p>Order matters on the eager (Fabric) loader: items register on class-load, then
 * they are appended to Core's shared creative tab. On NeoForge/Forge the
 * DeferredRegisters are created here and flushed to NeroTech's mod bus by the loader
 * entry point ({@code *RegistrationFactory.registerAll(...)}).
 */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void init() {
        ModItems.init();
        ModItems.addToCreativeTab();
    }
}
