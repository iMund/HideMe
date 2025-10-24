package com.tavares.hideme;

import com.tavares.hideme.access.PlayerListPacketAccess;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Central utilities for hiding players from the tab list.
 */
public final class PlayerVisibility {
	private static final String PERMISSION_NODE = "hide-me.use";
	private static final int OP_LEVEL_THRESHOLD = 2;
	private static final PermissionInvoker PERMISSION_INVOKER = createPermissionInvoker();
	private static final Set<UUID> HIDDEN_PLAYERS = ConcurrentHashMap.newKeySet();
	private static final ThreadLocal<Boolean> MASK_SUPPRESSOR = ThreadLocal.withInitial(() -> false);
	private static int syncTicker;

	private PlayerVisibility() {
	}

	public static boolean shouldHide(ServerPlayerEntity player) {
		if (player == null) {
			return false;
		}

		if (isOperator(player)) {
			return true;
		}

		return PERMISSION_INVOKER.hasPermission(player, PERMISSION_NODE);
	}

	public static boolean canViewerBypass(ServerPlayerEntity viewer) {
		if (viewer == null) {
			return false;
		}

		if (isOperator(viewer)) {
			return true;
		}

		return PERMISSION_INVOKER.hasPermission(viewer, PERMISSION_NODE);
	}

	public static boolean isHidden(UUID playerId) {
		return HIDDEN_PLAYERS.contains(playerId);
	}

	public static void refreshTracking(ServerPlayerEntity player) {
		boolean shouldHide = shouldHide(player);
		boolean wasHidden = HIDDEN_PLAYERS.contains(player.getUuid());
		Main.LOGGER.debug("hide-me: refresh {} - shouldHide={}, wasHidden={}", player.getName().getString(), shouldHide, wasHidden);

		if (shouldHide == wasHidden) {
			return;
		}

		if (shouldHide) {
			HIDDEN_PLAYERS.add(player.getUuid());
		} else {
			HIDDEN_PLAYERS.remove(player.getUuid());
		}

		broadcastTabUpdate(player);
	}

	public static void removeTracking(UUID playerId) {
		HIDDEN_PLAYERS.remove(playerId);
	}

	public static void sendCurrentHiddenStateTo(ServerPlayerEntity viewer) {
		MinecraftServer server = viewer.getCommandSource().getServer();
		if (server == null || HIDDEN_PLAYERS.isEmpty()) {
			return;
		}

		for (UUID hiddenId : HIDDEN_PLAYERS) {
			ServerPlayerEntity hiddenPlayer = server.getPlayerManager().getPlayer(hiddenId);
			if (hiddenPlayer != null) {
				dispatch(viewer, hiddenPlayer);
			}
		}
	}

	private static void broadcastTabUpdate(ServerPlayerEntity player) {
		MinecraftServer server = player.getCommandSource().getServer();
		if (server == null) {
			return;
		}

		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			dispatch(viewer, player);
		}
	}

	private static void dispatch(ServerPlayerEntity viewer, ServerPlayerEntity target) {
		if (viewer == null) {
			return;
		}

		boolean hidden = isHidden(target.getUuid());
		boolean bypass = canViewerBypass(viewer);
		Main.LOGGER.debug("hide-me: dispatch target={} viewer={} hidden={} bypass={}", target.getName().getString(), viewer.getName().getString(), hidden, bypass);
		if (!hidden || bypass || viewer == target) {
			sendVisibleUpdate(viewer, target);
		} else {
			sendHiddenUpdate(viewer, target);
		}
	}

	private static void sendHiddenUpdate(ServerPlayerEntity viewer, ServerPlayerEntity target) {
		Main.LOGGER.debug("hide-me: sendHiddenUpdate viewer={} target={}", viewer.getName().getString(), target.getUuid());
		runWithMaskSuppressed(() -> {
			PlayerListS2CPacket addPacket = PlayerListS2CPacket.entryFromPlayer(Collections.singleton(target));
			PlayerListPacketAccess addAccess = (PlayerListPacketAccess) addPacket;
			List<PlayerListS2CPacket.Entry> addAdjusted = new ArrayList<>(addAccess.hideMe$getEntries().size());
			for (PlayerListS2CPacket.Entry entry : addAccess.hideMe$getEntries()) {
				addAdjusted.add(asUnlisted(entry));
			}
			addAccess.hideMe$setEntries(addAdjusted);
			viewer.networkHandler.sendPacket(addPacket);

			PlayerListS2CPacket hidePacket = new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED), Collections.singleton(target));
			PlayerListPacketAccess hideAccess = (PlayerListPacketAccess) hidePacket;
			List<PlayerListS2CPacket.Entry> hideAdjusted = new ArrayList<>(hideAccess.hideMe$getEntries().size());
			for (PlayerListS2CPacket.Entry entry : hideAccess.hideMe$getEntries()) {
				hideAdjusted.add(asUnlisted(entry));
			}
			hideAccess.hideMe$setEntries(hideAdjusted);
			viewer.networkHandler.sendPacket(hidePacket);
		});
	}

	private static void sendVisibleUpdate(ServerPlayerEntity viewer, ServerPlayerEntity target) {
		Main.LOGGER.debug("hide-me: sendVisibleUpdate viewer={} target={}", viewer.getName().getString(), target.getUuid());
		PlayerListS2CPacket packet = PlayerListS2CPacket.entryFromPlayer(Collections.singleton(target));
		PlayerListPacketAccess access = (PlayerListPacketAccess) packet;
		List<PlayerListS2CPacket.Entry> adjusted = new ArrayList<>(access.hideMe$getEntries().size());
		for (PlayerListS2CPacket.Entry entry : access.hideMe$getEntries()) {
			adjusted.add(asListed(entry));
		}
		access.hideMe$setEntries(adjusted);
		viewer.networkHandler.sendPacket(packet);
	}

	private static boolean isOperator(ServerPlayerEntity player) {
		if (player.hasPermissionLevel(OP_LEVEL_THRESHOLD)) {
			return true;
		}

		MinecraftServer server = player.getCommandSource().getServer();
		if (server == null) {
			return false;
		}

		PlayerManager manager = server.getPlayerManager();
		if (manager == null) {
			return false;
		}

		return manager.isOperator(new PlayerConfigEntry(player.getGameProfile()));
	}

	public static List<PlayerListS2CPacket.Entry> maskPacketForViewer(PlayerListS2CPacket packet, ServerPlayerEntity viewer) {
		if (isMaskSuppressed() || packet == null || viewer == null || HIDDEN_PLAYERS.isEmpty()) {
			return null;
		}

		PlayerListPacketAccess access = (PlayerListPacketAccess) packet;
		if (canViewerBypass(viewer)) {
			return null;
		}

		List<PlayerListS2CPacket.Entry> source = access.hideMe$getEntries();
		boolean modified = false;
		List<PlayerListS2CPacket.Entry> adjusted = new ArrayList<>(source.size());
		for (PlayerListS2CPacket.Entry entry : source) {
			if (isHidden(entry.profileId())) {
				Main.LOGGER.debug("hide-me: forcing unlisted {} in packet {} for viewer {}", entry.profileId(), packet.getActions(), viewer.getName().getString());
				adjusted.add(asUnlisted(entry));
				modified = true;
			} else {
				adjusted.add(entry);
			}
		}

		if (!modified) {
			return null;
		}

		List<PlayerListS2CPacket.Entry> snapshot = new ArrayList<>(source);
		access.hideMe$setEntries(adjusted);
		return snapshot;
	}

	private static void runWithMaskSuppressed(Runnable action) {
		boolean previous = MASK_SUPPRESSOR.get();
		MASK_SUPPRESSOR.set(true);
		try {
			action.run();
		} finally {
			MASK_SUPPRESSOR.set(previous);
		}
	}

	private static boolean isMaskSuppressed() {
		return MASK_SUPPRESSOR.get();
	}
	public static void restorePacketEntries(PlayerListS2CPacket packet, List<PlayerListS2CPacket.Entry> original) {
		if (packet == null || original == null) {
			return;
		}

		PlayerListPacketAccess access = (PlayerListPacketAccess) packet;
		access.hideMe$setEntries(original);
	}

	public static void schedulePostJoinSync(MinecraftServer server, ServerPlayerEntity viewer) {
		if (server == null || viewer == null) {
			return;
		}

		server.execute(() -> {
			if (server.getPlayerManager().getPlayer(viewer.getUuid()) != viewer) {
				return;
			}
			sendCurrentHiddenStateTo(viewer);
		});
	}

	public static void tick(MinecraftServer server) {
		if (server == null || server.getPlayerManager() == null) {
			return;
		}

		if (++syncTicker % 20 != 0) {
			return;
		}

		for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
			sendCurrentHiddenStateTo(viewer);
		}
	}

	private static PlayerListS2CPacket.Entry asUnlisted(PlayerListS2CPacket.Entry entry) {
		return new PlayerListS2CPacket.Entry(
			entry.profileId(),
			entry.profile(),
			false,
			entry.latency(),
			entry.gameMode(),
			entry.displayName(),
			entry.showHat(),
			entry.listOrder(),
			entry.chatSession()
		);
	}

	private static PlayerListS2CPacket.Entry asListed(PlayerListS2CPacket.Entry entry) {
		return new PlayerListS2CPacket.Entry(
			entry.profileId(),
			entry.profile(),
			true,
			entry.latency(),
			entry.gameMode(),
			entry.displayName(),
			entry.showHat(),
			entry.listOrder(),
			entry.chatSession()
		);
	}

	private static PermissionInvoker createPermissionInvoker() {
		try {
			Class<?> clazz = Class.forName("net.fabricmc.fabric.api.permission.v0.Permissions");
			Method method = findCheckMethod(clazz);
			if (method != null) {
				method.setAccessible(true);
				Class<?> subjectType = method.getParameterTypes()[0];

				if (subjectType.isAssignableFrom(ServerPlayerEntity.class)) {
					Main.LOGGER.info("Fabric permissions API detected (direct player support).");
					return (player, permission) -> invokePermission(method, player, permission);
				}

				if (subjectType.isAssignableFrom(ServerCommandSource.class)) {
					Main.LOGGER.info("Fabric permissions API detected (command source support).");
					return (player, permission) -> invokePermission(method, player.getCommandSource(), permission);
				}

				Main.LOGGER.warn("Fabric permissions API detected but unsupported subject type '{}'", subjectType.getName());
			} else {
				Main.LOGGER.warn("Fabric permissions API detected but no compatible check method was found.");
			}
		} catch (ClassNotFoundException ignored) {
			Main.LOGGER.info("Fabric permissions API not present. Using operator level as the only hide condition.");
		}

		return (player, permission) -> false;
	}

	private static Method findCheckMethod(Class<?> clazz) {
		for (Method method : clazz.getMethods()) {
			if (!"check".equals(method.getName())) {
				continue;
			}

			Class<?>[] params = method.getParameterTypes();
			if (params.length == 3 && params[1] == String.class && params[2] == boolean.class) {
				return method;
			}
		}

		return null;
	}

	private static boolean invokePermission(Method method, Object subject, String permission) {
		try {
			Object result = method.invoke(null, subject, permission, Boolean.FALSE);
			return Boolean.TRUE.equals(result);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			Main.LOGGER.debug("Failed to query fabric-permissions-api; defaulting to operator status.", exception);
			return false;
		}
	}

	@FunctionalInterface
	private interface PermissionInvoker {
		boolean hasPermission(ServerPlayerEntity player, String permission);
	}
}
