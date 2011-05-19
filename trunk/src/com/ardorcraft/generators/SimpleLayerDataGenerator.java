
package com.ardorcraft.generators;

import com.ardorcraft.util.ImprovedNoise;

public class SimpleLayerDataGenerator extends LayerDataGenerator {
    public SimpleLayerDataGenerator(final int waterHeight) {
        super(3, waterHeight);
    }

    @Override
    public int getLayerHeight(final int layer, final int x, final int z) {
        if (layer == 0) {
            return 5;
        } else if (layer == 1) {
            return (int) (Math.abs(ImprovedNoise.noise(x * 0.01, 0.5, z * 0.01)) * 40);
        } else if (layer == 2) {
            return (int) (Math.abs(ImprovedNoise.noise(x * 0.03, 1.0, z * 0.03)) * 15);
        }
        return 0;
    }

    @Override
    public int getLayerType(final int layer, final int x, final int z) {
        if (layer == 0) {
            return 1;
        } else if (layer == 1) {
            return 3;
        } else if (layer == 2) {
            return 2;
        }
        return 2;
    }

    @Override
    public boolean isCave(final int x, final int y, final int z) {
        if (y > 1) {
            final int testHeight = (int) (ImprovedNoise.noise(x * 0.05, 0.0, z * 0.05) * 5 + waterHeight);
            if (y > testHeight) {
                return ImprovedNoise.noise(x * 0.02, y * 0.02, z * 0.02) > Math.abs(y - testHeight) / 60.0;
            } else {
                return ImprovedNoise.noise(x * 0.02, y * 0.02, z * 0.02) > Math.abs(y - testHeight) / 40.0 + 0.1;
            }
        }
        return false;
    }

}
