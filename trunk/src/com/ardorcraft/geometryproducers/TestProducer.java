
package com.ardorcraft.geometryproducers;

import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.util.geometryproducers.GeometryProducer;
import com.ardorcraft.world.BlockProvider;
import com.ardorcraft.world.BlockWorld.BlockSide;
import com.ardorcraft.world.GeometryHandler;

public final class TestProducer implements GeometryProducer {

    @Override
    public void generateBlock(final int blockId, final GeometryHandler geometryHandler, final BlockProvider provider,
            final BlockUtil blockUtil, final BlockSide orientation, final int x, final int y, final int z) {
        if (geometryHandler.hasVertices()) {
            geometryHandler.setVertex(0, x, y, z);
            geometryHandler.setVertex(1, x, y + 1, z);
            geometryHandler.setVertex(2, x + 1, y + 1, z + 1);
            geometryHandler.setVertex(3, x + 1, y, z + 1);
        }

        if (geometryHandler.hasTextureCoords()) {
            final ReadOnlyVector2 coord = blockUtil.getBlockTextureCoord(blockId, BlockSide.Front);

            geometryHandler.setTextureCoord(0, coord.getXf(), coord.getYf());
            geometryHandler.setTextureCoord(1, coord.getXf(), coord.getYf() + blockUtil.getTileHeight());
            geometryHandler.setTextureCoord(2, coord.getXf() + blockUtil.getTileWidth(),
                    coord.getYf() + blockUtil.getTileHeight());
            geometryHandler.setTextureCoord(3, coord.getXf() + blockUtil.getTileWidth(), coord.getYf());
        }

        if (geometryHandler.hasColors()) {
            float lighting;

            lighting = geometryHandler.getLighting(x, y, z);
            geometryHandler.setColor(0, lighting, lighting, lighting);

            lighting = geometryHandler.getLighting(x, y + 1, z);
            geometryHandler.setColor(1, lighting, lighting, lighting);

            lighting = geometryHandler.getLighting(x + 1, y + 1, z + 1);
            geometryHandler.setColor(2, lighting, lighting, lighting);

            lighting = geometryHandler.getLighting(x + 1, y, z + 1);
            geometryHandler.setColor(3, lighting, lighting, lighting);
        }

        if (geometryHandler.hasIndices()) {
            geometryHandler.setIndex(0, 0);
            geometryHandler.setIndex(1, 1);
            geometryHandler.setIndex(2, 3);
            geometryHandler.setIndex(3, 1);
            geometryHandler.setIndex(4, 2);
            geometryHandler.setIndex(5, 3);
        }

        geometryHandler.setVertexCount(4);
        geometryHandler.setIndexCount(6);
    }

}
