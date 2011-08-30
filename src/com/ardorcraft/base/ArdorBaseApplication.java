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

import java.awt.EventQueue;
import java.net.URL;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.FrameHandler;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.Updater;
import com.ardor3d.framework.lwjgl.LwjglCanvas;
import com.ardor3d.framework.lwjgl.LwjglCanvasRenderer;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.image.util.ScreenShotImageExporter;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.lwjgl.LwjglControllerWrapper;
import com.ardor3d.input.lwjgl.LwjglKeyboardWrapper;
import com.ardor3d.input.lwjgl.LwjglMouseManager;
import com.ardor3d.input.lwjgl.LwjglMouseWrapper;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.lwjgl.LwjglTextureRendererProvider;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.util.Constants;
import com.ardor3d.util.ContextGarbageCollector;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.Timer;
import com.ardor3d.util.geom.Debugger;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.screen.ScreenExporter;
import com.ardor3d.util.stat.StatCollector;
import com.ardorcraft.util.queue.WorkerManager;

public abstract class ArdorBaseApplication implements Runnable, Updater, Scene {
    private static final Logger logger = Logger.getLogger(ArdorBaseApplication.class.getName());

    private final ArdorCraftGame ardorCraft;

    /** If true (the default) we will call System.exit on end of demo. */
    public static boolean QUIT_VM_ON_EXIT = true;

    protected final LogicalLayer _logicalLayer = new LogicalLayer();

    protected PhysicalLayer _physicalLayer;

    protected final Timer _timer = new Timer();
    protected final FrameHandler _frameHandler = new FrameHandler(_timer);

    protected DisplaySettings _settings;

    protected final Node _root = new Node();

    protected WireframeState _wireframeState;

    protected volatile boolean _exit = false;

    protected NativeCanvas _canvas;

    protected ScreenShotImageExporter _screenShotExp = new ScreenShotImageExporter();

    protected boolean _showBounds = false;
    protected boolean _doShot = false;

    protected MouseManager _mouseManager;

    protected static int _minDepthBits = -1;
    protected static int _minAlphaBits = -1;
    protected static int _minStencilBits = -1;

    public ArdorBaseApplication(final ArdorCraftGame ardorCraft) {
        this.ardorCraft = ardorCraft;

        // Ask for properties
        final PropertiesGameSettings prefs = getAttributes(new PropertiesGameSettings("ardorSettings.properties", null));

        // Convert to DisplayProperties (XXX: maybe merge these classes?)
        final DisplaySettings settings = new DisplaySettings(prefs.getWidth(), prefs.getHeight(), prefs.getDepth(),
                prefs.getFrequency(),
                // alpha
                _minAlphaBits != -1 ? _minAlphaBits : prefs.getAlphaBits(),
                // depth
                _minDepthBits != -1 ? _minDepthBits : prefs.getDepthBits(),
                // stencil
                _minStencilBits != -1 ? _minStencilBits : prefs.getStencilBits(),
                // samples
                prefs.getSamples(),
                // other
                prefs.isFullscreen(), false);

        _settings = settings;

        // get our framework
        final LwjglCanvasRenderer canvasRenderer = new LwjglCanvasRenderer(this);
        _canvas = new LwjglCanvas(canvasRenderer, settings);
        _physicalLayer = new PhysicalLayer(new LwjglKeyboardWrapper(), new LwjglMouseWrapper(),
                new LwjglControllerWrapper(), (LwjglCanvas) _canvas);
        _mouseManager = new LwjglMouseManager();
        TextureRendererFactory.INSTANCE.setProvider(new LwjglTextureRendererProvider());

        // _logicalLayer.registerInput(_canvas, _physicalLayer);

        // Register our example as an updater.
        _frameHandler.addUpdater(this);

        // register our native canvas
        _frameHandler.addCanvas(_canvas);
    }

    @Override
    public void run() {
        try {
            _frameHandler.init();

            while (!_exit) {
                _frameHandler.updateFrame();
                Thread.yield();
            }
            // grab the graphics context so cleanup will work out.
            final CanvasRenderer cr = _canvas.getCanvasRenderer();
            cr.makeCurrentContext();
            quit(_canvas.getCanvasRenderer().getRenderer());
            cr.releaseCurrentContext();
            if (QUIT_VM_ON_EXIT) {
                System.exit(0);
            }
        } catch (final Throwable t) {
            System.err.println("Throwable caught in MainThread - exiting");
            t.printStackTrace(System.err);
        }
    }

    public void exit() {
        ardorCraft.destroy();
        _exit = true;
    }

    @Override
    @MainThread
    public void init() {
        final ContextCapabilities caps = ContextManager.getCurrentContext().getCapabilities();
        logger.info("Display Vendor: " + caps.getDisplayVendor());
        logger.info("Display Renderer: " + caps.getDisplayRenderer());
        logger.info("Display Version: " + caps.getDisplayVersion());
        logger.info("Shading Language Version: " + caps.getShadingLanguageVersion());

        registerInputTriggers();

        AWTImageLoader.registerLoader();

        /**
         * Create a ZBuffer to display pixels closest to the camera above farther ones.
         */
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        _wireframeState = new WireframeState();
        _wireframeState.setEnabled(false);
        _root.setRenderState(_wireframeState);

        _root.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
        _root.getSceneHints().setCullHint(CullHint.Never);

        final CanvasRelayer canvas = new NativeCanvasRelayer(_canvas);
        ardorCraft.init(_root, canvas, _logicalLayer, _physicalLayer, _mouseManager);

        _root.updateGeometricState(0);
    }

    @Override
    @MainThread
    public void update(final ReadOnlyTimer timer) {
        if (_canvas.isClosing()) {
            exit();
        }

        /** update stats, if enabled. */
        if (Constants.stats) {
            StatCollector.update();
        }

        updateLogicalLayer(timer);

        // Execute updateQueue item
        GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.UPDATE)
                .execute();

        /** Call simpleUpdate in any derived classes of ArdorBaseApplication. */
        ardorCraft.update(timer);

        /** Update controllers/render states/transforms/bounds for rootNode. */
        // _root.updateGeometricState(timer.getTimePerFrame(), true);
    }

    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        _logicalLayer.checkTriggers(timer.getTimePerFrame());
    }

    @Override
    @MainThread
    public boolean renderUnto(final Renderer renderer) {
        // Execute renderQueue item
        WorkerManager.getWorker().execute(renderer);

        _root.updateGeometricState(0, true);

        GameTaskQueueManager.getManager(_canvas.getCanvasRenderer().getRenderContext()).getQueue(GameTaskQueue.RENDER)
                .execute(renderer);

        // Clean up card garbage such as textures, vbos, etc.
        ContextGarbageCollector.doRuntimeCleanup(renderer);

        /** Draw the rootNode and all its children. */
        if (!_canvas.isClosing()) {
            /** Call renderExample in any derived classes. */
            ardorCraft.render(renderer);
            if (_showBounds) {
                Debugger.drawBounds(_root, renderer, true);
            }

            if (_doShot) {
                // force any waiting scene elements to be renderer.
                renderer.renderBuckets();
                ScreenExporter.exportCurrentScreen(_canvas.getCanvasRenderer().getRenderer(), _screenShotExp);
                _doShot = false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public PickResults doPick(final Ray3 pickRay) {
        final PrimitivePickResults pickResults = new PrimitivePickResults();
        pickResults.setCheckDistance(true);
        PickingUtil.findPick(_root, pickRay, pickResults);
        processPicks(pickResults);
        return pickResults;
    }

    protected void processPicks(final PrimitivePickResults pickResults) {
        int i = 0;
        while (pickResults.getNumber() > 0
                && pickResults.getPickData(i).getIntersectionRecord().getNumberOfIntersections() == 0
                && ++i < pickResults.getNumber()) {
        }
        if (pickResults.getNumber() > i) {
            final PickData pick = pickResults.getPickData(i);
            System.err.println("picked: " + pick.getTarget() + " at: "
                    + pick.getIntersectionRecord().getIntersectionPoint(0));
        } else {
            System.err.println("picked: nothing");
        }
    }

    protected void quit(final Renderer renderer) {
        ContextGarbageCollector.doFinalCleanup(renderer);
        _canvas.close();
    }

    protected static PropertiesGameSettings getAttributes(final PropertiesGameSettings settings) {
        // Always show the dialog in these examples.
        URL dialogImage = null;
        final String dflt = settings.getDefaultSettingsWidgetImage();
        if (dflt != null) {
            try {
                dialogImage = ResourceLocatorTool.getClassPathResource(ArdorBaseApplication.class, dflt);
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Resource lookup of '" + dflt + "' failed.  Proceeding.");
            }
        }
        if (dialogImage == null) {
            logger.fine("No dialog image loaded");
        } else {
            logger.fine("Using dialog image '" + dialogImage + "'");
        }

        final URL dialogImageRef = dialogImage;
        final AtomicReference<PropertiesDialog> dialogRef = new AtomicReference<PropertiesDialog>();
        final Stack<Runnable> mainThreadTasks = new Stack<Runnable>();
        try {
            if (EventQueue.isDispatchThread()) {
                dialogRef.set(new PropertiesDialog(settings, dialogImageRef, mainThreadTasks));
            } else {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        dialogRef.set(new PropertiesDialog(settings, dialogImageRef, mainThreadTasks));
                    }
                });
            }
        } catch (final Exception e) {
            logger.logp(Level.SEVERE, ArdorBaseApplication.class.getClass().toString(),
                    "ArdorBaseApplication.getAttributes(settings)", "Exception", e);
            return null;
        }

        PropertiesDialog dialogCheck = dialogRef.get();
        while (dialogCheck == null || dialogCheck.isVisible()) {
            try {
                // check worker queue for work
                while (!mainThreadTasks.isEmpty()) {
                    mainThreadTasks.pop().run();
                }
                // go back to sleep for a while
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                logger.warning("Error waiting for dialog system, using defaults.");
            }

            dialogCheck = dialogRef.get();
        }

        if (dialogCheck.isCancelled()) {
            System.exit(0);
        }
        return settings;
    }

    protected void registerInputTriggers() {
        _logicalLayer.registerInput(_canvas, _physicalLayer);

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ESCAPE), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                exit();
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.T), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _wireframeState.setEnabled(!_wireframeState.isEnabled());
                // Either an update or a markDirty is needed here since we did
                // not touch the affected spatial directly.
                _root.markDirty(DirtyType.RenderState);
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.B), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _showBounds = !_showBounds;
            }
        }));

        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F1), new TriggerAction() {
            @Override
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                _doShot = true;
            }
        }));
    }
}
