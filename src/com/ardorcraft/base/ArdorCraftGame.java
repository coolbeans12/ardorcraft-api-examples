
package com.ardorcraft.base;

import com.ardor3d.input.MouseManager;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.ReadOnlyTimer;

public interface ArdorCraftGame {

    void render(final Renderer renderer);

    void update(final ReadOnlyTimer timer);

    void init(final Node root, final CanvasRelayer canvas, final LogicalLayer logicalLayer,
            final PhysicalLayer physicalLayer, final MouseManager mouseManager);

    void resize(final int newWidth, final int newHeight);

    void destroy();

}