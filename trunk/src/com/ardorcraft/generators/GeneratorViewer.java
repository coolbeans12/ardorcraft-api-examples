
package com.ardorcraft.generators;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.math.MathUtils;
import com.ardorcraft.world.WorldModifier;

/**
 * Use this to play around more easily with your terrain generators.
 * 
 */
public class GeneratorViewer {
    private static final Logger logger = Logger.getLogger(GeneratorViewer.class.getName());

    public static void main(final String[] args) {
        new GeneratorViewer(new InterpolatedNoiseDataGenerator(), 32, 150, 16);
    }

    private int spacing = 1;

    private final Color[][] colors;

    private final int chunkWidth;
    private final int chunkHeight;
    private final int gridSize;
    private final int totalSize;

    private int offsetX = 0;
    private int offsetZ = 0;

    private final BufferedImage image;
    private final int[] data;
    private final JPanel panel;
    private final JButton button;
    private final JSpinner spinner;

    private int nrUpdateThreads = 1;
    private final ExecutorService executorService = Executors.newCachedThreadPool(new DeamonThreadFactory());
    private final Stack<Future<?>> futureStack = new Stack<Future<?>>();

    private class PaintPanel extends JPanel {
        @Override
        public void paint(final Graphics g) {
            g.drawImage(image, 0, 0, null);
        }
    }

    private final DataGenerator generator;

    private final ThreadLocal<WorldModifier> worldModifierPool = new ThreadLocal<WorldModifier>() {
        @Override
        protected WorldModifier initialValue() {
            return new LocalBlockModifier(new byte[chunkWidth * chunkWidth * chunkHeight]);
        }
    };

    public GeneratorViewer(final DataGenerator generator, final int chunkWidth, final int chunkHeight,
            final int gridSize) {
        this.generator = generator;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.gridSize = gridSize;

        nrUpdateThreads = Runtime.getRuntime().availableProcessors();
        logger.info("Running on " + nrUpdateThreads + " core" + (nrUpdateThreads > 1 ? "s" : ""));

        totalSize = chunkWidth * gridSize;

        panel = new PaintPanel();

        final JPanel controlPanel = new JPanel();
        button = new JButton("Render");
        controlPanel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                render();
            }
        });
        final SpinnerModel model = new SpinnerNumberModel(1, 1, 100, 1);
        spinner = new JSpinner(model);
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                spacing = 1 << ((Integer) model.getValue()).intValue() - 1;
            }
        });
        controlPanel.add(spinner);

        JButton button1;

        button1 = new JButton("Left");
        controlPanel.add(button1);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                offsetX -= 16;
                render();
            }
        });
        button1 = new JButton("Right");
        controlPanel.add(button1);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                offsetX += 16;
                render();
            }
        });
        button1 = new JButton("Up");
        controlPanel.add(button1);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                offsetZ += 16;
                render();
            }
        });
        button1 = new JButton("Down");
        controlPanel.add(button1);
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                offsetZ -= 16;
                render();
            }
        });

        final JFrame frame = new JFrame("Terrain Cache Debug");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.getContentPane().add(controlPanel, BorderLayout.SOUTH);
        frame.setBounds(10, 10, totalSize + 17, totalSize + 78);
        frame.setVisible(true);

        final Random rand = new Random(1337);
        colors = new Color[255][chunkHeight];
        for (int i = 0; i < 255; i++) {
            float r = rand.nextFloat() * 0.5f + 0.5f;
            float g = rand.nextFloat() * 0.5f + 0.5f;
            float b = rand.nextFloat() * 0.5f + 0.5f;

            for (int j = chunkHeight - 1; j >= 0; j--) {
                colors[i][j] = new Color(r, g, b);
                r *= 1.0f - 1.0f / chunkHeight;
                g *= 1.0f - 1.0f / chunkHeight;
                b *= 1.0f - 1.0f / chunkHeight;
            }
        }

        image = new BufferedImage(totalSize, totalSize, BufferedImage.TYPE_INT_RGB);
        data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        render();
    }

    public void render() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(false);
                spinner.setEnabled(false);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (nrUpdateThreads <= 1) {
                    updateChunks(0, gridSize);
                } else {
                    for (int i = 0; i < nrUpdateThreads; i++) {
                        final int from = gridSize * i / nrUpdateThreads;
                        final int to = gridSize * (i + 1) / nrUpdateThreads;
                        final Future<?> future = executorService.submit(new Runnable() {
                            public void run() {
                                updateChunks(from, to);
                            }
                        });
                        futureStack.push(future);
                    }
                    try {
                        while (!futureStack.isEmpty()) {
                            futureStack.pop().get();
                        }
                    } catch (final InterruptedException ex) {
                        logger.log(Level.SEVERE, "InterruptedException in thread execution", ex);
                    } catch (final ExecutionException ex) {
                        logger.log(Level.SEVERE, "ExecutionException in thread execution", ex);
                    }
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        button.setEnabled(true);
                        spinner.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private void updateChunks(final int from, final int to) {
        final WorldModifier worldEdit = worldModifierPool.get();

        for (int x = from * spacing; x < to * spacing; x++) {
            for (int z = 0; z < gridSize * spacing; z++) {
                final int xx = x + offsetX;
                final int zz = z + offsetZ;
                generator.generateChunk(xx * chunkWidth, zz * chunkWidth, xx * chunkWidth + chunkWidth, zz * chunkWidth
                        + chunkWidth, spacing, chunkHeight, worldEdit);
                paintChunk(MathUtils.moduloPositive(xx, gridSize), MathUtils.moduloPositive(zz, gridSize), worldEdit);
                panel.repaint();

                Thread.yield();
            }
        }
    }

    private void paintChunk(final int xx, final int zz, final WorldModifier worldEdit) {
        for (int x = 0; x < chunkWidth / spacing; x++) {
            for (int z = 0; z < chunkWidth / spacing; z++) {
                boolean light = true;
                boolean empty = true;
                for (int y = chunkHeight - 1; y >= 0; y--) {
                    final int blockId = worldEdit.getBlock(x * spacing, y, z * spacing);
                    if (blockId != 0) {
                        final int xpos = xx * chunkWidth / spacing + x;
                        final int ypos = zz * chunkWidth / spacing + z - y;
                        if (ypos >= 0) {
                            data[ypos * totalSize + xpos] = colors[blockId][y].getRGB();
                            if (empty && ypos - 1 >= 0) {
                                int yval = light ? y + 15 : y;
                                yval = empty ? yval + 30 : yval;
                                yval = Math.min(chunkHeight - 1, yval);
                                data[(ypos - 1) * totalSize + xpos] = colors[blockId][yval].getRGB();
                            }
                        }
                        light = false;
                        empty = false;
                    } else {
                        if (y == 0) {
                            final int xpos = xx * chunkWidth / spacing + x;
                            final int ypos = zz * chunkWidth / spacing + z;
                            data[ypos * totalSize + xpos] = Color.black.getRGB();
                        }
                        empty = true;
                    }
                }
            }
        }
    }

    class LocalBlockModifier implements WorldModifier {
        private final byte[] localBlock;

        public LocalBlockModifier(final byte[] localBlock) {
            this.localBlock = localBlock;
        }

        @Override
        public void setBlock(final int x, final int y, final int z, final int data) {
            if (y < 0 || y >= chunkHeight - 1) {
                return;
            }
            final int xx = MathUtils.moduloPositive(x, chunkWidth);
            final int zz = MathUtils.moduloPositive(z, chunkWidth);
            localBlock[xx + (y + zz * chunkHeight) * chunkWidth] = (byte) data;
        }

        @Override
        public int getBlock(final int x, final int y, final int z) {
            if (y < 0 || y >= chunkHeight - 1) {
                return 0;
            }
            final int xx = MathUtils.moduloPositive(x, chunkWidth);
            final int zz = MathUtils.moduloPositive(z, chunkWidth);
            return localBlock[xx + (y + zz * chunkHeight) * chunkWidth] & 0xff;
        }
    }

    private static class DeamonThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger(1);
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DeamonThreadFactory() {
            final SecurityManager s = System.getSecurityManager();
            group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "Worker Pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

}
