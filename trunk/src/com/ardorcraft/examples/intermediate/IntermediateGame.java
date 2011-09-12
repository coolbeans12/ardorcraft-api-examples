/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.examples.intermediate;

import java.net.URISyntaxException;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.control.FlyControl;
import com.ardorcraft.data.Pos;
import com.ardorcraft.generators.InterpolatedNoiseDataGenerator;
import com.ardorcraft.network.LocalServerConnection;
import com.ardorcraft.network.LocalServerDataHandler;
import com.ardorcraft.player.PlayerWithCollision;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.IServerConnection;
import com.ardorcraft.world.WorldSettings;

/**
 * Adds some collision and block add/delete functionality to the simple example
 */
public class IntermediateGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int gridSize = 16;
    private final int height = 100;
    private final double farPlane = (gridSize - 1) / 2 * tileSize;

    private final ReadOnlyColorRGBA fogColor = new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f);
    private Node root;
    private Camera camera;
    private PlayerWithCollision player;

    @Override
    public void update(final ReadOnlyTimer timer) {
        player.update(blockWorld, timer);

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

        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    IntermediateGame.class, "com/ardorcraft/resources"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        canvas.setTitle("ArdorCraft API Example - IntermediateGame.java");
        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(fogColor);

        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, farPlane);

        setupFog();

        // Create player object
        player = new PlayerWithCollision();
        player.getPosition().set(0, 50, 0);
        FlyControl.setupTriggers(player, logicalLayer, Vector3.UNIT_Y, false);

        registerTriggers(logicalLayer, mouseManager);

        // Create block world settings.
        final WorldSettings settings = new WorldSettings();
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrainQ.png"));
        settings.setTerrainTextureTileSize(16);
        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTileSize(tileSize);
        settings.setTileHeight(height);
        settings.setGridSize(gridSize);

        // Create a local "fake" server
        final IServerConnection serverConnection = new LocalServerConnection(new LocalServerDataHandler(tileSize,
                height, gridSize, new InterpolatedNoiseDataGenerator(), null));
        settings.setServerConnection(serverConnection);

        blockWorld = new BlockWorld(settings);

        root.attachChild(blockWorld.getWorldNode());

        final BasicText cross = BasicText.createDefaultTextLabel("Text", "+", 16);
        cross.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        cross.setTranslation(new Vector3(canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas
                .getCanvasRenderer().getCamera().getHeight() / 2 - 10, 0));
        root.attachChild(cross);

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

    private final IntersectionResult intersectionResult = new IntersectionResult();

    private void registerTriggers(final LogicalLayer logicalLayer, final MouseManager mouseManager) {
        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT),
                new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                        addBlock();
                    }
                }));

        logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.RIGHT),
                new TriggerAction() {
                    @Override
                    public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                        removeBlock();
                    }
                }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                blockWorld.tracePicking(player.getPosition(), player.getDirection(), 200, intersectionResult);
                if (intersectionResult.hit) {
                    final Pos addPos = intersectionResult.oldPos;
                    for (int x = 0; x < 3; x++) {
                        for (int y = 0; y < 3; y++) {
                            for (int z = 0; z < 3; z++) {
                                blockWorld.setBlock(addPos.x + x - 1, addPos.y + y - 1, addPos.z + z - 1, 3);
                            }
                        }
                    }
                }
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                blockWorld.tracePicking(player.getPosition(), player.getDirection(), 200, intersectionResult);
                if (intersectionResult.hit) {
                    final Pos addPos = intersectionResult.pos;
                    for (int x = 0; x < 3; x++) {
                        for (int y = 0; y < 3; y++) {
                            for (int z = 0; z < 3; z++) {
                                blockWorld.setBlock(addPos.x + x - 1, addPos.y + y - 1, addPos.z + z - 1, 0);
                            }
                        }
                    }
                }
            }
        }));

        if (mouseManager.isSetGrabbedSupported()) {
            mouseManager.setGrabbed(GrabbedState.GRABBED);
        }
    }

    @Override
    public void destroy() {
        blockWorld.stopThreads();
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {}

    private void addBlock() {
        blockWorld.tracePicking(player.getPosition(), player.getDirection(), 200, intersectionResult);
        if (intersectionResult.hit) {
            final Pos addPos = intersectionResult.oldPos;
            blockWorld.setBlock(addPos.x, addPos.y, addPos.z, 3);
        }
    }

    private void removeBlock() {
        blockWorld.tracePicking(player.getPosition(), player.getDirection(), 200, intersectionResult);
        if (intersectionResult.hit) {
            final Pos deletePos = intersectionResult.pos;
            blockWorld.setBlock(deletePos.x, deletePos.y, deletePos.z, 0);
        }
    }

}
