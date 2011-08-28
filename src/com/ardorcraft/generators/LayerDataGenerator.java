
package com.ardorcraft.generators;

import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.WorldModifier;

public abstract class LayerDataGenerator implements DataGenerator {
    private final int nrLayers;
    protected int waterHeight;

    public LayerDataGenerator(final int nrLayers, final int waterHeight) {
        this.nrLayers = nrLayers;
        this.waterHeight = waterHeight;
    }

    @Override
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, int spacing,
            final int height, final WorldModifier blockScene) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                generateColumn(x, z, height, blockScene);
            }
        }
    }

    private void generateColumn(final int x, final int z, final int height, final WorldModifier blockScene) {
        int startHeight = 1;
        blockScene.setBlock(x, 0, z, 4);
        for (int i = 0; i < nrLayers; i++) {
            final int localHeight = Math.max(0, getLayerHeight(i, x, startHeight, z, blockScene));
            final int type = getLayerType(i, x, z, blockScene);

            for (int y = startHeight; y < startHeight + localHeight && y < height; y++) {
                if (!isCave(x, y, z, blockScene)) {
                    blockScene.setBlock(x, y, z, type);
                } else if (y < waterHeight) {
                    blockScene.setBlock(x, y, z, BlockWorld.WATER);
                } else {
                    blockScene.setBlock(x, y, z, 0);
                }
            }
            startHeight += localHeight;
        }
        for (int y = startHeight; y < height; y++) {
            if (y < waterHeight) {
                blockScene.setBlock(x, y, z, BlockWorld.WATER);
            } else {
                blockScene.setBlock(x, y, z, 0);
            }
        }
    }

    public abstract boolean isCave(int x, int y, int z, WorldModifier blockScene);

    public abstract int getLayerType(int layer, int x, int z, WorldModifier blockScene);

    public abstract int getLayerHeight(int layer, int x, int y, int z, WorldModifier blockScene);
}
