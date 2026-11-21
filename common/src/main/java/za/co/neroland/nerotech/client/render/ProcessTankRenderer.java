package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.tank.ProcessTankBlockEntity;
import za.co.neroland.nerotech.machine.tank.TankMedium;

/**
 * Process Tank renderer: the medium behind the glass. Each tank draws its own share; a column
 * fills from the bottom tank up, so the shares of a stack join into one continuous level. A liquid
 * stands as a column to its level; a gas fills the whole tank as a drifting haze whose density
 * follows the amount. An empty tank draws nothing. A passive block: no status lamp.
 *
 * <p>Geometry stays inside the block. Reads only the tank's synced share. No player data.</p>
 */
public class ProcessTankRenderer implements BlockEntityRenderer<ProcessTankBlockEntity, ProcessTankRenderer.State> {

    /** Inside the glass (the panes sit one pixel in from the block's faces). */
    private static final float IN_0 = 1.5F / 16.0F;
    private static final float IN_1 = 14.5F / 16.0F;
    private static final float RING = 2.0F / 16.0F;
    private static final float GAS_DRIFT = 0.01F;
    private static final int DEFAULT_RGB = 0xB8D8E0;

    public static class State extends BlockEntityRenderState {
        TankMedium.Kind kind = TankMedium.Kind.NONE;
        float fill;
        int rgb;
        float yBase;
        float yTop = 1.0F;
        float scroll;
    }

    /**
     * Display colour of a fluid or gas by the path of its id, packed 0xRRGGBB (the art direction's
     * media table). Shared by the chemistry renderers.
     */
    public static int mediumColor(String path) {
        String id = path.startsWith("flowing_") ? path.substring("flowing_".length()) : path;
        switch (id) {
            case "hydrogen": return rgb(176, 206, 235);
            case "oxygen": return rgb(150, 226, 214);
            case "deuterium": return rgb(112, 146, 240);
            case "helium_3": return rgb(244, 188, 200);
            case "methane": return rgb(176, 224, 140);
            case "nitrogen": return rgb(150, 162, 190);
            case "carbon_dioxide": return rgb(168, 156, 146);
            case "cryo_coolant": return rgb(70, 170, 240);
            case "sulfuric_acid": return rgb(188, 220, 60);
            case "polymer_resin": return rgb(228, 200, 156);
            case "water": return rgb(58, 127, 224);
            case "lava": return rgb(224, 120, 42);
            default: return DEFAULT_RGB;
        }
    }

    private static int rgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    /** The {@code column} blockstate value (single, bottom, middle, top); single when absent. */
    private static String column(BlockState blockState) {
        for (Property<?> property : blockState.getProperties()) {
            if ("column".equals(property.getName())) {
                return valueName(blockState, property);
            }
        }
        return "single";
    }

    private static <T extends Comparable<T>> String valueName(BlockState blockState, Property<T> property) {
        return property.getName(blockState.getValue(property));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ProcessTankBlockEntity tank, State state, float partialTick,
            Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPos, breakProgress);
        TankMedium medium = tank.ownMedium();
        long amount = tank.ownAmount();
        if (medium.isEmpty() || amount <= 0L) {
            state.kind = TankMedium.Kind.NONE;
            state.fill = 0.0F;
            return;
        }
        state.kind = medium.kind();
        state.fill = Mth.clamp(amount / (float) ProcessTankBlockEntity.CAPACITY_MB, 0.0F, 1.0F);
        Identifier id = Identifier.tryParse(medium.id());
        state.rgb = id == null ? DEFAULT_RGB : mediumColor(id.getPath());
        String column = column(tank.getBlockState());
        state.yBase = "single".equals(column) || "bottom".equals(column) ? RING : 0.0F;
        state.yTop = "single".equals(column) || "top".equals(column) ? 1.0F - RING : 1.0F;
        Level level = tank.getLevel();
        state.scroll = level == null || !NeroTechConfig.renderAnimationsEnabled() ? 0.0F
                : ((level.getGameTime() % 2400L) + partialTick) * GAS_DRIFT;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (state.kind == TankMedium.Kind.NONE || state.fill <= 0.0F) {
            return;
        }
        int light = MachineRenderHelper.FULL_BRIGHT;
        int rgb = state.rgb;
        float y0 = state.yBase;
        if (state.kind == TankMedium.Kind.GAS) {
            float y1 = state.yTop;
            float scroll = state.scroll;
            collector.order(1).submitCustomGeometry(poseStack,
                    RenderTypes.entityCutout(MachineRenderHelper.gasTexture(state.fill)),
                    (pose, c) -> MachineRenderHelper.fluidColumn(c, pose, light, rgb,
                            IN_0, y0, IN_0, IN_1, y1, IN_1, scroll));
            return;
        }
        float y1 = y0 + (state.yTop - y0) * state.fill;
        collector.order(1).submitCustomGeometry(poseStack,
                RenderTypes.entityCutout(MachineRenderHelper.FLUID_TEX),
                (pose, c) -> MachineRenderHelper.fluidColumn(c, pose, light, rgb,
                        IN_0, y0, IN_0, IN_1, y1, IN_1, 0.0F));
    }
}
