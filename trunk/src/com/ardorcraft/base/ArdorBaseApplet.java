/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardorcraft.base;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.framework.lwjgl.LwjglDisplayCanvas;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.input.Key;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.lwjgl.LwjglControllerWrapper;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseManager;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.lwjgl.LwjglTextureRendererProvider;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.ardorcraft.util.queue.WorkerManager;

/**
 * An example base class for ardor3d/lwjgl applets. This is not meant to be a "best-practices" applet, just a rough demo
 * showing possibilities. As such, there are likely bugs, etc. Please report these. :)
 */
public abstract class ArdorBaseApplet extends Applet implements Scene {
    private static final long serialVersionUID = 1L;

    private final ArdorCraftGame ardorCraft;

    protected DisplaySettings _settings;
    protected LwjglDisplayCanvas _glCanvas;
    protected LogicalLayer _logicalLayer;
    protected PhysicalLayer _physicalLayer;
    protected Canvas _displayCanvas;
    protected LwjglMouseManager _mouseManager;

    protected Thread _gameThread;
    protected boolean _running = false;

    protected final Timer _timer = new Timer();
    protected final Node _root = new Node();

    public ArdorBaseApplet(final ArdorCraftGame ardorCraft) {
        this.ardorCraft = ardorCraft;
    }

    @Override
    public void init() {
        _settings = getSettings();
        setLayout(new BorderLayout(0, 0));
        try {
            _displayCanvas = new Canvas() {
                private static final long serialVersionUID = 1L;

                @Override
                public final void addNotify() {
                    super.addNotify();
                    startLWJGL();
                }

                @Override
                public final void removeNotify() {
                    stopLWJGL();
                    super.removeNotify();
                }
            };
            _displayCanvas.setSize(getWidth(), getHeight());
            add(_displayCanvas, BorderLayout.CENTER);
            _displayCanvas.setFocusable(true);
            _displayCanvas.requestFocus();
            _displayCanvas.setIgnoreRepaint(true);
            _displayCanvas.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    GameTaskQueueManager.getManager(_glCanvas.getCanvasRenderer().getRenderContext()).update(
                            new Callable<Void>() {
                                @Override
                                public Void call() throws Exception {
                                    final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                                    cam.resize(getWidth(), getHeight());
                                    cam.setFrustumPerspective(cam.getFovY(), getWidth() / (float) getHeight(),
                                            cam.getFrustumNear(), cam.getFrustumFar());
                                    ardorCraft.resize(getWidth(), getHeight());
                                    return null;
                                }
                            });
                }
            });
            setVisible(true);
        } catch (final Exception e) {
            System.err.println(e);
            throw new RuntimeException("Unable to create display");
        }
    }

    protected DisplaySettings getSettings() {
        return new DisplaySettings(getWidth(), getHeight(), 8, 0);
    }

    @Override
    public void destroy() {
        ardorCraft.destroy();
        remove(_displayCanvas);
    }

    protected void startLWJGL() {
        _gameThread = new Thread() {
            @Override
            public void run() {
                _running = true;
                try {
                    initGL();
                    initInput();
                    initBaseScene();

                    final CanvasRelayer canvas = new AppletCanvasRelayer(_glCanvas);
                    ardorCraft.init(_root, canvas, _logicalLayer, _physicalLayer, _mouseManager);

                    gameLoop();
                } catch (final LWJGLException ex) {
                    ex.printStackTrace();
                }
            }
        };
        _gameThread.start();
    }

    protected void stopLWJGL() {
        _running = false;
        try {
            _gameThread.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void gameLoop() {
        while (_running) {
            update();
            _glCanvas.draw(null);
            Thread.yield();
        }
    }

    protected void update() {
        _timer.update();

        _logicalLayer.checkTriggers(_timer.getTimePerFrame());

        // Execute updateQueue item
        // ArdorCraftTaskQueueManager
        // .getManager(_glCanvas.getCanvasRenderer().getRenderContext())
        // .getQueue(ArdorCraftTaskQueue.UPDATE).execute();

        ardorCraft.update(_timer);

        // Update controllers/render states/transforms/bounds for rootNode.
        _root.updateGeometricState(_timer.getTimePerFrame(), true);
    }

    protected void initGL() throws LWJGLException {
        TextureRendererFactory.INSTANCE.setProvider(new LwjglTextureRendererProvider());
        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
        _glCanvas = new LwjglDisplayCanvas(_displayCanvas, _settings, canvasRenderer);
        _glCanvas.init();

        // by default, we'll keep it vsync'd
        _glCanvas.setVSyncEnabled(true);
    }

    protected void initInput() {
        _mouseManager = new LwjglMouseManager();
        _logicalLayer = new LogicalLayer();
        _physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(),
                new LwjglControllerWrapper(), _glCanvas);
        _logicalLayer.registerInput(_glCanvas, _physicalLayer);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.F5), new TriggerAction() {
            @Override
            public void perform(final com.ardor3d.framework.Canvas source, final TwoInputStates inputState,
                    final double tpf) {
                try {
                    _glCanvas.setFullScreen(!_glCanvas.isFullScreen());
                    final Camera cam = _glCanvas.getCanvasRenderer().getCamera();
                    if (_glCanvas.isFullScreen()) {
                        final DisplayMode mode = Display.getDisplayMode();
                        cam.resize(mode.getWidth(), mode.getHeight());
                        cam.setFrustumPerspective(cam.getFovY(), mode.getWidth() / (float) mode.getHeight(),
                                cam.getFrustumNear(), cam.getFrustumFar());
                        ardorCraft.resize(mode.getWidth(), mode.getHeight());
                    } else {
                        cam.resize(getWidth(), getHeight());
                        cam.setFrustumPerspective(cam.getFovY(), getWidth() / (float) getHeight(),
                                cam.getFrustumNear(), cam.getFrustumFar());
                        ardorCraft.resize(getWidth(), getHeight());
                    }
                } catch (final LWJGLException ex) {
                    ex.printStackTrace();
                }
            }
        }));
    }

    protected void initBaseScene() {
        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    ArdorBaseApplet.class, "com/ardorlabs/away/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Create a ZBuffer to display pixels closest to the camera above
        // farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        _root.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        _root.getSceneHints().setCullHint(CullHint.Never);
    }

    @Override
    public PickResults doPick(final Ray3 pickRay) {
        // ignore
        return null;
    }

    @Override
    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        WorkerManager.getWorker().execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        ardorCraft.render(renderer);

        return true;
    }
}
