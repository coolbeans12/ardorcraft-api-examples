
package com.ardorcraft.network;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.data.Pos;
import com.ardorcraft.file.WorldFile;
import com.ardorcraft.generators.DataGenerator;
import com.ardorcraft.world.Chunk;
import com.ardorcraft.world.WorldModifier;
import com.ardorcraft.world.utils.ChunkDistanceComparator;
import com.google.common.collect.Lists;

/**
 * Simple data handler that works against a map file.
 */
public class LocalServerDataHandler {
    private static final Logger logger = Logger.getLogger(LocalServerDataHandler.class.getName());

    private final int height;
    private final int width;
    private final int gridSize;

    private final WorldFile worldFile;
    private final DataGenerator generator;

    public LocalServerDataHandler(final int width, final int height, final int gridSize, final DataGenerator generator,
            final File mapFile) {
        this.width = width;
        this.height = height;
        this.gridSize = gridSize;
        this.generator = generator;

        try {
            if (mapFile != null) {
                worldFile = new WorldFile(mapFile);
            } else {
                final File file = new File("world.acr");
                if (file.exists()) {
                    file.delete();
                }
                worldFile = new WorldFile(file);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            worldFile.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        System.out.println("World closed");
    }

    private byte[] loadChunkData(final int x, final int z) {
        byte[] block = null;
        try {
            if (!worldFile.contains(x, z)) {
                try {
                    generateChunks(x, z, 1, 1, true);
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    return null;
                }
            }

            block = worldFile.load(x, z);
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return block;
    }

    private void generateChunks(final int X, final int Z, final int chunksWidth, final int chunksHeight,
            final boolean onlyNew) throws Exception {
        final long t = System.currentTimeMillis();

        final byte localBlock[] = new byte[width * width * height];
        final WorldModifier worldEdit = new LocalBlockModifier(localBlock);

        int nrChunksGenerated = 0;
        for (int x = 0; x < chunksWidth; x++) {
            for (int z = 0; z < chunksHeight; z++) {
                final int xx = X + x;
                final int zz = Z + z;

                if (onlyNew && worldFile.contains(xx, zz)) {
                    continue;
                }

                synchronized (generator) {
                    generator.generateChunk(xx * width, zz * width, xx * width + width, zz * width + width, 1, height,
                            worldEdit);
                    Thread.yield();

                    worldFile.save(xx, zz, localBlock);
                    Thread.yield();
                }

                nrChunksGenerated++;
            }
        }
        if (nrChunksGenerated > 0) {
            logger.info(nrChunksGenerated + " new chunk" + (nrChunksGenerated > 1 ? "s" : "") + " generated. (time="
                    + (System.currentTimeMillis() - t) + ")");
        }
    }

    // private final ConcurrentMap<Pos, Chunk> chunkCache = new MapMaker().softValues().makeMap();

    private boolean hasChunk(final int x, final int z) {
        return true;// chunkCache.containsKey(new Pos(x, 0, z));
    }

    public synchronized Chunk getChunk(final int x, final int z) {
        // final Pos chunkPos = new Pos(x, 0, z);
        // Chunk chunk = chunkCache.get(chunkPos);
        // if (chunk == null) {
        final byte[] data = loadChunkData(x, z);
        final Chunk chunk = new Chunk(data, null);
        // chunkCache.put(chunkPos, chunk);
        // }
        return chunk;
    }

    public void setBlock(final int x, final int y, final int z, final int blockId) {
        final int chunkX = MathUtils.floor((float) x / width);
        final int chunkZ = MathUtils.floor((float) z / width);

        final Chunk chunk = getChunk(chunkX, chunkZ);
        setChunkBlock(chunk, MathUtils.moduloPositive(x, width), y, MathUtils.moduloPositive(z, width), blockId);

        saveChunk(chunkX, chunkZ, chunk);
    }

    public int getBlock(final int x, final int y, final int z) {
        final int chunkX = MathUtils.floor((float) x / width);
        final int chunkZ = MathUtils.floor((float) z / width);

        final Chunk chunk = getChunk(chunkX, chunkZ);
        return getChunkBlock(chunk, MathUtils.moduloPositive(x, width), y, MathUtils.moduloPositive(z, width));
    }

    private void setChunkBlock(final Chunk chunk, final int x, final int y, final int z, final int blockId) {
        chunk.getBlocks()[x + (y + z * height) * width] = (byte) blockId;
    }

    private int getChunkBlock(final Chunk chunk, final int x, final int y, final int z) {
        return chunk.getBlocks()[x + (y + z * height) * width] & 0xff;
    }

    private void saveChunk(final int x, final int z, final Chunk chunk) {
        try {
            final byte localBlock[] = chunk.getBlocks();
            worldFile.save(x, z, localBlock);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    class LocalBlockModifier implements WorldModifier {
        private final byte[] localBlock;

        public LocalBlockModifier(final byte[] localBlock) {
            this.localBlock = localBlock;
        }

        @Override
        public void setBlock(final int x, final int y, final int z, final int data) {
            if (y < 0 || y >= height - 1) {
                return;
            }
            final int xx = MathUtils.moduloPositive(x, width);
            final int zz = MathUtils.moduloPositive(z, width);
            localBlock[xx + (y + zz * height) * width] = (byte) data;
        }

        @Override
        public int getBlock(final int x, final int y, final int z) {
            if (y < 0 || y >= height - 1) {
                return 0;
            }
            final int xx = MathUtils.moduloPositive(x, width);
            final int zz = MathUtils.moduloPositive(z, width);
            return localBlock[xx + (y + zz * height) * width] & 0xff;
        }
    }

    private int oldTileX = Integer.MAX_VALUE;
    private int oldTileZ = Integer.MAX_VALUE;
    private final Set<Pos> tileCache = new LinkedHashSet<Pos>();

    class SavingThread implements Runnable {
        @Override
        public void run() {
            // while (!exit) {
            // try {
            // final List<Pos> list = savingMailBox.switchAndGet();
            //
            // if (!list.isEmpty()) {
            // for (final Pos pos : list) {
            // saveChunk(pos.x, pos.z);
            // Thread.sleep(10);
            // }
            // }
            //
            // Thread.sleep(1000);
            // } catch (final InterruptedException e) {
            // e.printStackTrace();
            // }
            // }
            // exitLatch.countDown();
        }
    }

    public void updateClientPosition(int currentTileX, int currentTileZ) {
        currentTileX /= gridSize / 2;
        currentTileZ /= gridSize / 2;

        if (currentTileX == oldTileX && currentTileZ == oldTileZ) {
            return;
        }
        oldTileX = currentTileX;
        oldTileZ = currentTileZ;

        final Set<Pos> newPos = new LinkedHashSet<Pos>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                final int xx = currentTileX + x - 1;
                final int zz = currentTileZ + z - 1;
                newPos.add(new Pos(xx, 0, zz));
            }
        }
        final Iterator<Pos> tileIterator = tileCache.iterator();
        while (tileIterator.hasNext()) {
            final Pos pos = tileIterator.next();

            if (!newPos.contains(pos)) {
                tileIterator.remove();
            } else {
                newPos.remove(pos);
            }
        }
        if (!newPos.isEmpty()) {
            try {
                final List<Pos> sortedPos = Lists.newArrayList();
                sortedPos.addAll(newPos);
                Collections.sort(sortedPos, new ChunkDistanceComparator(currentTileX, currentTileZ));

                for (final Pos pos : sortedPos) {
                    generateChunks(pos.x * gridSize - gridSize / 2, pos.z * gridSize - gridSize / 2, gridSize,
                            gridSize, true);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        tileCache.addAll(newPos);
    }
}
