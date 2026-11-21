package za.co.neroland.nerotech.registry;

/**
 * Aggregates NeroTech's cross-loader content registries. Called once from
 * {@link za.co.neroland.nerotech.NeroTechCommon#init()}.
 *
 * <p>Order matters on the eager (Fabric) loader: items register on class-load, then
 * NeroTech's own creative tab registers and draws them in. On NeoForge/Forge the
 * DeferredRegisters are created here and flushed to NeroTech's mod bus by the loader
 * entry point ({@code *RegistrationFactory.registerAll(...)}).
 */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void init() {
        ModDataComponents.init();
        za.co.neroland.nerotech.fluid.NeroTechFluids.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenuTypes.init();
        ModSounds.init();
        ModParticles.init();
        // 0.8.0 machine families: each registers its blocks, items, block entities and menus into the
        // four providers above (same order inside every family), so Fabric's eager registration and the
        // NeoForge/Forge DeferredRegister flush treat them exactly like the entries above.
        FamilyRegistry.init();
        // 0.8.0 tools: their data components and items, and the Induction Charger item adapter.
        za.co.neroland.nerotech.item.tools.ToolsContent.init();
        ModRecipeTypes.init();
        ModCreativeTab.init();
    }
}
