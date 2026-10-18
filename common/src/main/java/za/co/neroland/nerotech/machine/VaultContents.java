package za.co.neroland.nerotech.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The {@code nerotech:vault_contents} data component: a broken {@link SingularityVaultBlockEntity
 * Singularity Vault}'s bulk store, carried on the dropped vault item when it is too large to spill
 * as loose stacks (see {@link SingularityVaultBlockEntity#SPILL_CAP_STACKS}). Placing the item
 * restores the store. Item type + count only — no player data (POPIA/GDPR).
 *
 * <p>Uses {@link ItemStackTemplate} rather than {@code ItemStack} so the component has value
 * equality (data components are compared with {@code equals}).</p>
 */
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
