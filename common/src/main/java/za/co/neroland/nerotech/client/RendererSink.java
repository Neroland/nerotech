package za.co.neroland.nerotech.client;

import java.util.function.Supplier;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Block-entity-renderer registration for the machine families, handed to every
 * {@code <Family>Content.renderers(RendererSink)} from {@link ClientBlockEntityRenderers#registerAll}.
 * It wraps the renderer exactly like the built-in entries (the pre-1.21.9 legacy adapter included), so
 * a family registers {@code sink.add(MY_TYPE.get(), MyRenderer::new)} and nothing else.
 */
@FunctionalInterface
public interface RendererSink {

    <T extends BlockEntity, S extends BlockEntityRenderState> void add(BlockEntityType<? extends T> type,
            Supplier<? extends BlockEntityRenderer<T, S>> renderer);
}
