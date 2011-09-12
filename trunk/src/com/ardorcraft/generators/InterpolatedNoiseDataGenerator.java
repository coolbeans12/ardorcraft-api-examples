
package com.ardorcraft.generators;

import java.util.List;
import java.util.Random;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.data.Pos;
import com.ardorcraft.util.ImprovedNoise;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.WorldModifier;
import com.google.common.collect.Lists;

/**
 * Some nice terrain with trees.
 */
public class InterpolatedNoiseDataGenerator implements DataGenerator {

    private final double scale = 8.0;

    @Override
    public void generateChunk(final int xStart, final int zStart, final int xEnd, final int zEnd, final int spacing,
            final int height, final WorldModifier blockScene) {

        final int waterHeight = height / 3;
        final double noiceCache[][][] = new double[2][2][2];

        final List<Pos> treePositions = Lists.newArrayList();
        final List<Integer> treeHeights = Lists.newArrayList();
        final Random rand = new Random(xStart * 10000 + zStart);

        for (int x = xStart; x < xEnd; x += spacing * scale) {
            for (int y = 0; y < height; y += spacing * scale) {
                for (int z = zStart; z < zEnd; z += spacing * scale) {

                    for (int xx = 0; xx < 2; xx++) {
                        for (int zz = 0; zz < 2; zz++) {
                            for (int yy = 0; yy < 2; yy++) {
                                noiceCache[xx][yy][zz] = noisePoint(x + xx * spacing * scale, y + yy * spacing * scale,
                                        z + zz * spacing * scale, 1.0);
                            }
                        }
                    }

                    for (int xx = 0; xx < spacing * scale; xx++) {
                        for (int yy = 0; yy < spacing * scale; yy++) {
                            for (int zz = 0; zz < spacing * scale; zz++) {
                                final double noise = interpolatedNoise(noiceCache, xx, yy, zz) * 1.8 + 0.5;

                                final double heightval = (double) (y + yy) / (double) height;
                                final double testval = Math.sin(heightval * MathUtils.HALF_PI);
                                final double noiseTest = MathUtils.clamp(noise, 0.05, 0.95);
                                if (testval < noiseTest) {
                                    int type = 1;
                                    if (noiseTest - testval > 0.3) {
                                        type = 7;
                                    }

                                    blockScene.setBlock(x + xx, y + yy, z + zz, type);
                                } else {
                                    if (y + yy < waterHeight) {
                                        blockScene.setBlock(x + xx, y + yy, z + zz, BlockWorld.WATER);
                                    } else {
                                        blockScene.setBlock(x + xx, y + yy, z + zz, 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int x = xStart; x < xEnd; x += spacing) {
            for (int z = zStart; z < zEnd; z += spacing) {

                boolean top = true;
                int blockId = 0;
                int topHeight = height - 1;
                while (topHeight > waterHeight - 5) {
                    for (; topHeight > 0; topHeight--) {
                        blockId = blockScene.getBlock(x, topHeight, z);

                        if (blockId != 0 && blockId != BlockWorld.WATER) {
                            break;
                        }
                    }
                    topHeight++;

                    if (topHeight > waterHeight - 5) {
                        final double noise1 = ImprovedNoise.noise(x * 0.1, 0, z * 0.1) * 0.5 + 0.4;
                        final double noise2 = ImprovedNoise.noise(x * 0.01, 0, z * 0.01) * 0.5 + 0.4;
                        final int h = (int) (noise1 * noise2 * 10);

                        if (topHeight < waterHeight) {
                            for (int y = 0; y < h; y++) {
                                blockScene.setBlock(x, topHeight + y, z, 12);
                            }
                            blockScene.setBlock(x, topHeight + h, z, 12);
                        } else {
                            for (int y = 0; y < h; y++) {
                                blockScene.setBlock(x, topHeight + y, z, 3);
                            }

                            if (top) {
                                blockScene.setBlock(x, topHeight + h, z, 2);

                                final boolean addedTree = checkAddTree(x, z, xStart, zStart, xEnd, zEnd, topHeight + h
                                        + 1, treePositions, treeHeights, rand);

                                if (!addedTree) {
                                    final double noiseVegetation = ImprovedNoise.noise(x * 0.4, 0, z * 0.4)
                                            * (Math.abs(noise2) - 0.2) * 2.0;
                                    if (noiseVegetation > (double) (topHeight + h - waterHeight) / height) {
                                        blockScene.setBlock(x, topHeight + h + 1, z, 100 + MathUtils.rand.nextInt(8));
                                    }
                                }
                                top = false;
                            }
                        }
                    }

                    topHeight--;
                    for (; topHeight > 0; topHeight--) {
                        blockId = blockScene.getBlock(x, topHeight, z);

                        if (blockId == 0) {
                            break;
                        }
                    }

                }
            }
        }

        int index = 0;
        for (final Pos pos : treePositions) {
            final int treeHeight = treeHeights.get(index++);
            addTree(blockScene, pos, treeHeight, rand);
        }
    }

    private double noisePoint(final double x, final double y, final double z, final double ySeed) {
        final double noise1 = ImprovedNoise.noise(x * 0.001, y * 0.002 + ySeed, z * 0.001);
        final double noise2 = ImprovedNoise.noise(x * 0.003, y * 0.006 + ySeed * 2, z * 0.003);
        final double noise3 = ImprovedNoise.noise(x * 0.009, y * 0.018 + ySeed * 3, z * 0.009);
        final double noise4 = ImprovedNoise.noise(x * 0.027, y * 0.054 + ySeed * 4, z * 0.027);
        final double noise5 = ImprovedNoise.noise(x * 0.054, y * 0.1 + ySeed * 4, z * 0.054);

        double noise = 0.0;
        noise += noise1;
        noise += noise2 * 0.9;
        noise += noise3 * 0.8;
        noise += noise4 * 0.7;
        noise += noise5 * 0.6;

        noise /= 4.0;

        return noise;
    }

    private double interpolatedNoise(final double[][][] noiseCache, final int xTest, final int yTest, final int zTest) {
        final int xx = 0;
        final int yy = 0;
        final int zz = 0;

        final double x = xTest / scale;
        final double y = yTest / scale;
        final double z = zTest / scale;

        final double v000 = noiseCache[xx][yy][zz];
        final double v100 = noiseCache[xx + 1][yy][zz];
        final double v010 = noiseCache[xx][yy + 1][zz];
        final double v001 = noiseCache[xx][yy][zz + 1];
        final double v101 = noiseCache[xx + 1][yy][zz + 1];
        final double v011 = noiseCache[xx][yy + 1][zz + 1];
        final double v110 = noiseCache[xx + 1][yy + 1][zz];
        final double v111 = noiseCache[xx + 1][yy + 1][zz + 1];

        final double Vxyz = v000 * (1.0 - x) * (1.0 - y) * (1.0 - z) + //
                v100 * x * (1.0 - y) * (1.0 - z) + //
                v010 * (1.0 - x) * y * (1.0 - z) + //
                v001 * (1.0 - x) * (1.0 - y) * z + //
                v101 * x * (1.0 - y) * z + //
                v011 * (1.0 - x) * y * z + //
                v110 * x * y * (1.0 - z) + //
                v111 * x * y * z;
        return Vxyz;
    }

    private final int[] height = new int[] {
            5, 7, 9
    };

    private boolean checkAddTree(final int x, final int z, final int xStart, final int zStart, final int xEnd,
            final int zEnd, final int localHeight, final List<Pos> treePositions, final List<Integer> treeHeights,
            final Random rand) {
        final int treeHeight = height[rand.nextInt(height.length)];
        final int testHeight = (treeHeight - 1) / 2;

        if (x >= xStart + testHeight && x < xEnd - testHeight && z >= zStart + testHeight && z < zEnd - testHeight) {
            final double noiseTree = (ImprovedNoise.noise(x * 0.01, localHeight * 0.005, z * 0.01) + 0.3) * 0.2;
            final double r = rand.nextDouble();
            if (noiseTree > r) {
                treePositions.add(new Pos(x, localHeight, z));
                treeHeights.add(treeHeight);
                return true;
            }
        }
        return false;
    }

    private void addTree(final WorldModifier blockScene, final Pos pos, final int treeHeight, final Random rand) {
        for (int y = 0; y < treeHeight; y++) {
            blockScene.setBlock(pos.x, pos.y + y, pos.z, 17);
        }

        for (int x = 0; x < treeHeight; x++) {
            for (int z = 0; z < treeHeight; z++) {
                for (int y = 0; y < treeHeight; y++) {
                    final int xx = x - (treeHeight - 1) / 2;
                    final int yy = y - (treeHeight - 1) / 2;
                    final int zz = z - (treeHeight - 1) / 2;
                    if (xx == 0 && zz == 0 && yy <= 0) {
                        continue;
                    }
                    final double test = MathUtils.sqrt((double) xx * xx + yy * yy + zz * zz);
                    if (test < (treeHeight - 1.0) / 2.0) {
                        if (rand.nextDouble() < 0.8) {
                            blockScene.setBlock(pos.x + xx, pos.y + yy + treeHeight - 1, pos.z + zz, 18);
                        }
                    }
                }
            }
        }
    }
}
