
package com.ardorcraft.base;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.NativeCanvas;

public class NativeCanvasRelayer implements CanvasRelayer {

    private final NativeCanvas canvas;

    public NativeCanvasRelayer(final NativeCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public CanvasRenderer getCanvasRenderer() {
        return canvas.getCanvasRenderer();
    }

    @Override
    public void setTitle(final String title) {
        canvas.setTitle(title);
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
