/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.examples.thegame;

import java.io.File;
import java.util.concurrent.Callable;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyHeldCondition;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardorcraft.base.ArdorCraftGame;
import com.ardorcraft.base.CanvasRelayer;
import com.ardorcraft.collision.IntersectionResult;
import com.ardorcraft.data.Pos;
import com.ardorcraft.generators.NiceDataGenerator;
import com.ardorcraft.geometryproducers.MeshProducer;
import com.ardorcraft.objects.QuadBox;
import com.ardorcraft.objects.SkyDome;
import com.ardorcraft.player.PlayerWithPhysics;
import com.ardorcraft.util.BlockUtil;
import com.ardorcraft.util.geometryproducers.BoxProducer;
import com.ardorcraft.util.queue.ArdorCraftTaskQueue;
import com.ardorcraft.voxel.Voxelator;
import com.ardorcraft.world.BlockWorld;
import com.ardorcraft.world.BlockWorld.BlockSide;
import com.ardorcraft.world.BlockWorld.BlockType;
import com.ardorcraft.world.BlockWorld.Type;
import com.ardorcraft.world.WorldSettings;
import com.google.common.base.Predicate;

/**
 * A bigger example that will grow over time...
 */
public class RealGame implements ArdorCraftGame {

    private BlockWorld blockWorld;
    private final int tileSize = 16;
    private final int gridSize = 16;
    private final int height = 128;
    private final double farPlane = (gridSize - 1) / 2 * tileSize;

    private final IntersectionResult intersectionResult = new IntersectionResult();

    private final FogState fogState = new FogState();
    private final ColorRGBA fogColor = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
    private final ColorRGBA topColor = new ColorRGBA(0.5f, 0.6f, 1.0f, 1.0f);

    private CanvasRelayer canvas;
    private Node root;
    private Camera camera;
    private PlayerWithPhysics player;

    private Node worldNode;
    private Node textNode;
    private SkyDome skyDome;
    private QuadBox selectionBox;

    private int blockType = 1;
    private int globalLight = 15;
    private boolean isInWater = false;
    private final int[] blockTypeLookup = new int[] {
            1, 47, 4, 5, 20, 95, 12, 45, 48, 50
    };

    @Override
    public void update(final ReadOnlyTimer timer) {
        player.update(blockWorld, timer);

        blockWorld.tracePicking(player.getPosition(), player.getDirection(), 50, intersectionResult);
        if (intersectionResult.hit) {
            final Pos hitPos = intersectionResult.pos;
            selectionBox.setTranslation(hitPos.x + 0.5, hitPos.y + 0.5, hitPos.z + 0.5);
        }

        camera.setLocation(player.getPosition());
        camera.setDirection(player.getDirection());
        camera.setUp(player.getUp());
        camera.setLeft(player.getLeft());

        skyDome.setTranslation(player.getPosition());

        updateFog(player.getPosition());

        // The infinite world update
        blockWorld.updatePosition(player.getPosition());
        blockWorld.update(timer);
    }

    @Override
    public void render(final Renderer renderer) {
        // root.draw(renderer);

        // Taking over the drawing to draw in specific order without performance
        // hogging renderqueue sorting...
        skyDome.draw(renderer);
        worldNode.draw(renderer);
        if (intersectionResult.hit) {
            selectionBox.draw(renderer);
        }
        textNode.draw(renderer);
    }

    @Override
    public void init(final Node root, final CanvasRelayer canvas, final LogicalLayer logicalLayer,
            final PhysicalLayer physicalLayer, final MouseManager mouseManager) {
        this.root = root;
        this.canvas = canvas;

        canvas.setTitle("Real!");

        camera = canvas.getCanvasRenderer().getCamera();
        camera.setFrustumPerspective(75.0, (float) camera.getWidth() / (float) camera.getHeight(), 0.1, farPlane);

        setupFog();

        // Create player object
        player = new PlayerWithPhysics(logicalLayer);
        player.getPosition().set(15, 50, 15);
        player.setWalking(true);

        registerTriggers(logicalLayer, mouseManager);

        // Map file to use
        final File worldFileSource = new File("worldReal.acr");
        // Uncomment this if you want to start your mapfile from scratch each run...
        // if (worldFileSource.exists()) {
        // worldFileSource.delete();
        // }

        // Create main blockworld handler
        final WorldSettings settings = new WorldSettings();

        // Here you can load any terrain texture you wish (should contain 16x16 tiles).
        // Just make sure you set the correct tilesize, that is, the subtexture size in pixels.
        settings.setTerrainTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "terrain.png"));
        settings.setTerrainTextureTileSize(32);

        settings.setWaterTexture(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, "water.png"));
        settings.setTerrainGenerator(new NiceDataGenerator());
        settings.setMapFile(worldFileSource);
        settings.setTileSize(tileSize);
        settings.setTileHeight(height);
        settings.setGridSize(gridSize);

        blockWorld = new BlockWorld(settings);

        // Set block 45 (brickblock) to be a pyramid drawn with the meshproducer
        final BlockUtil blockUtil = blockWorld.getBlockUtil();
        final int blockId = 45;
        blockUtil.setBlockMapping(blockId, 7, 0); // brick block tile coords
        blockUtil.setBlockType(blockId, Type.Transparent); // Not covering the entire box block = not solid
        final Mesh mesh = new Pyramid("pyramid", 1.0, 1.0);
        final MeshProducer meshProducer = new MeshProducer(mesh);
        meshProducer.createOrientations(); // create all permutation rotations of the mesh
        meshProducer.setTransformTextureCoords(true); // transform 0-1 texcoords to the specific tile
        blockUtil.setGeometryProducer(blockId, meshProducer);

        worldNode = blockWorld.getWorldNode();
        root.attachChild(worldNode);

        skyDome = new SkyDome("Dome", 8, 8, 10);
        root.attachChild(skyDome);

        textNode = new Node("text");
        root.attachChild(textNode);
        createText("+", canvas.getCanvasRenderer().getCamera().getWidth() / 2 - 5, canvas.getCanvasRenderer()
                .getCamera().getHeight() / 2 - 10);
        createText("[Y/H] Change time of day", 10, 10);
        createText("[V] Voxelate a mesh at current target pos", 10, 30);
        createText("[F] Fly/Walk", 10, 50);
        createText("[0..9] Select blocktype (9=torch)", 10, 70);
        createText("[LMB/RMB] Add/Remove block", 10, 90);

        // Create box to show selected box
        selectionBox = new QuadBox("SelectionBox", new Vector3(), 0.501, 0.501, 0.501);
        selectionBox.getSceneHints().setNormalsMode(NormalsMode.Off);
        selectionBox.setDefaultColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 0.4f));
        selectionBox.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        final BlendState bs = new BlendState();
        bs.setBlendEnabled(true);
        selectionBox.setRenderState(bs);
        final WireframeState ws = new WireframeState();
        ws.setLineWidth(2);
        selectionBox.setRenderState(ws);
        selectionBox.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        root.attachChild(selectionBox);

        updateLighting();

        blockWorld.startThreads();
    }

    private void createText(final String text, final int x, final int y) {
        final BasicText info = BasicText.createDefaultTextLabel("Text2", text, 16);
        info.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        info.setTranslation(new Vector3(x, y, 0));
        textNode.attachChild(info);
    }

    private void updateLighting() {
        final float light = blockWorld.lookupLighting(globalLight);
        final ReadOnlyColorRGBA newColor = new ColorRGBA(fogColor).multiplyLocal(light);
        fogState.setColor(newColor);
        skyDome.getMidColor().set(newColor);
        skyDome.getTopColor().set(topColor).multiplyLocal(light);
        skyDome.updateColors();

        GameTaskQueueManager.getManager(ContextManager.getCurrentContext()).getQueue(ArdorCraftTaskQueue.RENDER)
                .enqueue(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        canvas.getCanvasRenderer().getRenderer().setBackgroundColor(newColor);
                        return true;
                    }
                });
    }

    private void setupFog() {
        fogState.setDensity(1.0f);
        fogState.setEnabled(true);
        fogState.setEnd((float) farPlane);
        fogState.setStart((float) farPlane / 3.0f);
        fogState.setDensityFunction(FogState.DensityFunction.Linear);
        fogState.setQuality(FogState.Quality.PerPixel);
        root.setRenderState(fogState);
    }

    private void updateFog(final Vector3 position) {
        final int block = blockWorld.getBlock((int) position.getX(), (int) (position.getY() + 0.15),
                (int) position.getZ(), BlockType.All);
        if (block == BlockWorld.WATER && !isInWater) {
            isInWater = true;
            fogColor.set(0.1f, 0.13f, 0.25f, 1.0f);
            topColor.set(0.15f, 0.2f, 0.35f, 1.0f);
            fogState.setStart(0);
            fogState.setEnd((float) farPlane / 8);
            updateLighting();
        } else if (block == 0 && isInWater) {
            isInWater = false;
            fogColor.set(1.0f, 1.0f, 1.0f, 1.0f);
            topColor.set(0.5f, 0.6f, 1.0f, 1.0f);
            fogState.setEnd((float) farPlane);
            fogState.setStart((float) farPlane / 3.0f);
            updateLighting();
        }
    }

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
                player.setWalking(!player.isWalking());
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyHeldCondition(Key.SPACE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                player.jump();
            }
        }));

        final Predicate<TwoInputStates> numberPressed = new Predicate<TwoInputStates>() {
            @Override
            public boolean apply(final TwoInputStates states) {
                final char keyChar = states.getCurrent().getKeyboardState().getKeyEvent().getKeyChar();
                if (Character.isDigit(keyChar)) {
                    blockType = blockTypeLookup[Character.digit(keyChar, 10)];
                    return true;
                }
                return false;
            }
        };
        logicalLayer.registerTrigger(new InputTrigger(numberPressed, new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {}
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.Y), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                globalLight = Math.min(globalLight + 1, 15);
                blockWorld.setGlobalLight(globalLight);
                updateLighting();
            }
        }));
        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                globalLight = Math.max(globalLight - 1, 0);
                blockWorld.setGlobalLight(globalLight);
                updateLighting();
            }
        }));

        logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                if (intersectionResult.hit) {
                    final Pos addPos = intersectionResult.oldPos;
                    final Voxelator voxelator = new Voxelator(blockWorld, 50, 50, 50);
                    voxelator.voxelate(addPos, new Teapot(), 1.0f, 43);
                }
            }
        }));

        if (mouseManager.isSetGrabbedSupported()) {
            mouseManager.setGrabbed(GrabbedState.GRABBED);
        }
    }

    @Override
    public void destroy() {
        blockWorld.destroy();
    }

    @Override
    public void resize(final int newWidth, final int newHeight) {}

    private void addBlock() {
        if (intersectionResult.hit) {
            final Pos addPos = intersectionResult.oldPos;
            if (!player.isPlayerSpace(addPos)) {
                final BlockSide orientation = getOrientation(blockType);
                blockWorld.setBlock(addPos.x, addPos.y, addPos.z, blockType, orientation);
            }
        }
    }

    private void removeBlock() {
        if (intersectionResult.hit) {
            final Pos deletePos = intersectionResult.pos;
            blockWorld.setBlock(deletePos.x, deletePos.y, deletePos.z, 0);
        }
    }

    private BlockSide getOrientation(final int blockId) {
        BlockSide orientation = BlockSide.Front;

        if (blockWorld.getBlockUtil().getGeometryProducer(blockId) instanceof BoxProducer) {
            final Vector3 dir = player.getDirection();
            if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
                if (dir.getX() < 0) {
                    orientation = BlockSide.Right;
                } else {
                    orientation = BlockSide.Left;
                }
            } else {
                if (dir.getZ() < 0) {
                    orientation = BlockSide.Front;
                } else {
                    orientation = BlockSide.Back;
                }
            }
        } else {
            final int xDir = intersectionResult.oldPos.x - intersectionResult.pos.x;
            final int yDir = intersectionResult.oldPos.y - intersectionResult.pos.y;
            final int zDir = intersectionResult.oldPos.z - intersectionResult.pos.z;
            if (xDir != 0) {
                if (xDir > 0) {
                    orientation = BlockSide.Right;
                } else {
                    orientation = BlockSide.Left;
                }
            } else if (yDir != 0) {
                if (yDir > 0) {
                    orientation = BlockSide.Bottom;
                } else {
                    orientation = BlockSide.Top;
                }
            } else if (zDir != 0) {
                if (zDir > 0) {
                    orientation = BlockSide.Front;
                } else {
                    orientation = BlockSide.Back;
                }
            }
        }

        return orientation;
    }
}
