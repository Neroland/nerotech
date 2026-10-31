package za.co.neroland.nerotech.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?}

/**
 * The {@code nerotech:vault_contents} data component: a broken {@link SingularityVaultBlockEntity
 * Singularity Vault}'s bulk store, carried on the dropped vault item when it is too large to spill
 * as loose stacks (see {@link SingularityVaultBlockEntity#SPILL_CAP_STACKS}). Placing the item
 * restores the store. Item type + count only — no player data (POPIA/GDPR).
 *
 * <p>Uses {@link ItemStackTemplate} rather than {@code ItemStack} so the component has value
 * equality (data components are compared with {@code equals}).</p>
 */
//? if >=26.1 {
public record VaultContents(ItemStackTemplate type, long count) {

    public static final Codec<VaultContents> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("type").forGetter(VaultContents::type),
            Codec.LONG.fieldOf("count").forGetter(VaultContents::count)
    ).apply(inst, VaultContents::new));

    /** The stored item as a single-count prototype. */
    public ItemStack prototype() {
        return this.type.create().copyWithCount(1);
    }
}
//?} else {
/*// Before 26.1 there is no ItemStackTemplate: the type is a single-count ItemStack, and equality is
// spelled out (item + components + count) so the component still compares by value.
public record VaultContents(ItemStack type, long count) {

    public static final Codec<VaultContents> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.CODEC.fieldOf("type").forGetter(VaultContents::type),
            Codec.LONG.fieldOf("count").forGetter(VaultContents::count)
    ).apply(inst, VaultContents::new));

    // The stored item as a single-count prototype.
    public ItemStack prototype() {
        return this.type.copyWithCount(1);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VaultContents that && this.count == that.count
                && ItemStack.isSameItemSameComponents(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return 31 * ItemStack.hashItemAndComponents(this.type) + Long.hashCode(this.count);
    }
}
*///?}
