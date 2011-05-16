package com.ardorcraft.generators;

import com.ardorcraft.util.ImprovedNoise;
import com.ardorcraft.world.WorldModifier;

public class CachedNoiseDataGenerator extends LayerDataGenerator {
	private int xStart, zStart;

	private final int scale = 4;
	private final int wSize;
	private final int hSize;
	private final double[][][] noiseCache1;
	private final double[][][] noiseCache2;
	private final double[][][] noiseCache3;
	private final double[][][] noiseCache4;

	public CachedNoiseDataGenerator(final int waterHeight, final int width,
			final int height) {
		super(3, waterHeight);

		wSize = width / scale + 1;
		hSize = height / scale + 1;

		noiseCache1 = new double[wSize][hSize][wSize];
		noiseCache2 = new double[wSize][hSize][wSize];
		noiseCache3 = new double[wSize][hSize][wSize];
		noiseCache4 = new double[wSize][hSize][wSize];
	}

	private double interpolatedNoise(final double[][][] noiseCache,
			final int xTest, final int yTest, final int zTest) {
		final int xx = (xTest - xStart) / scale;
		final int yy = yTest / scale;
		final int zz = (zTest - zStart) / scale;

		final double x = (xTest - xStart - scale * xx) / (double) scale;
		final double y = (yTest - scale * yy) / (double) scale;
		final double z = (zTest - zStart - scale * zz) / (double) scale;

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

	@Override
	public void generateChunk(final int xStart, final int zStart,
			final int xEnd, final int zEnd, final int height,
			final WorldModifier blockScene) {
		this.xStart = xStart;
		this.zStart = zStart;

		for (int xs = 0; xs < wSize; xs++) {
			for (int ys = 0; ys < hSize; ys++) {
				for (int zs = 0; zs < wSize; zs++) {
					final double x = xs * scale + xStart;
					final double y = ys * scale;
					final double z = zs * scale + zStart;
					noiseCache1[xs][ys][zs] = ImprovedNoise.noise(x * 0.005,
							0.5, z * 0.005);
					noiseCache2[xs][ys][zs] = ImprovedNoise.noise(x * 0.02,
							y * 0.02, z * 0.02);
					noiseCache3[xs][ys][zs] = ImprovedNoise.noise(x * 0.02,
							1.0, z * 0.02);
					noiseCache4[xs][ys][zs] = ImprovedNoise.noise(x * 0.05,
							1.0, z * 0.05);
				}
			}
		}

		super.generateChunk(xStart, zStart, xEnd, zEnd, height, blockScene);
	}

	@Override
	public int getLayerHeight(final int layer, final int x, final int z) {
		if (layer == 0) {
			return 5;
		} else if (layer == 1) {
			return (int) Math
					.abs((interpolatedNoise(noiseCache1, x, 0, z) + 0.5) * 30
							+ (interpolatedNoise(noiseCache3, x, 0, z) + 0.5)
							* 20);
		} else if (layer == 2) {
			final double t = interpolatedNoise(noiseCache1, x, 0, z);
			if (t > -0.2) {
				final double t2 = interpolatedNoise(noiseCache4, x, 0, z);
				if (t2 + 0.5 > 0) {
					return (int) (Math.abs(t2 + 0.5) * 30 * (t + 0.2));
				}
			}
		}
		return 0;
	}

	@Override
	public int getLayerType(final int layer, final int x, final int z) {
		if (layer == 0) {
			return 2;
		} else if (layer == 1) {
			return 3;
		} else if (layer == 2) {
			return 4;
		}
		return 3;
	}

	@Override
	public boolean isCave(final int x, final int y, final int z) {
		if (y > 1) {
			final int testHeight = (int) (interpolatedNoise(noiseCache1, x, y,
					z) * 5 + waterHeight);
			if (y > testHeight) {
				return interpolatedNoise(noiseCache2, x, y, z) > Math.abs(y
						- testHeight) / 60.0;
			} else {
				return interpolatedNoise(noiseCache2, x, y, z) > Math.abs(y
						- testHeight) / 40.0 + 0.1;
			}
		}
		return false;
	}

}
