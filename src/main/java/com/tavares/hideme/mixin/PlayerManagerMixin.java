package com.tavares.hideme.mixin;

import com.tavares.hideme.PlayerVisibility;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
	@Shadow public abstract List<ServerPlayerEntity> getPlayerList();
	@Shadow public abstract ServerPlayerEntity getPlayer(UUID uuid);

	@Redirect(
		method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
		)
	)
	private void hideMe$filterJoinBroadcast(PlayerManager manager, Text message, boolean overlay, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData) {
		if (PlayerVisibility.shouldHide(player)) {
			for (ServerPlayerEntity viewer : getPlayerList()) {
				if (PlayerVisibility.canViewerBypass(viewer)) {
					viewer.sendMessageToClient(message, overlay);
				}
			}
		} else {
			manager.broadcast(message, overlay);
		}
	}

	@Inject(method = "addToOperators(Lnet/minecraft/server/PlayerConfigEntry;)V", at = @At("TAIL"))
	private void hideMe$onOperatorAdded(PlayerConfigEntry entry, CallbackInfo ci) {
		hideMe$handleOperatorStatusChange(entry);
	}

	@Inject(method = "addToOperators(Lnet/minecraft/server/PlayerConfigEntry;Ljava/util/Optional;Ljava/util/Optional;)V", at = @At("TAIL"))
	private void hideMe$onOperatorAddedWithMeta(PlayerConfigEntry entry, Optional<Integer> level, Optional<Boolean> bypass, CallbackInfo ci) {
		hideMe$handleOperatorStatusChange(entry);
	}

	@Inject(method = "removeFromOperators", at = @At("TAIL"))
	private void hideMe$onOperatorRemoved(PlayerConfigEntry entry, CallbackInfo ci) {
		hideMe$handleOperatorStatusChange(entry);
	}

	private void hideMe$handleOperatorStatusChange(PlayerConfigEntry entry) {
		ServerPlayerEntity player = getPlayer(entry.id());
		if (player != null) {
			PlayerVisibility.refreshTracking(player);
			PlayerVisibility.sendCurrentHiddenStateTo(player);
		}
	}
}
