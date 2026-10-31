package za.co.neroland.nerotech.client.render.legacy;

// Minecraft 1.21.1 - 1.21.8 only (empty on every other line). Part of a small look-alike of the 1.21.9+
// block-entity render-state API (extract a state, then submit geometry), so NeroTech's renderers keep one
// source on every line. The build renames the vanilla imports to these classes below 1.21.9 (see
// legacyRenames), and LegacyBlockEntityRenderer adapts them to the older immediate-mode renderer.

//? if <1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The item to draw, captured at extract time and drawn through the item renderer at submit time.
public class ItemStackRenderState {

    private ItemStack stack = ItemStack.EMPTY;
    private ItemDisplayContext context = ItemDisplayContext.GROUND;
    private Level level;
    private int seed;

    public void update(ItemStack stack, ItemDisplayContext context, Level level, int seed) {
        this.stack = stack;
        this.context = context;
        this.level = level;
        this.seed = seed;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        if (this.stack.isEmpty()) {
            return;
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(this.stack, this.context, light, overlay, poseStack,
                collector.buffers(), this.level, this.seed);
    }
}
*///?}
