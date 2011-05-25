
package com.ardorcraft.base;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.lwjgl.LwjglDisplayCanvas;

public class AppletCanvasRelayer implements CanvasRelayer {

    private final LwjglDisplayCanvas canvas;

    public AppletCanvasRelayer(final LwjglDisplayCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public CanvasRenderer getCanvasRenderer() {
        return canvas.getCanvasRenderer();
    }

    @Override
    public void setTitle(final String title) {
    // No title for applets
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public void setVSyncEnabled(final boolean enabled) {
        canvas.setVSyncEnabled(enabled);
    }
}
