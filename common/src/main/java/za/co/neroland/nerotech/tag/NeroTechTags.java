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
