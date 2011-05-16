
package com.ardorcraft.examples.intermediate;

import com.ardorcraft.base.ArdorBaseApplication;

public class IntermediateApplication extends ArdorBaseApplication {

    public IntermediateApplication() {
        super(new IntermediateGame());
    }

    public static void main(final String[] args) {
        final ArdorBaseApplication example = new IntermediateApplication();
        new Thread(example, "MainArdorThread").start();
    }
}
