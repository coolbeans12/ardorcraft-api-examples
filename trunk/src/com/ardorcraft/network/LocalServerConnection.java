
package com.ardorcraft.network;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardorcraft.world.BlockSide;
import com.ardorcraft.world.Chunk;
import com.ardorcraft.world.ChunkModifier;
import com.ardorcraft.world.IServerConnection;

/**
 * "Fake" local server implementation which just generate data upon request throught the LocalServerDataHandler and
 * pongs the data back. For real server/client communication, just implement the IServerConnection and send the requests
 * etc to your server.
 */
public class LocalServerConnection implements IServerConnection {
    private final LocalServerDataHandler server;
    private ChunkModifier chunkModifier;

    public LocalServerConnection(final LocalServerDataHandler server) {
        this.server = server;

        final Thread serverRunner1 = new Thread(updateCacheMailbox, "updateCacheMailbox");
        serverRunner1.setDaemon(true);
        serverRunner1.start();
        final Thread serverRunner2 = new Thread(requestChunkMailbox, "requestChunkMailbox");
        serverRunner2.setDaemon(true);
        serverRunner2.start();
    }

    private final ServerRunner updateCacheMailbox = new ServerRunner();
    private final ServerRunner requestChunkMailbox = new ServerRunner();

    class ServerRunner implements Runnable {
        private final ConcurrentLinkedQueue<Callable<Void>> mailBox = new ConcurrentLinkedQueue<Callable<Void>>();

        @Override
        public void run() {
            while (true) {
                final Callable<Void> action = mailBox.poll();
                if (action != null) {
                    try {
                        // System.out.println(Thread.currentThread().getName() + ": " + mailBox.size());
                        action.call();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public ConcurrentLinkedQueue<Callable<Void>> getMailBox() {
            return mailBox;
        }
    }

    @Override
    public void getModifier(final ChunkModifier chunkModifier) {
        this.chunkModifier = chunkModifier;
    }

    @Override
    public void update(final int x, final int z) {
        updateCacheMailbox.getMailBox().offer(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Thread.sleep(MathUtils.rand.nextInt(400) + 200);
                server.updateClientPosition(x, z);
                return null;
            }
        });
    }

    @Override
    public void requestChunk(final int x, final int z) {
        requestChunkMailbox.getMailBox().offer(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final Chunk chunk = server.getChunk(x, z);
                // Thread.sleep(MathUtils.rand.nextInt(400) + 200);
                // Thread.sleep(MathUtils.rand.nextInt(5));
                chunkModifier.postChunk(x, z, chunk);
                return null;
            }
        });
    }

    @Override
    public void setBlock(final int x, final int y, final int z, final int blockId, final BlockSide orientation) {
        requestChunkMailbox.getMailBox().offer(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                server.setBlock(x, y, z, blockId);
                return null;
            }
        });
    }

    public void updatePlayerPosition(final ReadOnlyVector3 location, final ReadOnlyVector3 direction) {}

    public void connect(final String address) {}

    @Override
    public void close() {
        server.close();
    }
}
