package com.tavares.hideme.mixin;

import com.tavares.hideme.PlayerVisibility;
import com.tavares.hideme.access.ServerPlayNetworkHandlerAccess;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin implements ServerPlayNetworkHandlerAccess {
	@Shadow public ServerPlayerEntity player;

	@Override
	public ServerPlayerEntity hideMe$getPlayer() {
		return player;
	}

	@Redirect(
		method = "cleanUp",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
		)
	)
	private void hideMe$filterLeaveBroadcast(PlayerManager manager, Text message, boolean overlay) {
		if (PlayerVisibility.shouldHide(player)) {
			for (ServerPlayerEntity viewer : manager.getPlayerList()) {
				if (PlayerVisibility.canViewerBypass(viewer)) {
					viewer.sendMessageToClient(message, overlay);
				}
			}
		} else {
			manager.broadcast(message, overlay);
		}
	}
}
