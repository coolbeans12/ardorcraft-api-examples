
package com.ardorcraft.generators;

import com.ardorcraft.world.WorldModifier;

public abstract class BasicDataGenerator implements DataGenerator {
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, int spacing,
            final int height, final WorldModifier proxy) {
        for (int x = xStart; x < xEnd; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = zStart; z < zEnd; z++) {
                    generateBlock(x, y, z, height, proxy);
                }
            }
        }
    }

    public abstract void generateBlock(int x, int y, int z, int height, WorldModifier world);
}
