package za.co.neroland.nerotech.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Shared textured-quad helpers for NeroTech's block-entity renderers, distilled from Nerospace's
 * proven {@code SolarPanelRenderer}/{@code QuarryControllerRenderer} geometry code: full-bright
 * emissive light, double-sided faces (cutout-safe from any angle), simple boxes with a full sprite
 * per face, and the MODELS.md heat-readability lerp {@code T_CYAN → H_WARN → H_CRIT}.
 *
 * <p>All geometry is submitted through {@code RenderTypes.entityCutout(texture)} via
 * {@code SubmitNodeCollector.submitCustomGeometry} (the 26.x submit API — never the old
 * {@code render()}). Pure client visuals; no player data anywhere near this (POPIA/GDPR n/a).
 */
public final class MachineRenderHelper {

    /**
     * Packed full-bright light for emissive quads. Submitted custom geometry cannot rely on
     * {@code state.lightCoords} being populated — left at 0 the quads read pitch-black (the
     * Nerospace survey's finding), so every emissive part draws at full brightness and shades
     * only by its normals.
     */
    public static final int FULL_BRIGHT = 0x00F000F0;

    // MODELS.md palette: the heat lerp endpoints (H_* never appears in static base art).
    private static final int[] T_CYAN = {36, 208, 222};
    private static final int[] H_WARN = {255, 178, 56};
    private static final int[] H_CRIT = {255, 84, 56};

    /** Heat-critical RGB (the Fusion Reactor warning strobe overlay). */
    public static final int CRIT_RGB = (255 << 16) | (84 << 8) | 56;

    private MachineRenderHelper() {
    }

    /** {@code nerotech:textures/block/<name>.png}. */
    public static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "textures/block/" + name + ".png");
    }

    /**
     * The MODELS.md heat lerp: 0 → T_CYAN (cold cyan), 0.5 → H_WARN (amber), 1 → H_CRIT (red).
     * Returns packed 0xRRGGBB; split with {@link #red}/{@link #green}/{@link #blue}.
     */
    public static int heatColor(float heatFraction) {
        float f = Mth.clamp(heatFraction, 0.0F, 1.0F);
        int[] from = f < 0.5F ? T_CYAN : H_WARN;
        int[] to = f < 0.5F ? H_WARN : H_CRIT;
        float t = f < 0.5F ? f * 2.0F : (f - 0.5F) * 2.0F;
        int r = (int) Mth.lerp(t, from[0], to[0]);
        int g = (int) Mth.lerp(t, from[1], to[1]);
        int b = (int) Mth.lerp(t, from[2], to[2]);
        return (r << 16) | (g << 8) | b;
    }

    public static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    /**
     * Rotate the pose from model space (all NeroTech machines are authored facing NORTH) to the
     * block's horizontal facing, matching the blockstate {@code y} rotation exactly
     * (north=0, east=90, south=180, west=270 — clockwise from above, hence the negated angle
     * for the counter-clockwise {@code Axis.YP}). Pivots about the block centre column.
     */
    public static void rotateToFacing(PoseStack poseStack, Direction facing) {
        float yRot = switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        if (yRot != 0.0F) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            poseStack.translate(-0.5F, 0.0F, -0.5F);
        }
    }

    /** A tinted box, every face carrying the full sprite; all faces double-sided (cutout-safe). */
    public static void box(VertexConsumer c, PoseStack.Pose pose, int light, int rgb,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        // top (+Y) / bottom (-Y)
        face(c, pose, light, rgb, 0, 1, 0,
                x0, y1, z0, 0, 0, x0, y1, z1, 0, 1, x1, y1, z1, 1, 1, x1, y1, z0, 1, 0);
        face(c, pose, light, rgb, 0, -1, 0,
                x0, y0, z0, 0, 0, x1, y0, z0, 1, 0, x1, y0, z1, 1, 1, x0, y0, z1, 0, 1);
        // north (-Z) / south (+Z)
        face(c, pose, light, rgb, 0, 0, -1,
                x0, y0, z0, 0, 1, x0, y1, z0, 0, 0, x1, y1, z0, 1, 0, x1, y0, z0, 1, 1);
        face(c, pose, light, rgb, 0, 0, 1,
                x1, y0, z1, 0, 1, x1, y1, z1, 0, 0, x0, y1, z1, 1, 0, x0, y0, z1, 1, 1);
        // west (-X) / east (+X)
        face(c, pose, light, rgb, -1, 0, 0,
                x0, y0, z1, 0, 1, x0, y1, z1, 0, 0, x0, y1, z0, 1, 0, x0, y0, z0, 1, 1);
        face(c, pose, light, rgb, 1, 0, 0,
                x1, y0, z0, 0, 1, x1, y1, z0, 0, 0, x1, y1, z1, 1, 0, x1, y0, z1, 1, 1);
    }

    /** White box convenience overload. */
    public static void box(VertexConsumer c, PoseStack.Pose pose, int light,
            float x0, float y0, float z0, float x1, float y1, float z1) {
        box(c, pose, light, 0xFFFFFF, x0, y0, z0, x1, y1, z1);
    }

    /**
     * A tinted quad emitted BOTH ways (front with the given normal, back reversed) so it shows from
     * either side through the cutout cull — the Nerospace double-sided-face recipe.
     */
    public static void face(VertexConsumer c, PoseStack.Pose pose, int light, int rgb,
            float nx, float ny, float nz,
            float ax, float ay, float az, float au, float av,
            float bx, float by, float bz, float bu, float bv,
            float cx, float cy, float cz, float cu, float cv,
            float dx, float dy, float dz, float du, float dv) {
        vertex(c, pose, light, rgb, ax, ay, az, au, av, nx, ny, nz);
        vertex(c, pose, light, rgb, bx, by, bz, bu, bv, nx, ny, nz);
        vertex(c, pose, light, rgb, cx, cy, cz, cu, cv, nx, ny, nz);
        vertex(c, pose, light, rgb, dx, dy, dz, du, dv, nx, ny, nz);
        vertex(c, pose, light, rgb, dx, dy, dz, du, dv, -nx, -ny, -nz);
        vertex(c, pose, light, rgb, cx, cy, cz, cu, cv, -nx, -ny, -nz);
        vertex(c, pose, light, rgb, bx, by, bz, bu, bv, -nx, -ny, -nz);
        vertex(c, pose, light, rgb, ax, ay, az, au, av, -nx, -ny, -nz);
    }

    /** One vertex with tint + full sprite UV + packed light + normal. */
    public static void vertex(VertexConsumer c, PoseStack.Pose pose, int light, int rgb,
            float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        c.addVertex(pose, x, y, z)
                .setColor(red(rgb), green(rgb), blue(rgb), 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
