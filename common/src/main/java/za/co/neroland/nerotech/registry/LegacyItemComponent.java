package za.co.neroland.nerotech.registry;

//? if <1.20.5 {
/*import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public record LegacyItemComponent<T>(String key, Codec<T> codec) {
    public T read(ItemStack stack) {
        Tag value = stack.hasTag() ? stack.getTag().get(key) : null;
        return value == null ? null : codec.parse(NbtOps.INSTANCE, value).result().orElse(null);
    }

    public void write(ItemStack stack, T value) {
        Tag encoded = codec.encodeStart(NbtOps.INSTANCE, value).result()
                .orElseThrow(() -> new IllegalArgumentException("Could not encode item key " + key));
        stack.getOrCreateTag().put(key, encoded);
    }

    public void clear(ItemStack stack) {
        stack.removeTagKey(key);
    }
}
*///?}
