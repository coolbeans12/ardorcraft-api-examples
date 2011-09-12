/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.examples.simple;

import java.net.URISyntaxException;

import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.control.FlyControl;
import com.ardorcraft.generators.LayerDataGenerator;
import com.ardorcraft.network.LocalServerConnection;
import com.ardorcraft.network.LocalServerDataHandler;
import com.ardorcraft.player.PlayerBase;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.IServerConnection;
import com.ardorcraft.world.WorldModifier;
import com.ardorcraft.world.WorldSettings;

/**
 * A simple example showing the very basics of block world building
 */
public class SimpleGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int gridSize = 16;
    private final int height = 32;
    private Node root;
    private Camera camera;
    private PlayerBase player;

    @Override
    public void update(final ReadOnlyTimer timer) {
        camera.setLocation(player.getPosition());
        camera.setDirection(player.getDirection());
        camera.setUp(player.getUp());
        camera.setLeft(player.getLeft());

        // The infinite world update
        blockWorld.updatePlayer(player.getPosition(), player.getDirection());
        blockWorld.update(timer);
    }

    @Override
    public void render(final Renderer renderer) {
        root.draw(renderer);
    }

    @Override
    public void init(final Node root, final CanvasRelayer canvas, final LogicalLayer logicalLayer,
            final PhysicalLayer physicalLayer, final MouseManager mouseManager) {
        this.root = root;

        // Make sure the world can find the textures.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    SimpleGame.class, "com/ardorcraft/resources"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Some window details
        canvas.setTitle("ArdorCraft API Example - SimpleGame.java");
        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.WHITE);

        // Setup camera fov and view distance
        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, 1000);

        // Create player object
        player = new PlayerBase();
        player.getPosition().set(0, 30, 0);
        FlyControl.setupTriggers(player, logicalLayer, Vector3.UNIT_Y, true);

        // Create block world settings.
        final WorldSettings settings = new WorldSettings();
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrainQ.png"));
        settings.setTerrainTextureTileSize(16);
        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTileSize(tileSize);
        settings.setTileHeight(height);
        settings.setGridSize(gridSize);
        settings.setUseVBO(false);

        // Create a local "fake" server
        final IServerConnection serverConnection = new LocalServerConnection(new LocalServerDataHandler(tileSize,
                height, gridSize, simpleSineGenerator, null));
        settings.setServerConnection(serverConnection);

        // Create the actual world and put its world node under our main scenegraph node.
        blockWorld = new BlockWorld(settings);
        root.attachChild(blockWorld.getWorldNode());

        // Start the processing!
        blockWorld.startThreads();
    }

    @Override
    public void destroy() {
        blockWorld.stopThreads();
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {}

    /**
     * A simple world based on a sine wave.
     */
    private final LayerDataGenerator simpleSineGenerator = new LayerDataGenerator(1, 5) {
        @Override
        public boolean isCave(final int x, final int y, final int z, final WorldModifier blockScene) {
            return y > 10 && y < 15;
        }

        @Override
        public int getLayerType(final int layer, final int x, final int z, final WorldModifier blockScene) {
            return MathUtils.rand.nextInt(4) + 1;
        }

        @Override
        public int getLayerHeight(final int layer, final int x, final int y, final int z, final WorldModifier blockScene) {
            return (int) (Math.abs(Math.sin(x * 0.05) * Math.cos(z * 0.05)) * height);
        }
    };
}
