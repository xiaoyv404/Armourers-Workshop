package moe.plushie.armourers_workshop.utils;

import com.apple.library.coregraphics.CGRect;
import com.apple.library.uikit.UIColor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import moe.plushie.armourers_workshop.api.math.IRectangle3f;
import moe.plushie.armourers_workshop.api.math.IRectangle3i;
import moe.plushie.armourers_workshop.api.math.ITransformf;
import moe.plushie.armourers_workshop.compatibility.client.AbstractRenderSystem;
import moe.plushie.armourers_workshop.core.armature.ModelBinder;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderType;
import moe.plushie.armourers_workshop.core.client.other.SkinVertexBufferBuilder;
import moe.plushie.armourers_workshop.init.ModDebugger;
import moe.plushie.armourers_workshop.utils.math.OpenMatrix3f;
import moe.plushie.armourers_workshop.utils.math.OpenMatrix4f;
import moe.plushie.armourers_workshop.utils.math.Rectangle3f;
import moe.plushie.armourers_workshop.utils.math.Rectangle3i;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

@Environment(EnvType.CLIENT)
public final class RenderSystem extends AbstractRenderSystem {

    private static final AtomicInteger extendedMatrixFlags = new AtomicInteger();

    private static final Storage<OpenMatrix3f> extendedNormalMatrix = new Storage<>(OpenMatrix3f.createScaleMatrix(1, 1, 1));
    private static final Storage<OpenMatrix4f> extendedTextureMatrix = new Storage<>(OpenMatrix4f.createScaleMatrix(1, 1, 1));
    private static final Storage<OpenMatrix4f> extendedLightmapTextureMatrix = new Storage<>(OpenMatrix4f.createScaleMatrix(1, 1, 1));
    private static final Storage<OpenMatrix4f> extendedModelViewMatrix = new Storage<>(OpenMatrix4f.createScaleMatrix(1, 1, 1));

    private static final FloatBuffer BUFFER = ObjectUtils.createFloatBuffer(3);

    private static final byte[][][] FACE_MARK_TEXTURES = {
            // 0, 1(w), 2(h), 3(d)
            {{1, 3}, {1, 0}, {0, 0}, {0, 3}},
            {{1, 3}, {1, 0}, {0, 0}, {0, 3}},

            {{1, 2}, {1, 0}, {0, 0}, {0, 2}},
            {{1, 2}, {1, 0}, {0, 0}, {0, 2}},

            {{3, 2}, {3, 0}, {0, 0}, {0, 2}},
            {{3, 2}, {3, 0}, {0, 0}, {0, 2}},
    };

    private static final byte[][][] FACE_MARK_VERTEXES = new byte[][][]{
            {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, -1, 0}}, // -y
            {{1, 1, 1}, {1, 1, 0}, {0, 1, 0}, {0, 1, 1}, {0, 1, 0}},  // +y
            {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}, {0, 0, -1}}, // -z
            {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}, {0, 0, 1}},  // +z
            {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}, {-1, 0, 0}}, // -x
            {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}, {1, 0, 0}},  // +x
    };

    private static final LinkedList<CGRect> clipBounds = new LinkedList<>();


    public static void call(Runnable task) {
        if (isOnRenderThread()) {
            task.run();
        } else {
            recordRenderCall(task::run);
        }
    }

    public static int getPixelColor(int x, int y) {
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        int sx = (int) (x * guiScale);
        int sy = (int) ((window.getGuiScaledHeight() - y) * guiScale);
        BUFFER.rewind();
        GL11.glReadPixels(sx, sy, 1, 1, GL11.GL_RGB, GL11.GL_FLOAT, BUFFER);
        GL11.glFinish();
        int r = Math.round(BUFFER.get() * 255);
        int g = Math.round(BUFFER.get() * 255);
        int b = Math.round(BUFFER.get() * 255);
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    public static void addClipRect(int x, int y, int width, int height) {
        addClipRect(new CGRect(x, y, width, height));
    }

    public static void addClipRect(CGRect rect) {
        if (!clipBounds.isEmpty()) {
            CGRect rect1 = clipBounds.getLast();
            rect = rect.intersection(rect1);
        }
        clipBounds.add(rect);
        applyScissor(rect);
    }

    public static void removeClipRect() {
        if (clipBounds.isEmpty()) {
            return;
        }
        clipBounds.removeLast();
        if (!clipBounds.isEmpty()) {
            applyScissor(clipBounds.getLast());
        } else {
            disableScissor();
        }
    }

    public static void applyScissor(CGRect rect) {
        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        double sx = rect.getX() * scale;
        double sy = window.getHeight() - rect.getMaxY() * scale;
        double sw = rect.getWidth() * scale;
        double sh = rect.getHeight() * scale;
        enableScissor((int) sx, (int) sy, (int) sw, (int) sh);
    }

    public static boolean inScissorRect(CGRect rect1) {
        if (!clipBounds.isEmpty()) {
            CGRect rect = clipBounds.getLast();
            return rect.intersects(rect1);
        }
        return true;
    }

    public static boolean inScissorRect(int x, int y, int width, int height) {
        if (!clipBounds.isEmpty()) {
            CGRect rect = clipBounds.getLast();
            return rect.intersects(x, y, width, height);
        }
        return true;
    }

    public static void drawText(PoseStack poseStack, Font font, FormattedText text, int x, int y, int width, int zLevel, int textColor) {
        drawText(poseStack, font, Collections.singleton(text), x, y, width, zLevel, false, 9, textColor);
    }

    public static void drawShadowText(PoseStack poseStack, Iterable<FormattedText> lines, int x, int y, int width, int zLevel, Font font, int fontSize, int textColor) {
        drawText(poseStack, font, lines, x, y, width, zLevel, true, fontSize, textColor);
    }

    public static void drawText(PoseStack poseStack, Font font, Iterable<FormattedText> lines, int x, int y, int width, int zLevel, boolean shadow, int fontSize, int textColor) {
        float f = fontSize / 9f;
        ArrayList<FormattedText> wrappedTextLines = new ArrayList<>();
        for (FormattedText line : lines) {
            wrappedTextLines.addAll(font.getSplitter().splitLines(line, (int) (width / f), Style.EMPTY));
        }
        poseStack.pushPose();
        poseStack.translate(x, y, zLevel);
        poseStack.scale(f, f, f);
        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        int dx = 0, dy = 0;
        for (FormattedText line : wrappedTextLines) {
            int qx = font.drawInBatch(Language.getInstance().getVisualOrder(line), dx, dy, textColor, shadow, pose, buffers, false, 0, 15728880);
            if (qx == dx) {
                dy += 7;
            } else {
                dy += 10;
            }
        }

        buffers.endBatch();
        poseStack.popPose();

        // drawing text causes the Alpha test to reset
        enableAlphaTest();
    }


    public static void drawLine(PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, MultiBufferSource buffers) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer builder = buffers.getBuffer(SkinRenderType.lines());
        drawLine(pose, x0, y0, z0, x1, y1, z1, color, builder);
    }

    private static void drawLine(PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, VertexConsumer builder) {
        float nx = 0, ny = 0, nz = 0;
        if (x0 != x1) {
            nx = 1;
        }
        if (y0 != y1) {
            ny = 1;
        }
        if (z0 != z1) {
            nz = 1;
        }
        builder.vertex(pose.pose(), x0, y0, z0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(pose.normal(), nx, ny, nz).endVertex();
        builder.vertex(pose.pose(), x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).normal(pose.normal(), nx, ny, nz).endVertex();
    }

    public static void drawBoundingBox(PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, VertexConsumer builder) {
        PoseStack.Pose pose = poseStack.last();
        drawLine(pose, x1, y0, z1, x0, y0, z1, color, builder);
        drawLine(pose, x1, y0, z1, x1, y1, z1, color, builder);
        drawLine(pose, x1, y0, z1, x1, y0, z0, color, builder);
        drawLine(pose, x1, y1, z0, x0, y1, z0, color, builder);
        drawLine(pose, x1, y1, z0, x1, y0, z0, color, builder);
        drawLine(pose, x1, y1, z0, x1, y1, z1, color, builder);
        drawLine(pose, x0, y1, z1, x1, y1, z1, color, builder);
        drawLine(pose, x0, y1, z1, x0, y0, z1, color, builder);
        drawLine(pose, x0, y1, z1, x0, y1, z0, color, builder);
        drawLine(pose, x0, y0, z0, x1, y0, z0, color, builder);
        drawLine(pose, x0, y0, z0, x0, y1, z0, color, builder);
        drawLine(pose, x0, y0, z0, x0, y0, z1, color, builder);
    }

    public static void drawPoint(PoseStack poseStack, @Nullable MultiBufferSource renderTypeBuffer) {
        drawPoint(poseStack, null, 2, renderTypeBuffer);
    }

    public static void drawPoint(PoseStack poseStack, @Nullable Vector3f point, float size, @Nullable MultiBufferSource renderTypeBuffer) {
        drawPoint(poseStack, point, size, size, size, renderTypeBuffer);
    }

    public static void drawPoint(PoseStack poseStack, @Nullable Vector3f point, float width, float height, float depth, @Nullable MultiBufferSource renderTypeBuffer) {
        if (renderTypeBuffer == null) {
            renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        }
        VertexConsumer builder = renderTypeBuffer.getBuffer(SkinRenderType.lines());
        float x0 = 0;
        float y0 = 0;
        float z0 = 0;
        if (point != null) {
            x0 = point.getX();
            y0 = point.getY();
            z0 = point.getZ();
        }
        PoseStack.Pose pose = poseStack.last();
        drawLine(pose, x0 - width, y0, z0, x0 + width, y0, z0, UIColor.RED, builder); // x
        drawLine(pose, x0, y0 - height, z0, x0, y0 + height, z0, UIColor.GREEN, builder); // Y
        drawLine(pose, x0, y0, z0 - depth, x0, y0, z0 + depth, UIColor.BLUE, builder); // Z
    }

    public static void drawTargetBox(PoseStack poseStack, float width, float height, float depth, MultiBufferSource buffers) {
        if (ModDebugger.targetBounds) {
            drawBoundingBox(poseStack, -width / 2, -height / 2, -depth / 2, width / 2, height / 2, depth / 2, UIColor.ORANGE, buffers);
            drawPoint(poseStack, null, width, height, depth, buffers);
        }
    }

    public static void drawBoundingBox(PoseStack poseStack, float x0, float y0, float z0, float x1, float y1, float z1, UIColor color, MultiBufferSource buffers) {
        VertexConsumer builder = buffers.getBuffer(SkinRenderType.lines());
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, builder);
    }

    public static void drawBoundingBox(PoseStack poseStack, CGRect rect, UIColor color) {
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        drawBoundingBox(poseStack, rect.x, rect.y, 0, rect.x + rect.width, rect.y + rect.height, 0, color, buffers);
        buffers.endBatch();
    }

//    public static void drawAllEdges(PoseStack matrix, VoxelShape shape, UIColor color) {
//        MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
//        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
//        Matrix4f mat = matrix.last().pose();
//        shape.forAllEdges((x0, y0, z0, x1, y1, z1) -> {
//            builder.vertex(mat, (float) x0, (float) y0, (float) z0).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
//            builder.vertex(mat, (float) x1, (float) y1, (float) z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
//        });
//    }

    public static void drawBoundingBox(PoseStack poseStack, Rectangle3f rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        float x0 = rec.getMinX();
        float y0 = rec.getMinY();
        float z0 = rec.getMinZ();
        float x1 = rec.getMaxX();
        float y1 = rec.getMaxY();
        float z1 = rec.getMaxZ();
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

    public static void drawBoundingBox(PoseStack poseStack, Rectangle3i rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        int x0 = rec.getMinX();
        int y0 = rec.getMinY();
        int z0 = rec.getMinZ();
        int x1 = rec.getMaxX();
        int y1 = rec.getMaxY();
        int z1 = rec.getMaxZ();
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

    public static void drawBoundingBox(PoseStack poseStack, AABB rec, UIColor color, MultiBufferSource renderTypeBuffer) {
        float x0 = (float) rec.minX;
        float y0 = (float) rec.minY;
        float z0 = (float) rec.minZ;
        float x1 = (float) rec.maxX;
        float y1 = (float) rec.maxY;
        float z1 = (float) rec.maxZ;
        drawBoundingBox(poseStack, x0, y0, z0, x1, y1, z1, color, renderTypeBuffer);
    }

//    public static void drawShape(PoseStack poseStack, OpenVoxelShape shape, UIColor color, MultiBufferSource buffers) {
//        VertexConsumer builder = buffers.getBuffer(SkinRenderType.lines());
//        PoseStack.Pose pose = poseStack.last();
////        Vector4f pt1 = null;
////        for (Vector4f pt2 : shape) {
////            if (pt1 != null) {
////                drawLine(pose, pt1.x(), pt1.y(), pt1.z(), pt2.x(), pt2.y(), pt2.z(), color, builder);
////            }
////            pt1 = pt2;
////        }
//    }

    public static void drawCube(PoseStack poseStack, IRectangle3i rect, float r, float g, float b, float a, MultiBufferSource buffers) {
        float x = rect.getMinX();
        float y = rect.getMinY();
        float z = rect.getMinZ();
        float w = rect.getWidth();
        float h = rect.getHeight();
        float d = rect.getDepth();
        drawCube(poseStack, x, y, z, w, h, d, r, g, b, a, buffers);
    }

    public static void drawCube(PoseStack poseStack, IRectangle3f rect, float r, float g, float b, float a, MultiBufferSource buffers) {
        float x = rect.getMinX();
        float y = rect.getMinY();
        float z = rect.getMinZ();
        float w = rect.getWidth();
        float h = rect.getHeight();
        float d = rect.getDepth();
        drawCube(poseStack, x, y, z, w, h, d, r, g, b, a, buffers);
    }

    public static void drawCube(PoseStack poseStack, float x, float y, float z, float w, float h, float d, float r, float g, float b, float a, MultiBufferSource buffers) {
        if (w == 0 || h == 0 || d == 0) {
            return;
        }
        PoseStack.Pose pose = poseStack.last();
        SkinVertexBufferBuilder builder1 = SkinVertexBufferBuilder.getBuffer(buffers);
        VertexConsumer builder = builder1.getBuffer(SkinRenderType.IMAGE_GUIDE);
        for (Direction dir : Direction.values()) {
            drawFace(pose, dir, x, y, z, w, h, d, 0, 0, r, g, b, a, builder);
        }
    }

    public static void drawFace(PoseStack.Pose pose, Direction dir, float x, float y, float z, float w, float h, float d, float u, float v, float r, float g, float b, float a, VertexConsumer builder) {
        byte[][] vertexes = FACE_MARK_VERTEXES[dir.get3DDataValue()];
        byte[][] textures = FACE_MARK_TEXTURES[dir.get3DDataValue()];
        float[] values = {0, w, h, d};
        for (int i = 0; i < 4; ++i) {
            builder.vertex(pose.pose(), x + vertexes[i][0] * w, y + vertexes[i][1] * h, z + vertexes[i][2] * d)
                    .color(r, g, b, a)
                    .uv(u + values[textures[i][0]], v + values[textures[i][1]])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xf000f0)
                    .endVertex();
        }
    }

    public static void drawImage(ResourceLocation texture, int x, int y, int u, int v, int width, int height, int sourceWidth, int sourceHeight, int texWidth, int texHeight, PoseStack poseStack) {
        setShaderTexture(0, texture);
        RectangleTesselator tesselator = new RectangleTesselator(poseStack);
        tesselator.begin(SkinRenderType.GUI_IMAGE, texWidth, texHeight);
        tesselator.add(x, y, width, height, u, v, sourceWidth, sourceHeight, 0);
        tesselator.end();
    }

    public static void drawClipImage(ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, int topBorder, int bottomBorder, int leftBorder, int rightBorder, float zLevel, PoseStack poseStack) {
        setShaderTexture(0, res);
        setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        enableBlend();
        defaultBlendFunc();

        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;

        RectangleTesselator tesselator = new RectangleTesselator(poseStack);
        tesselator.begin(SkinRenderType.GUI_IMAGE, 256, 256);

        // Draw Border
        // Top Left
        tesselator.add(
                x, y,
                leftBorder, topBorder,
                u, v,
                zLevel);
        // Top Right
        tesselator.add(x + leftBorder + canvasWidth, y,
                rightBorder, topBorder,
                u + leftBorder + fillerWidth, v,
                zLevel);
        // Bottom Left
        tesselator.add(
                x, y + topBorder + canvasHeight,
                leftBorder, bottomBorder,
                u, v + topBorder + fillerHeight,
                zLevel);
        // Bottom Right
        tesselator.add(x + leftBorder + canvasWidth, y + topBorder + canvasHeight,
                rightBorder, bottomBorder,
                u + leftBorder + fillerWidth, v + topBorder + fillerHeight,
                zLevel);

        for (int i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); i++) {
            // Top Border
            tesselator.add(
                    x + leftBorder + (i * fillerWidth), y,
                    (i == xPasses ? remainderWidth : fillerWidth), topBorder,
                    u + leftBorder, v,
                    zLevel);
            // Bottom Border
            tesselator.add(
                    x + leftBorder + (i * fillerWidth), y + topBorder + canvasHeight,
                    (i == xPasses ? remainderWidth : fillerWidth), bottomBorder,
                    u + leftBorder, v + topBorder + fillerHeight,
                    zLevel);


            // Throw in some filler for good measure
            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++)
                tesselator.add(
                        x + leftBorder + (i * fillerWidth), y + topBorder + (j * fillerHeight),
                        (i == xPasses ? remainderWidth : fillerWidth), (j == yPasses ? remainderHeight : fillerHeight),
                        u + leftBorder, v + topBorder,
                        zLevel);
        }

        // Side Borders
        for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); j++) {
            // Left Border
            tesselator.add(
                    x, y + topBorder + (j * fillerHeight),
                    leftBorder, (j == yPasses ? remainderHeight : fillerHeight),
                    u, v + topBorder,
                    zLevel);
            // Right Border
            tesselator.add(
                    x + leftBorder + canvasWidth, y + topBorder + (j * fillerHeight),
                    rightBorder, (j == yPasses ? remainderHeight : fillerHeight),
                    u + leftBorder + fillerWidth, v + topBorder,
                    zLevel);
        }

        tesselator.end();
    }

    public static void setShaderColor(UIColor color) {
        setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public static void setShaderColor(float f, float g, float h) {
        setShaderColor(f, g, h, 1.0f);
    }

    public static OpenMatrix3f getExtendedNormalMatrix() {
        return extendedNormalMatrix.get();
    }

    public static void setExtendedNormalMatrix(OpenMatrix3f value) {
        extendedNormalMatrix.set(value);
    }

    public static OpenMatrix4f getExtendedTextureMatrix() {
        return extendedTextureMatrix.get();
    }

    public static void setExtendedTextureMatrix(OpenMatrix4f value) {
        extendedTextureMatrix.set(value);
    }

    public static OpenMatrix4f getExtendedLightmapTextureMatrix() {
        return extendedLightmapTextureMatrix.get();
    }

    public static void setExtendedLightmapTextureMatrix(OpenMatrix4f value) {
        extendedLightmapTextureMatrix.set(value);
    }

    public static OpenMatrix4f getExtendedModelViewMatrix() {
        return extendedModelViewMatrix.get();
    }

    public static void setExtendedModelViewMatrix(OpenMatrix4f value) {
        extendedModelViewMatrix.set(value);
    }

    public static void setExtendedMatrixFlags(int options) {
        extendedMatrixFlags.set(options);
    }

    public static int getExtendedMatrixFlags() {
        return extendedMatrixFlags.get();
    }

    public static void backupExtendedMatrix() {
        extendedTextureMatrix.save();
        extendedNormalMatrix.save();
        extendedLightmapTextureMatrix.save();
        extendedModelViewMatrix.save();
    }

    public static void restoreExtendedMatrix() {
        extendedTextureMatrix.load();
        extendedNormalMatrix.load();
        extendedLightmapTextureMatrix.load();
        extendedModelViewMatrix.load();
    }

    public static class Storage<T> {

        private T value;
        private T backup;

        public Storage(T value) {
            this.value = value;
            this.backup = value;
        }

        public void save() {
            backup = value;
        }

        public void load() {
            value = backup;
        }

        public void set(T value) {
            if (!isOnRenderThread()) {
                recordRenderCall(() -> this.value = value);
            } else {
                this.value = value;
            }
        }

        public T get() {
            assertOnRenderThread();
            return value;
        }
    }
}
