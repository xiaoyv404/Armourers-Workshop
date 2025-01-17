package moe.plushie.armourers_workshop.core.client.other;

import com.apple.library.uikit.UIColor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.vertex.VertexFormat;
import moe.plushie.armourers_workshop.api.armature.IJoint;
import moe.plushie.armourers_workshop.api.armature.IJointTransform;
import moe.plushie.armourers_workshop.api.client.IRenderedBuffer;
import moe.plushie.armourers_workshop.api.skin.ISkinPartType;
import moe.plushie.armourers_workshop.compatibility.client.AbstractBufferBuilder;
import moe.plushie.armourers_workshop.compatibility.client.AbstractBufferSource;
import moe.plushie.armourers_workshop.compatibility.client.AbstractPoseStack;
import moe.plushie.armourers_workshop.core.armature.Armature;
import moe.plushie.armourers_workshop.core.armature.JointShape;
import moe.plushie.armourers_workshop.core.client.bake.BakedArmature;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkin;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkinPart;
import moe.plushie.armourers_workshop.core.client.shader.ShaderVertexObject;
import moe.plushie.armourers_workshop.core.data.cache.SkinCache;
import moe.plushie.armourers_workshop.core.data.color.ColorScheme;
import moe.plushie.armourers_workshop.init.ModDebugger;
import moe.plushie.armourers_workshop.utils.ColorUtils;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import moe.plushie.armourers_workshop.utils.ShapeTesselator;
import moe.plushie.armourers_workshop.utils.ThreadUtils;
import moe.plushie.armourers_workshop.utils.math.OpenPoseStack;
import moe.plushie.armourers_workshop.utils.math.OpenVoxelShape;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import manifold.ext.rt.api.auto;

@Environment(EnvType.CLIENT)
public class SkinRenderObjectBuilder implements SkinRenderBufferSource.ObjectBuilder {

    private static final ExecutorService workThread = ThreadUtils.newFixedThreadPool(1, "AW-SKIN-VB");
    private static final Cache<Object, CachedTask> cachingTasks = CacheBuilder.newBuilder()
            .expireAfterAccess(3, TimeUnit.SECONDS)
            .removalListener(CachedTask::release)
            .build();

    protected final BakedSkin skin;
    protected final CachedRenderPipeline cachedRenderPipeline = new CachedRenderPipeline();

    protected final ArrayList<CachedTask> pendingCacheTasks = new ArrayList<>();
    protected boolean isSent = false;

    public SkinRenderObjectBuilder(BakedSkin skin) {
        this.skin = skin;
    }

    public static void clearAllCache() {
        cachingTasks.invalidateAll();
        cachingTasks.cleanUp();
    }

    @Override
    public int addPart(BakedSkinPart part, BakedSkin bakedSkin, ColorScheme scheme, boolean shouldRender, SkinRenderContext context) {
        // debug the vbo render.
        if (ModDebugger.vertexBufferObject) {
            return draw(part, bakedSkin, scheme, context.getOverlay(), context);
        }
        CachedTask cachedTask = compile(part, bakedSkin, scheme, context.getOverlay());
        if (cachedTask != null) {
            // we need compile the skin part, but does not render now.
            if (!shouldRender) {
                return 0;
            }
            return cachedRenderPipeline.draw(cachedTask, context);
        }
        return 0;
    }

    @Override
    public void addShape(Vector3f origin, SkinRenderContext context) {
        auto buffers = AbstractBufferSource.defaultBufferSource();
//        RenderUtils.drawBoundingBox(poseStack, box, color, SkinRenderBuffer.getInstance());
        ShapeTesselator.vector(origin, 16, context.pose(), buffers);
    }

    @Override
    public void addShape(OpenVoxelShape shape, UIColor color, SkinRenderContext context) {
        auto buffers = AbstractBufferSource.defaultBufferSource();
        ShapeTesselator.stroke(shape.bounds(), color, context.pose(), buffers);
    }

    @Override
    public void addShape(BakedArmature armature, SkinRenderContext context) {
        auto buffers = AbstractBufferSource.defaultBufferSource();
        auto transforms = armature.getTransforms();
        auto armature1 = armature.getArmature();
        for (auto joint : armature1.allJoints()) {
            auto shape = armature1.getShape(joint.getId());
            auto transform = transforms[joint.getId()];
            if (ModDebugger.defaultArmature) {
                transform = armature1.getGlobalTransform(joint.getId());
            }
            if (shape != null && transform != null) {
                context.pushPose();
                transform.apply(context.pose());
//                ModDebugger.translate(context.pose().pose());
//			poseStack.translate(box.o.getX(), box.o.getY(), box.o.getZ());
                ShapeTesselator.stroke(shape, ColorUtils.getPaletteColor(joint.getId()), context.pose(), buffers);
                ShapeTesselator.vector(0, 0, 0, 4, 4, 4, context.pose(), buffers);
                context.popPose();
            }
        }
    }

    public void endBatch(SkinVertexBufferBuilder.Pipeline pipeline) {
        cachedRenderPipeline.commit(pipeline::add);
    }

    @Nullable
    public CachedTask compile(BakedSkinPart part, BakedSkin bakedSkin, ColorScheme scheme, int overlay) {
        auto key = SkinCache.borrowKey(bakedSkin.getId(), part.getId(), part.requirements(scheme), overlay);
        auto cachedTask = cachingTasks.getIfPresent(key);
        if (cachedTask != null) {
            SkinCache.returnKey(key);
            if (cachedTask.isCompiled) {
                return cachedTask;
            }
            return null; // wait compile

        }
        cachedTask = new CachedTask(part, scheme, overlay);
        cachingTasks.put(key, cachedTask);
        addCompileTask(cachedTask);
        return null; // wait compile
    }

    private synchronized void addCompileTask(CachedTask cachedTask) {
        pendingCacheTasks.add(cachedTask);
        if (isSent) {
            return;
        }
        isSent = true;
        workThread.execute(this::doCompile);
    }

    private void doCompile() {
        ArrayList<CachedTask> tasks;
        synchronized (this) {
            tasks = new ArrayList<>(pendingCacheTasks);
            pendingCacheTasks.clear();
            isSent = false;
        }
        if (tasks.isEmpty()) {
            return;
        }
//        long startTime = System.currentTimeMillis();
        OpenPoseStack poseStack1 = new OpenPoseStack();
        ArrayList<CompiledTask> buildingTasks = new ArrayList<>();
        for (CachedTask task : tasks) {
            int overlay = task.overlay;
            BakedSkinPart part = task.part;
            ColorScheme scheme = task.scheme;
            ArrayList<CompiledTask> mergedTasks = new ArrayList<>();
            part.forEach((renderType, quads) -> {
                auto builder = new AbstractBufferBuilder(quads.size() * 8 * renderType.format().getVertexSize());
                builder.begin(renderType);
                quads.forEach(quad -> quad.render(part, scheme, 0xf000f0, overlay, poseStack1, builder));
                IRenderedBuffer renderedBuffer = builder.end();
                CompiledTask compiledTask = new CompiledTask(renderType, renderedBuffer, part.getRenderPolygonOffset(), part.getType());
                mergedTasks.add(compiledTask);
                buildingTasks.add(compiledTask);
            });
            task.mergedTasks = mergedTasks;
        }
        combineAndUpload(tasks, buildingTasks);
//        long totalTime = System.currentTimeMillis() - startTime;
//        ModLog.debug("compile tasks {}, times: {}ms", tasks.size(), totalTime);
    }

    private int draw(BakedSkinPart part, BakedSkin bakedSkin, ColorScheme scheme, int overlay, SkinRenderContext context) {
        auto buffers = context.getBuffers();
        auto poseStack1 = context.pose();
//        CachedTask task = new CachedTask(part, scheme, overlay);
//        ArrayList<CompiledTask> mergedTasks = new ArrayList<>();
        part.forEach((renderType, quads) -> {
//            auto builder = BufferBuilder.createBuilderBuffer(quads.size() * 8 * renderType.format().getVertexSize());
//            builder.begin(renderType);
            quads.forEach(quad -> quad.render(part, scheme, 0xf000f0, overlay, poseStack1, buffers.getBuffer(renderType)));
//            IRenderedBuffer renderedBuffer = builder.end();
//            CompiledTask compiledTask = new CompiledTask(renderType, renderedBuffer, part.getRenderPolygonOffset(), part.getType());
//            mergedTasks.add(compiledTask);
//            buildingTasks.add(compiledTask);
        });
//        task.mergedTasks = mergedTasks;
        return 1;
    }

    private void combineAndUpload(ArrayList<CachedTask> qt, ArrayList<CompiledTask> buildingTasks) {
        int totalRenderedBytes = 0;
        SkinRenderObject vertexBuffer = new SkinRenderObject();
        ArrayList<ByteBuffer> byteBuffers = new ArrayList<>();

        for (CompiledTask compiledTask : buildingTasks) {
            auto drawState = compiledTask.bufferBuilder.drawState();
            auto format = drawState.format();
            auto byteBuffer = compiledTask.bufferBuilder.vertexBuffer();
            compiledTask.vertexBuffer = vertexBuffer;
            compiledTask.vertexCount = drawState.vertexCount();
            compiledTask.vertexOffset = totalRenderedBytes;
            compiledTask.bufferBuilder.release();
            compiledTask.bufferBuilder = null;
            compiledTask.format = format;
            byteBuffers.add(byteBuffer);
            totalRenderedBytes += byteBuffer.remaining();
        }

        ByteBuffer mergedByteBuffer = ByteBuffer.allocateDirect(totalRenderedBytes);
        for (ByteBuffer byteBuffer : byteBuffers) {
            mergedByteBuffer.put(byteBuffer);
        }
        mergedByteBuffer.rewind();
        vertexBuffer.upload(mergedByteBuffer);
        RenderSystem.recordRenderCall(() -> {
            for (CachedTask cachedTask : qt) {
                cachedTask.setRenderObject(vertexBuffer);
                cachedTask.finish();
            }
            vertexBuffer.release();
        });
    }

    static class CachedTask {

        int totalTask;
        boolean isCompiled = false;
        ArrayList<CompiledTask> mergedTasks;
        BakedSkinPart part;
        ColorScheme scheme;
        int overlay;
        SkinRenderObject renderObject;

        CachedTask(BakedSkinPart part, ColorScheme scheme, int overlay) {
            this.part = part;
            this.scheme = scheme.copy();
            this.overlay = overlay;
        }

        static void release(RemovalNotification<Object, Object> notification) {
            CachedTask task = ObjectUtils.safeCast(notification.getValue(), CachedTask.class);
            if (task != null) {
                task.setRenderObject(null);
            }
        }

        void setRenderObject(SkinRenderObject renderObject) {
            if (this.renderObject != null) {
                this.renderObject.release();
            }
            this.renderObject = renderObject;
            if (this.renderObject != null) {
                this.renderObject.retain();
            }
        }


        void finish() {
            isCompiled = true;
            totalTask = mergedTasks.size();
        }
    }

    static class CompiledTask {

        final float polygonOffset;
        final ISkinPartType partType;
        final RenderType renderType;
        int vertexCount;
        int vertexOffset;
        IRenderedBuffer bufferBuilder;
        SkinRenderObject vertexBuffer;
        VertexFormat format;

        CompiledTask(RenderType renderType, IRenderedBuffer bufferBuilder, float polygonOffset, ISkinPartType partType) {
            this.partType = partType;
            this.renderType = renderType;
            this.bufferBuilder = bufferBuilder;
            this.polygonOffset = polygonOffset;
        }
    }

    static class CachedRenderPipeline {

        protected final ArrayList<CompiledPass> tasks = new ArrayList<>();

        int draw(CachedTask task, SkinRenderContext context) {
            int lightmap = context.getLightmap();
            float animationTicks = context.getAnimationTicks();
            float renderPriority = context.getReferenced().getRenderPriority();
            auto poseStack = context.pose();
            auto modelViewStack = AbstractPoseStack.wrap(RenderSystem.getModelViewStack());
            auto finalPostStack = new OpenPoseStack();
            auto lastPose = finalPostStack.last().pose();
            auto lastNormal = finalPostStack.last().normal();
            // https://web.archive.org/web/20240125142900/http://www.songho.ca/opengl/gl_normaltransform.html
            //finalPostStack.last().setProperties(poseStack.last().properties());
            lastPose.multiply(modelViewStack.last().pose());
            lastPose.multiply(poseStack.last().pose());
            //lastNormal.multiply(modelViewStack.last().normal());
            lastNormal.multiply(poseStack.last().normal());
            lastNormal.invert();
            task.mergedTasks.forEach(t -> tasks.add(new CompiledPass(t, finalPostStack, lightmap, animationTicks, renderPriority)));
            return task.totalTask;
        }

        void commit(Consumer<ShaderVertexObject> consumer) {
            if (tasks.size() != 0) {
                tasks.forEach(consumer);
                tasks.clear();
            }
        }
    }

    static class CompiledPass extends ShaderVertexObject {

        int lightmap;
        float animationTicks;

        float additionalPolygonOffset;
        boolean isGrowing;

        OpenPoseStack poseStack;
        CompiledTask compiledTask;

        CompiledPass(CompiledTask compiledTask, OpenPoseStack poseStack, int lightmap, float animationTicks, float renderPriority) {
            super();
            this.compiledTask = compiledTask;
            this.poseStack = poseStack;
            this.lightmap = lightmap;
            this.animationTicks = animationTicks;
            this.isGrowing = SkinRenderType.isGrowing(compiledTask.renderType);
            this.additionalPolygonOffset = renderPriority;
        }

        @Override
        public RenderType getType() {
            return compiledTask.renderType;
        }

        @Override
        public int getVertexOffset() {
            return compiledTask.vertexOffset;
        }

        @Override
        public int getVertexCount() {
            return compiledTask.vertexCount;
        }

        @Override
        public SkinRenderObject getVertexBuffer() {
            return compiledTask.vertexBuffer;
        }

        @Override
        public float getPolygonOffset() {
            return compiledTask.polygonOffset + additionalPolygonOffset;
        }

        @Override
        public OpenPoseStack getPoseStack() {
            return poseStack;
        }

        @Override
        public VertexFormat getFormat() {
            if (compiledTask.format == null) {
                return compiledTask.renderType.format();
            }
            return compiledTask.format;
        }

        @Override
        public int getLightmap() {
            return lightmap;
        }

        @Override
        public boolean isGrowing() {
            return isGrowing;
        }
    }
}
