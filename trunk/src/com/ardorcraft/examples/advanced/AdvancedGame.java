/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.examples.advanced;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyHeldCondition;
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
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.data.Pos;
import com.ardorcraft.generators.NiceDataGenerator;
import com.ardorcraft.player.PlayerWithPhysics;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.BlockWorld.BlockType;
import com.ardorcraft.world.WorldSettings;

/**
 * Adds physics and some other things to the intermediate example
 */
public class AdvancedGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int gridSize = 16;
    private final double farPlane = (gridSize - 1) / 2 * tileSize;

    private final ReadOnlyColorRGBA fogColor = new ColorRGBA(0.9f, 0.9f, 1.0f, 1.0f);
    private Node root;
    private Camera camera;
    private PlayerWithPhysics player;

    @Override
    public void update(final ReadOnlyTimer timer) {
        player.update(blockWorld, timer);

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

        canvas.setTitle("Advanced");
        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(fogColor);

        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, farPlane);

        setupFog();

        // Create player object
        player = new PlayerWithPhysics(logicalLayer);
        player.setWalking(true);
        player.getPosition().set(15, 40, 15);

        registerTriggers(logicalLayer, mouseManager);

        // Create block world. Not setting any map file leads to automatic creation of a world.acr map file (which is
        // overwritten at each run)
        final WorldSettings settings = new WorldSettings();
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrain.png"));
        settings.setTerrainTextureTileSize(32);
        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTerrainGenerator(new NiceDataGenerator());
        settings.setTileSize(tileSize);
        settings.setTileHeight(100);
        settings.setGridSize(gridSize);

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

        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.SPACE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                player.jump();
            }
        }));

        if (mouseManager.isSetGrabbedSupported()) {
            mouseManager.setGrabbed(GrabbedState.GRABBED);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public void resize(final int newWidth, final int newHeight) {}

    private void addBlock() {
        blockWorld
                .traceCollision(player.getPosition(), player.getDirection(), 200, BlockType.Solid, intersectionResult);
        if (intersectionResult.hit) {
            final Pos addPos = intersectionResult.oldPos;
            blockWorld.setBlock(addPos.x, addPos.y, addPos.z, 3, true);
        }
    }

    private void removeBlock() {
        blockWorld
                .traceCollision(player.getPosition(), player.getDirection(), 200, BlockType.Solid, intersectionResult);
        if (intersectionResult.hit) {
            final Pos deletePos = intersectionResult.pos;
            blockWorld.setBlock(deletePos.x, deletePos.y, deletePos.z, 0, true);
        }
    }

}
