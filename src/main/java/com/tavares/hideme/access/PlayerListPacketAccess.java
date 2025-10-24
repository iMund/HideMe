package com.tavares.hideme.access;

import java.util.List;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

public interface PlayerListPacketAccess {
	List<PlayerListS2CPacket.Entry> hideMe$getEntries();

	void hideMe$setEntries(List<PlayerListS2CPacket.Entry> entries);

	boolean hideMe$hasAction(PlayerListS2CPacket.Action action);
}
