package com.tavares.hideme;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Entry point for dedicated server specific wiring.
 */
public final class HideMeServer implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		Main.LOGGER.info("Hide-Me dedicated server components initialized.");

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			PlayerVisibility.refreshTracking(player);
			PlayerVisibility.sendCurrentHiddenStateTo(player);
			PlayerVisibility.schedulePostJoinSync(server, player);
		});

		ServerTickEvents.END_SERVER_TICK.register(PlayerVisibility::tick);

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			PlayerVisibility.removeTracking(handler.getPlayer().getUuid()));
	}
}
