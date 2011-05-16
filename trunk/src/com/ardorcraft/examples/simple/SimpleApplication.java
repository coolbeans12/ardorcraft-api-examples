
package com.ardorcraft.examples.simple;

import com.ardorcraft.base.ArdorBaseApplication;

public class SimpleApplication extends ArdorBaseApplication {

    public SimpleApplication() {
        super(new SimpleGame());
    }

    public static void main(final String[] args) {
        final ArdorBaseApplication example = new SimpleApplication();
        new Thread(example, "MainArdorThread").start();
    }
}
