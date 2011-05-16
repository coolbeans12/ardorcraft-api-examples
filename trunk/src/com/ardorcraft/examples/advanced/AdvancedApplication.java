package com.ardorcraft.examples.advanced;

import com.ardorcraft.base.ArdorBaseApplication;

public class AdvancedApplication extends ArdorBaseApplication {

	public AdvancedApplication() {
		super(new AdvancedGame());
	}

	public static void main(final String[] args) {
		final ArdorBaseApplication example = new AdvancedApplication();
		new Thread(example, "MainArdorThread").start();
	}
}
