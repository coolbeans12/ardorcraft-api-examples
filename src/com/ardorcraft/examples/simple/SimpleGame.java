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

import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.control.FlyControl;
import com.ardorcraft.generators.LayerDataGenerator;
import com.ardorcraft.player.PlayerBase;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.WorldModifier;
import com.ardorcraft.world.WorldSettings;

/**
 * A simple example showing the very basics of block world building
 */
public class SimpleGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int gridSize = 16;
    private final int height = 64;
    private final double farPlane = (gridSize - 1) / 2 * tileSize;

    private final ReadOnlyColorRGBA fogColor = new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f);
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
        blockWorld.updatePosition(player.getPosition());
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

        canvas.setTitle("Simple");
        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(fogColor);

        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, farPlane);

        setupFog();

        // Create player object
        player = new PlayerBase();
        player.getPosition().set(0, 50, 0);
        FlyControl.setupTriggers(player, logicalLayer, Vector3.UNIT_Y, true);

        // Create block world. Not setting any map file leads to automatic creation of a world.acr map file (which is
        // overwritten at each run)
        final WorldSettings settings = new WorldSettings();
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrainQ.png"));
        settings.setTerrainTextureTileSize(16);
        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTerrainGenerator(simpleSineGenerator);
        settings.setTileSize(tileSize);
        settings.setTileHeight(height);
        settings.setGridSize(gridSize);

        blockWorld = new BlockWorld(settings);
        root.attachChild(blockWorld.getWorldNode());

        blockWorld.startThreads();
    }

    private void setupFog() {
        final FogState fogState = new FogState();
        fogState.setDensity(1.0f);
        fogState.setEnabled(true);
        fogState.setColor(fogColor);
        fogState.setEnd((float) farPlane);
        fogState.setStart((float) farPlane / 3.0f);
        fogState.setDensityFunction(FogState.DensityFunction.Linear);
        fogState.setQuality(FogState.Quality.PerPixel);
        root.setRenderState(fogState);
    }

    @Override
    public void destroy() {}

    @Override
    public void resize(final int newWidth, final int newHeight) {}

    LayerDataGenerator simpleSineGenerator = new LayerDataGenerator(1, 0) {
        @Override
        public boolean isCave(final int x, final int y, final int z, final WorldModifier blockScene) {
            return false;
        }

        @Override
        public int getLayerType(final int layer, final int x, final int z, final WorldModifier blockScene) {
            return 4;
        }

        @Override
        public int getLayerHeight(final int layer, final int x, final int y, final int z, final WorldModifier blockScene) {
            return (int) (Math.abs(Math.sin(x * 0.1) * Math.cos(z * 0.1)) * height / 2);
        }
    };
}
