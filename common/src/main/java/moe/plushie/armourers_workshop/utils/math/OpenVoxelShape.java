package moe.plushie.armourers_workshop.utils.math;

import com.google.common.collect.Lists;
import moe.plushie.armourers_workshop.api.math.IMatrix4f;
import moe.plushie.armourers_workshop.api.math.IRectangle3f;
import moe.plushie.armourers_workshop.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("unused")
public class OpenVoxelShape implements Iterable<Vector4f> {

    private OpenBoundingBox aabb;
    private Rectangle3f box;
    private ArrayList<Vector4f> vertexes;

    public OpenVoxelShape() {
    }

    public static OpenVoxelShape empty() {
        return new OpenVoxelShape();
    }


    public static OpenVoxelShape box(IRectangle3f bounds) {
        Rectangle3f rect = ObjectUtils.safeCast(bounds, Rectangle3f.class);
        if (rect != null) {
            return box(rect);
        }
        return box(new Rectangle3f(bounds));
    }

    public static OpenVoxelShape box(Rectangle3f bounds) {
        OpenVoxelShape shape = new OpenVoxelShape();
        shape.box = bounds;
        return shape;
    }

    public OpenBoundingBox aabb() {
        if (aabb != null) {
            return aabb;
        }
        aabb = new OpenBoundingBox(bounds());
        return aabb;
    }

    public Rectangle3f bounds() {
        if (box != null) {
            return box;
        }
        if (vertexes == null || vertexes.size() == 0) {
            return Rectangle3f.ZERO;
        }
        Iterator<Vector4f> iterator = vertexes.iterator();
        Vector4f fp = iterator.next();
        float minX = fp.x(), minY = fp.y(), minZ = fp.z();
        float maxX = fp.x(), maxY = fp.y(), maxZ = fp.z();
        while (iterator.hasNext()) {
            Vector4f point = iterator.next();
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
            maxZ = Math.max(maxZ, point.z());
        }
        box = new Rectangle3f(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ);
        return box;
    }

    public void mul(IMatrix4f matrix) {
        for (Vector4f vector : getVertexes()) {
            vector.transform(matrix);
        }
        box = null;
        aabb = null;
    }

    public void add(float x, float y, float z, float width, float height, float depth) {
        List<Vector4f> list = getVertexes();
        list.add(new Vector4f(x, y, z, 1.0f));
        list.add(new Vector4f(x + width, y, z, 1.0f));
        list.add(new Vector4f(x + width, y + height, z, 1.0f));
        list.add(new Vector4f(x, y + height, z, 1.0f));
        list.add(new Vector4f(x, y, z + depth, 1.0f));
        list.add(new Vector4f(x + width, y, z + depth, 1.0f));
        list.add(new Vector4f(x + width, y + height, z + depth, 1.0f));
        list.add(new Vector4f(x, y + height, z + depth, 1.0f));
        box = null;
        aabb = null;
    }

    public void add(OpenVoxelShape shape1) {
        List<Vector4f> list = getVertexes();
        list.addAll(shape1.getVertexes());
        box = null;
    }

    public void add(IRectangle3f rect) {
        add(rect.getX(), rect.getY(), rect.getZ(), rect.getWidth(), rect.getHeight(), rect.getDepth());
    }

    public void optimize() {
        if (vertexes == null || vertexes.size() <= 8) {
            return;
        }
        List<Vector4f> list = getVertexes();
        HashSet<Vector4f> addVertexes = new HashSet<>(list.size());
        HashSet<Vector4f> uniquesVertexes = new HashSet<>(list.size());
        // when vertex is used than 1, that means this a overlapping vertex.
        for (Vector4f vector : list) {
            if (addVertexes.contains(vector)) {
                uniquesVertexes.remove(vector);
            } else {
                addVertexes.add(vector);
                uniquesVertexes.add(vector);
            }
        }
        vertexes = Lists.newArrayList(uniquesVertexes);
    }

    public OpenVoxelShape copy() {
        OpenVoxelShape shape = new OpenVoxelShape();
        shape.box = box;
        shape.aabb = aabb;
        if (vertexes != null) {
            ArrayList<Vector4f> newVertexes = new ArrayList<>();
            newVertexes.ensureCapacity(vertexes.size());
            for (Vector4f vector : vertexes) {
                newVertexes.add(vector.copy());
            }
            shape.vertexes = newVertexes;
        }
        return shape;
    }

    public Iterator<Vector4f> iterator() {
        if (vertexes != null) {
            return vertexes.iterator();
        }
        return getVertexes(box).iterator();
    }

    private List<Vector4f> getVertexes() {
        if (vertexes == null) {
            vertexes = getVertexes(box);
        }
        return vertexes;
    }

    private ArrayList<Vector4f> getVertexes(IRectangle3f box) {
        if (box == null) {
            return Lists.newArrayList();
        }
        return Lists.newArrayList(
                new Vector4f(box.getMinX(), box.getMinY(), box.getMinZ(), 1.0f),
                new Vector4f(box.getMaxX(), box.getMinY(), box.getMinZ(), 1.0f),
                new Vector4f(box.getMaxX(), box.getMaxY(), box.getMinZ(), 1.0f),
                new Vector4f(box.getMinX(), box.getMaxY(), box.getMinZ(), 1.0f),
                new Vector4f(box.getMinX(), box.getMinY(), box.getMaxZ(), 1.0f),
                new Vector4f(box.getMaxX(), box.getMinY(), box.getMaxZ(), 1.0f),
                new Vector4f(box.getMaxX(), box.getMaxY(), box.getMaxZ(), 1.0f),
                new Vector4f(box.getMinX(), box.getMaxY(), box.getMaxZ(), 1.0f)
        );
    }
}
