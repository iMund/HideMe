package com.tavares.hideme.mixin;

import com.tavares.hideme.PlayerVisibility;
import com.tavares.hideme.access.PlayerListPacketAccess;
import com.tavares.hideme.access.ServerPlayNetworkHandlerAccess;
import java.util.List;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonNetworkHandler.class)
abstract class ServerCommonNetworkHandlerMixin {
	@Unique
	private List<PlayerListS2CPacket.Entry> hideMe$originalEntries;

	@Inject(method = "sendPacket", at = @At("HEAD"))
	private void hideMe$maskTabListHead(Packet<?> packet, CallbackInfo ci) {
		hideMe$originalEntries = null;
		if (!(packet instanceof PlayerListS2CPacket listPacket)) {
			return;
		}

		ServerPlayerEntity viewer = (this instanceof ServerPlayNetworkHandlerAccess accessor) ? accessor.hideMe$getPlayer() : null;
		hideMe$originalEntries = PlayerVisibility.maskPacketForViewer(listPacket, viewer);
	}

	@Inject(method = "sendPacket", at = @At("TAIL"))
	private void hideMe$maskTabListTail(Packet<?> packet, CallbackInfo ci) {
		if (packet instanceof PlayerListS2CPacket listPacket && hideMe$originalEntries != null) {
			PlayerVisibility.restorePacketEntries(listPacket, hideMe$originalEntries);
		}
		hideMe$originalEntries = null;
	}
}
