package za.co.neroland.nerotech.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.client.RendererSink;
import za.co.neroland.nerotech.client.ScreenSink;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.chemistry.ChemistryContent;
import za.co.neroland.nerotech.machine.components.ComponentsContent;
import za.co.neroland.nerotech.machine.extraction.ExtractionContent;
import za.co.neroland.nerotech.machine.foundry.FoundryContent;
import za.co.neroland.nerotech.machine.fusion.FusionContent;
import za.co.neroland.nerotech.machine.heat.HeatContent;
import za.co.neroland.nerotech.machine.metallurgy.MetallurgyContent;
import za.co.neroland.nerotech.machine.utility.UtilityContent;

/**
 * The eight 0.8.0 machine families, in a fixed order. Every central place iterates this list once
 * (registry init after the four providers, the {@code own*MachineTypes()} lists, the creative tab, the
 * three loaders' screen registration and {@code ClientBlockEntityRenderers.registerAll}), so a family
 * package adds a machine by editing only its own {@code <Family>Content} class.
 *
 * <p>Registration order: a family registers into {@link ModBlocks#BLOCKS}, {@link ModItems#ITEMS},
 * {@link ModBlockEntities#BLOCK_ENTITIES} and {@link ModMenuTypes#MENUS} from its static fields, so on
 * Fabric (eager) its entries register when its class first loads and on NeoForge/Forge they join the
 * same DeferredRegisters the loader entry point flushes. Inside a Content class declare blocks first,
 * then items, then block-entity types, then menus.
 */
public final class FamilyRegistry {

    private static final List<MachineFamily> FAMILIES = List.of(
            new MachineFamily("metallurgy", MetallurgyContent::init, MetallurgyContent::machineTypes,
                    MetallurgyContent::screens, MetallurgyContent::renderers, MetallurgyContent::creativeContents),
            new MachineFamily("foundry", FoundryContent::init, FoundryContent::machineTypes,
                    FoundryContent::screens, FoundryContent::renderers, FoundryContent::creativeContents),
            new MachineFamily("chemistry", ChemistryContent::init, ChemistryContent::machineTypes,
                    ChemistryContent::screens, ChemistryContent::renderers, ChemistryContent::creativeContents),
            new MachineFamily("components", ComponentsContent::init, ComponentsContent::machineTypes,
                    ComponentsContent::screens, ComponentsContent::renderers, ComponentsContent::creativeContents),
            new MachineFamily("heat", HeatContent::init, HeatContent::machineTypes,
                    HeatContent::screens, HeatContent::renderers, HeatContent::creativeContents),
            new MachineFamily("extraction", ExtractionContent::init, ExtractionContent::machineTypes,
                    ExtractionContent::screens, ExtractionContent::renderers, ExtractionContent::creativeContents),
            new MachineFamily("fusion", FusionContent::init, FusionContent::machineTypes,
                    FusionContent::screens, FusionContent::renderers, FusionContent::creativeContents),
            new MachineFamily("utility", UtilityContent::init, UtilityContent::machineTypes,
                    UtilityContent::screens, UtilityContent::renderers, UtilityContent::creativeContents));

    private static boolean initialised;

    private FamilyRegistry() {
    }

    /** The families, in order. */
    public static List<MachineFamily> families() {
        return FAMILIES;
    }

    /** Run every family's registrations, once (from {@code ModRegistries.init()}, after the four providers). */
    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        for (MachineFamily family : FAMILIES) {
            family.init().run();
        }
    }

    /** Every family's block-entity types on {@code surface}, in family order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> machineTypes(
            MachineTypeRegistry.Surface surface) {
        List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> out = new ArrayList<>();
        for (MachineFamily family : FAMILIES) {
            out.addAll(family.machineTypes().apply(surface));
        }
        return out;
    }

    /** Every family's creative-tab entries, in family order. */
    public static List<ItemLike> creativeContents() {
        List<ItemLike> out = new ArrayList<>();
        for (MachineFamily family : FAMILIES) {
            out.addAll(family.creativeContents().get());
        }
        return out;
    }

    /** Client only: register every family's menu screens. */
    public static void screens(ScreenSink sink) {
        for (MachineFamily family : FAMILIES) {
            family.screens().accept(sink);
        }
    }

    /** Client only: register every family's block-entity renderers. */
    public static void renderers(RendererSink sink) {
        for (MachineFamily family : FAMILIES) {
            family.renderers().accept(sink);
        }
    }
}
