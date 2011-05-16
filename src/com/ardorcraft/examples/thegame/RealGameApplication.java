package com.ardorcraft.examples.thegame;

import com.ardorcraft.base.ArdorBaseApplication;

public class RealGameApplication extends ArdorBaseApplication {

	public RealGameApplication() {
		super(new RealGame());
	}

	public static void main(final String[] args) {
		final ArdorBaseApplication example = new RealGameApplication();
		new Thread(example, "MainArdorThread").start();
	}
}
