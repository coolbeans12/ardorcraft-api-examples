
package com.ardorcraft.generators;

import com.ardorcraft.util.ImprovedNoise;
import com.ardorcraft.world.WorldModifier;

public class SimpleDataGenerator extends BasicDataGenerator {

    @Override
    public void generateBlock(final int x, final int y, final int z, final int height, final WorldModifier world) {
        final double d = ImprovedNoise.noise(x * 0.05, y * 0.05, z * 0.05);
        if (d > -0.01) {
            if (y < height / 2) {
                world.setBlock(x, y, z, 2);
            } else if (y <= height / 2) {
                world.setBlock(x, y, z, 1);
            } else {
                world.setBlock(x, y, z, 0);
            }
        } else {
            world.setBlock(x, y, z, 0);
        }
    }
}
