
package com.ardorcraft.geometryproducers;

import java.nio.FloatBuffer;
import java.util.EnumMap;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.util.geometryproducers.GeometryProducer;
import com.ardorcraft.world.BlockProvider;
import com.ardorcraft.world.BlockWorld.BlockSide;
import com.ardorcraft.world.GeometryHandler;
import com.google.common.collect.Maps;

public final class MeshProducer implements GeometryProducer {

    private final EnumMap<BlockSide, MeshData> meshDatas = Maps.newEnumMap(BlockSide.class);
    private boolean transformCoords;

    public MeshProducer(final Mesh mesh) {
        final MeshData data = mesh.getMeshData();
        for (final BlockSide side : BlockSide.values()) {
            meshDatas.put(side, data);
        }

        if (data.getIndexMode(0) != IndexMode.Triangles) {
            throw new IllegalArgumentException("Only meshes with IndexMode.Triangles allowed");
        }
    }

    public void createOrientations() {
        MeshData copy = meshDatas.get(BlockSide.Bottom).makeCopy();

        copy.rotatePoints(new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        meshDatas.put(BlockSide.Front, copy);

        final Quaternion rotation = new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_Y);

        copy = copy.makeCopy();
        copy.rotatePoints(rotation);
        meshDatas.put(BlockSide.Left, copy);

        copy = copy.makeCopy();
        copy.rotatePoints(rotation);
        meshDatas.put(BlockSide.Back, copy);

        copy = copy.makeCopy();
        copy.rotatePoints(rotation);
        meshDatas.put(BlockSide.Right, copy);

        copy = copy.makeCopy();
        copy.rotatePoints(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_Z));
        meshDatas.put(BlockSide.Top, copy);
    }

    public void setTransformTextureCoords(final boolean value) {
        transformCoords = value;
    }

    @Override
    public void generateBlock(final int blockId, final GeometryHandler geometryHandler,
            final BlockProvider blockProvider, final BlockUtil blockUtil, final BlockSide orientation, final int x,
            final int y, final int z) {

        final MeshData data = meshDatas.get(orientation);

        if (geometryHandler.hasVertices()) {
            final FloatBuffer vertices = data.getVertexBuffer();
            for (int i = 0; i < data.getVertexCount(); i++) {
                geometryHandler.setVertex(i, vertices.get(i * 3 + 0) + x + 0.5f, vertices.get(i * 3 + 1) + y + 0.5f,
                        vertices.get(i * 3 + 2) + z + 0.5f);
            }
        }

        if (geometryHandler.hasTextureCoords()) {
            final FloatBuffer texcoords = data.getTextureBuffer(0);
            if (!transformCoords) {
                for (int i = 0; i < data.getVertexCount(); i++) {
                    geometryHandler.setTextureCoord(i, texcoords.get(i * 2 + 0), texcoords.get(i * 2 + 1));
                }
            } else {
                final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(blockId, BlockSide.Front);

                for (int i = 0; i < data.getVertexCount(); i++) {
                    geometryHandler.setTextureCoord(i,
                            coord.getXf() + texcoords.get(i * 2 + 0) * blockUtil.getTileWidth(), coord.getYf()
                                    + texcoords.get(i * 2 + 1) * blockUtil.getTileHeight());
                }
            }
        }

        if (geometryHandler.hasColors()) {
            final float lighting = geometryHandler.getLighting(x, y + 1, z);

            for (int i = 0; i < data.getVertexCount(); i++) {
                geometryHandler.setColor(i, lighting, lighting, lighting);
            }
        }

        int indexCount = 0;
        if (geometryHandler.hasIndices()) {
            final IndexBufferData<?> indices = data.getIndices();
            indexCount = indices.limit();
            for (int i = 0; i < indexCount; i++) {
                geometryHandler.setIndex(i, indices.get(i));
            }
        }

        geometryHandler.setVertexCount(data.getVertexCount());
        geometryHandler.setIndexCount(indexCount);
    }
}
