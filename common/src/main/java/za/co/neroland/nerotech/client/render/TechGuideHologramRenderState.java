package za.co.neroland.nerotech.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** Render state for the Tech Guide pedestal hologram: the floating next-step icon + animation. */
public class TechGuideHologramRenderState extends BlockEntityRenderState {

    /** Whether the pedestal is loaded (hologram visible). */
    public boolean visible;
    /** Y-spin in degrees. */
    public float spin;
    /** Vertical bob offset (blocks). */
    public float bob;
    /** The hologram icon's pooled item render state. */
    public final ItemStackRenderState renderState = new ItemStackRenderState();
}
