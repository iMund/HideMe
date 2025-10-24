package com.tavares.hideme;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main implements ModInitializer {
	public static final String MOD_ID = "hide-me";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Runs once the game has finished preparing the mod-loading phase.
		LOGGER.info("Hello Fabric world!");
	}
}
