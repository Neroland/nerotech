package za.co.neroland.nerotech.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Item tags NeroTech matches against. Tier-2 content consumes Nerospace/planet materials purely through
 * Core's {@code c:} convention tags (no Nerospace import), and reactor fuel is recognised by a
 * datapack-overridable tag so Nerospace, Mekanism or any pack can supply it.
 */
public final class NeroTechTags {

    /** Datapack-overridable reactor fuel tag (NeroTech's {@code fusion_cell} + any pack-added fuels). */
    public static final TagKey<Item> FUSION_FUELS = itemTag(NeroTechCommon.MOD_ID, "fusion_fuels");

    /** Tier-2 fusion fuel ({@code plasma_cell}) — the multiblock reactor's mid output band. */
    public static final TagKey<Item> FUSION_FUEL_TIER2 = itemTag(NeroTechCommon.MOD_ID, "fusion_fuel/tier2");
    /** Tier-3 fusion fuel ({@code stellar_cell}) — the multiblock reactor's top output band. */
    public static final TagKey<Item> FUSION_FUEL_TIER3 = itemTag(NeroTechCommon.MOD_ID, "fusion_fuel/tier3");
    /**
     * Tier-4 fusion fuel ({@code antimatter_cell}) — Collider-only, and only the 7³ shell will
     * contain it. Burning it adds a flat +2 to the reactor's heat rate: the meltdown risk IS the
     * price of the longest burn in the mod.
     */
    public static final TagKey<Item> FUSION_FUEL_TIER4 = itemTag(NeroTechCommon.MOD_ID, "fusion_fuel/tier4");

    /**
     * Datapack-overridable Bio Generator fuel tag (Stage D). NeroTech seeds it with dried kelp
     * blocks only; NeroAgriculture — or any pack — adds its own farmed feedstock, and the generator
     * accepts it with no code change and no cross-mod dependency.
     */
    public static final TagKey<Item> BIO_FUELS = itemTag(NeroTechCommon.MOD_ID, "bio_fuels");

    /** Core convention tags — the soft coupling to Nerospace planet materials. */
    public static final TagKey<Item> C_STARSTEEL_INGOTS = cTag("ingots/starsteel");
    public static final TagKey<Item> C_VOID_CRYSTAL_GEMS = cTag("gems/void_crystal");

    private NeroTechTags() {
    }

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
