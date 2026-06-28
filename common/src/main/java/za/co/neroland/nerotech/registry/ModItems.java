package za.co.neroland.nerotech.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import za.co.neroland.nerolandcore.registry.CoreCreativeTab;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroTech's Tier-1 component and processing items, registered cross-loader through
 * the {@link RegistrationProvider} seam over the vanilla item registry.
 *
 * <p>NeroTech does not re-register Core's shared materials (Nero Alloy, Starsteel,
 * Void Crystal, Plasma Glass) — those live in Core. These are NeroTech-specific
 * crafting components plus Earth-metal processing dusts the Tier-1 ore processor
 * (Stage 2) will output. Every item is appended to Core's shared "Neroland"
 * creative tab via {@link CoreCreativeTab#add}.
 *
 * <p>Tagging (hand-authored in {@code common/src/main/resources}, no datagen):
 * dusts carry {@code c:dusts/<metal>} (external interop with Mekanism/Create/…);
 * the machine components carry the internal {@code nerotech:machine_components} tag
 * for recipe checks.
 */
public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, NeroTechCommon.MOD_ID);

    // --- Machine crafting components ----------------------------------------
    /** The universal machine casing — crafting base shared by every Tier-1 machine (Stage 2). */
    public static final RegistryEntry<Item> MACHINE_FRAME = item("machine_frame");
    /** Basic electronic component — the "logic" ingredient in machine recipes. */
    public static final RegistryEntry<Item> CIRCUIT_BOARD = item("circuit_board");
    /** Wound conductive coil — the "power" ingredient in generator/motor recipes. */
    public static final RegistryEntry<Item> NERO_COIL = item("nero_coil");

    // --- Earth-metal processing dusts (ore processor output; c:dusts/<m>) ----
    public static final RegistryEntry<Item> IRON_DUST = item("iron_dust");
    public static final RegistryEntry<Item> COPPER_DUST = item("copper_dust");
    public static final RegistryEntry<Item> GOLD_DUST = item("gold_dust");

    /** Every NeroTech item, in display order, for the shared {@link CoreCreativeTab}. */
    private static List<RegistryEntry<Item>> creativeOrder() {
        return List.of(
                MACHINE_FRAME, CIRCUIT_BOARD, NERO_COIL,
                IRON_DUST, COPPER_DUST, GOLD_DUST);
    }

    private static RegistryEntry<Item> item(String name) {
        return ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }

    /** Append every NeroTech item to Core's shared Neroland creative tab. */
    public static void addToCreativeTab() {
        creativeOrder().forEach(entry -> CoreCreativeTab.add(entry::get));
    }

    private ModItems() {
    }

    /** Force class-load so the static registrations run (eager on Fabric). */
    public static void init() {
    }
}
