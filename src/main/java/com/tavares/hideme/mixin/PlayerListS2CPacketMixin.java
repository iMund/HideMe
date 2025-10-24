package com.tavares.hideme.mixin;

import com.google.common.collect.ImmutableList;
import com.tavares.hideme.access.PlayerListPacketAccess;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerListS2CPacket.class)
abstract class PlayerListS2CPacketMixin implements PlayerListPacketAccess {
	@Shadow @Final private EnumSet<PlayerListS2CPacket.Action> actions;
	@Shadow @Final @Mutable private List<PlayerListS2CPacket.Entry> entries;

	@Override
	public List<PlayerListS2CPacket.Entry> hideMe$getEntries() {
		return entries;
	}

	@Override
	public void hideMe$setEntries(List<PlayerListS2CPacket.Entry> newEntries) {
		this.entries = ImmutableList.copyOf(newEntries);
	}

	@Override
	public boolean hideMe$hasAction(PlayerListS2CPacket.Action action) {
		return actions.contains(action);
	}
}
