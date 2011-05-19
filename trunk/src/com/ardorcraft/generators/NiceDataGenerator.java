
package com.ardorcraft.generators;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.util.ImprovedNoise;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.BlockWorld.BlockType;
import com.ardorcraft.world.WorldModifier;

public class NiceDataGenerator implements DataGenerator {
    private final int waterHeight = 15;

    @Override
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, final int height,
            final WorldModifier blockScene) {
        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                generateColumn(x, z, height, blockScene);
            }
        }
    }

    private void generateColumn(final int x, final int z, final int height, final WorldModifier blockScene) {
        final double gen = 5;

        int localHeight = 0;

        localHeight = generateLayer(x, z, gen + 0, 0.4, 0, 0.1f * height, 7, 0.75f, height, blockScene);
        localHeight = generateLayer(x, z, gen + 0.2, 1.5, localHeight, 0.08f * height + 2.0f * (localHeight - 10), 1,
                0.8f, height, blockScene);
        localHeight = generateLayer(x, z, gen + 0.5, 2.0, localHeight, 0.06f * height + 0.3f * (localHeight - 5), 3,
                0.6f, height, blockScene);

        // mountain
        final double noise1 = ImprovedNoise.noise(x * 0.01, 20, z * 0.01) + 0.5;
        final double noise3 = ImprovedNoise.noise(x * 0.05, 20, z * 0.05) + 0.5;
        final double noise2 = ImprovedNoise.noise(x * 0.05, 100, z * 0.05);
        double mul = (localHeight + height / 2.0) / height;
        mul = 10.0 * MathUtils.clamp(mul, 0.0, 1.0);
        int val = (int) (mul * noise1 * noise3 * (noise2 > 0.2 ? 1.0 : 0.0));
        val = Math.max(0, val);
        int type = 1;
        for (int y = localHeight; y < localHeight + val; y++) {
            if (y <= waterHeight + 1) {
                type = 12;
            }
            final double scaleY = (Math.abs(y - height / 5) + 10.0) / height * 3.5;
            final double scale = 0.05;
            final double noise4 = ImprovedNoise.noise(x * scale, y * scale * 2.5, z * scale);
            if (noise4 < scaleY) {
                blockScene.setBlock(x, y, z, type);
            } else {
                blockScene.setBlock(x, y, z, 0);
            }
        }
        localHeight += val;

        // sediment
        final int block = blockScene.getBlock(x, localHeight - 1, z, BlockType.Solid);
        if (block == 3) {
            if (localHeight - 1 <= waterHeight + 1) {
                blockScene.setBlock(x, localHeight - 1, z, 12);
            } else {
                blockScene.setBlock(x, localHeight - 1, z, 2);
            }

            if (noise2 < -0.4) {
                final double noiseTree = ImprovedNoise.noise(x * 0.2, localHeight * 0.2, z * 0.2);
                if (noiseTree > 0.4) {
                    final int mountainHeight = (int) ((noiseTree - 0.4) * 10);
                    for (int y = localHeight; y < localHeight + mountainHeight; y++) {
                        blockScene.setBlock(x, y, z, 1);
                    }
                    localHeight += mountainHeight;
                }
            }
        }

        if (localHeight < waterHeight) {
            for (; localHeight < waterHeight; localHeight++) {
                blockScene.setBlock(x, localHeight, z, BlockWorld.WATER);
            }
        }

        for (int y = localHeight; y < height; y++) {
            blockScene.setBlock(x, y, z, 0);
        }
    }

    private int generateLayer(final int x, final int z, final double noiseVal, final double noiseScale,
            final int startheight, float layerheight, final int type, final float adder, final int height,
            final WorldModifier blockScene) {
        layerheight = Math.max(0.0f, layerheight);

        double noise = ImprovedNoise.noise(x * 0.01 * noiseScale, noiseVal, z * 0.01 * noiseScale) + adder;
        double noise2 = ImprovedNoise.noise(x * 0.05 * noiseScale, noiseVal, z * 0.05 * noiseScale) + adder;

        double phatnoise = ImprovedNoise.noise(x * 0.004, noiseVal, z * 0.004);
        phatnoise = MathUtils.clamp(Math.abs(phatnoise) + 0.6, 0.0, 1.0);
        noise2 *= phatnoise;
        noise *= phatnoise;

        int localHeight = (int) (noise * layerheight + noise2 * layerheight * 0.35);
        localHeight = Math.max(0, localHeight);

        for (int y = startheight; y < startheight + localHeight; y++) {
            if (y <= 1) {
                blockScene.setBlock(x, y, z, 12);
            }
            final double scaleY = (Math.abs(y - height / 3) + 15.0) / height * 1.5;
            final double scale = 0.05;
            final double noise3 = ImprovedNoise.noise(x * scale, y * scale * 2.0, z * scale);
            if (noise3 < scaleY) {
                if (type == 13 && localHeight == 1 && y > waterHeight) {
                    blockScene.setBlock(x, y, z, 12);
                } else {
                    blockScene.setBlock(x, y, z, type);
                }
            } else {
                if (y < waterHeight) {
                    blockScene.setBlock(x, y, z, BlockWorld.WATER);
                } else {
                    blockScene.setBlock(x, y, z, 0);
                }
            }
        }

        return startheight + localHeight;
    }
}
