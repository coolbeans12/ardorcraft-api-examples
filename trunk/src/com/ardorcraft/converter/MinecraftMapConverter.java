
package com.ardorcraft.converter;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import com.ardorcraft.file.WorldFile;
import com.ardorcraft.world.BlockWorld;

public class MinecraftMapConverter {
    public static void main(final String[] args) {
        // Just an example path...
        new MinecraftMapConverter(new File("/Users/rikardherlitz/Documents/code/MinecraftMap/New World"), new File(
                "worldReal.acr"));
    }

    private final byte localData[] = new byte[16 * 16 * 128];

    public MinecraftMapConverter(final File minecraftMapRoot, final File outputArdorCraftMap) {
        try {
            if (outputArdorCraftMap.exists()) {
                outputArdorCraftMap.delete();
            }
            final WorldFile file = new WorldFile(outputArdorCraftMap);

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    final int chunkX = x - 16;
                    final int chunkZ = z - 16;

                    final byte[] data = readChunk(minecraftMapRoot, chunkX, chunkZ);
                    if (data == null) {
                        continue;
                    }

                    for (short i = 0; i < 16; i++) {
                        for (short j = 0; j < 16; j++) {
                            for (short k = 0; k < 128; k++) {
                                final byte block = data[i << 11 | j << 7 | k];
                                if (block == 0) {
                                    localData[i + (k + j * 128) * 16] = 0; // Air
                                } else if (block == 9) {
                                    localData[i + (k + j * 128) * 16] = (byte) BlockWorld.WATER; // Stationary water
                                } else {
                                    localData[i + (k + j * 128) * 16] = block; // Rest of the blocks
                                }
                            }
                        }
                    }

                    file.save(chunkX, chunkZ, localData);
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] readChunk(final File basePath, final int chunkX, final int chunkZ) throws IOException {
        final DataInputStream istream = RegionFileCache.getChunkDataInputStream(basePath, chunkX, chunkZ);
        if (istream == null) {
            return null;
        }
        final Tag test = Tag.readFrom(istream);
        final Tag blocks = test.findTagByName("Blocks");
        return (byte[]) blocks.getValue();
    }
}
