
package com.ardorcraft.base;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.CanvasRenderer;

public interface CanvasRelayer {
    CanvasRenderer getCanvasRenderer();

    Canvas getCanvas();

    void setTitle(String title);

    void setVSyncEnabled(boolean enabled);
}
