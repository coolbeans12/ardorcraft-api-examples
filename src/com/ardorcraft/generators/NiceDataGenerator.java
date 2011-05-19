
package com.ardorcraft.generators;

import java.util.List;
import java.util.Random;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.data.Pos;
import com.ardorcraft.util.ImprovedNoise;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.BlockWorld.BlockType;
import com.ardorcraft.world.WorldModifier;
import com.google.common.collect.Lists;

public class NiceDataGenerator implements DataGenerator {
    private final int waterHeight = 15;
    private final List<Pos> treePositions = Lists.newArrayList();
    private final Random rand = new Random();

    @Override
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, final int height,
            final WorldModifier blockScene) {
        rand.setSeed(xStart * 10000 + zStart);
        treePositions.clear();

        for (int x = xStart; x < xEnd; x++) {
            for (int z = zStart; z < zEnd; z++) {
                generateColumn(x, z, height, blockScene, xStart, zStart, xEnd, zEnd);
            }
        }

        for (final Pos pos : treePositions) {
            if (blockScene.getBlock(pos.x - 1, pos.y + 2, pos.z, BlockType.All) == 0
                    && blockScene.getBlock(pos.x + 2, pos.y + 2, pos.z, BlockType.All) == 0
                    && blockScene.getBlock(pos.x, pos.y + 2, pos.z - 1, BlockType.All) == 0
                    && blockScene.getBlock(pos.x, pos.y + 2, pos.z + 1, BlockType.All) == 0) {
                addTree(blockScene, pos);
            }
        }
    }

    private void addTree(final WorldModifier blockScene, final Pos pos) {
        final int treeHeight = rand.nextInt(4) + 2;
        for (int y = 0; y < treeHeight; y++) {
            blockScene.setBlock(pos.x, pos.y + y, pos.z, 17);
        }

        final int maxWidth = 3;
        final int leavesHeight = treeHeight / 2 + 2;
        for (int x = 0; x < maxWidth; x++) {
            for (int z = 0; z < maxWidth; z++) {
                for (int y = 0; y < leavesHeight; y++) {
                    final int xx = x - maxWidth / 2;
                    final int yy = y - leavesHeight / 2;
                    final int zz = z - maxWidth / 2;
                    if (xx == 0 && zz == 0) {
                        continue;
                    }
                    final double test = (Math.abs(xx) + Math.abs(yy) + Math.abs(zz)) * 2.0
                            / (maxWidth * 2.5 + leavesHeight);
                    if (rand.nextDouble() > test) {
                        blockScene.setBlock(pos.x + xx, pos.y + yy + treeHeight, pos.z + zz, 18);
                    }
                }
            }
        }
    }

    private void generateColumn(final int x, final int z, final int height, final WorldModifier blockScene,
            final int xStart, final int zStart, final int xEnd, final int zEnd) {
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
                checkAddTree(x, z, xStart, zStart, xEnd, zEnd, localHeight);
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

    private void checkAddTree(final int x, final int z, final int xStart, final int zStart, final int xEnd,
            final int zEnd, final int localHeight) {
        if (x > xStart && x < xEnd - 1 && z > zStart && z < zEnd - 1 && x % 3 == 0 && (z + 1) % 3 == 0) {
            final double noiseTree = ImprovedNoise.noise(x * 0.01, 0, z * 0.01) + 0.0;
            final double r = rand.nextDouble();
            if (noiseTree > r) {
                treePositions.add(new Pos(x, localHeight, z));
            }
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
