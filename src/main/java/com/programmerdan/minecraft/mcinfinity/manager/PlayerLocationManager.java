package com.programmerdan.minecraft.mcinfinity.manager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.model.ChunkCoord;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer;
import com.programmerdan.minecraft.mcinfinity.model.MCIWorld;
import com.programmerdan.minecraft.mcinfinity.model.RotatingChunkCoord;

public class PlayerLocationManager {
	
	private MCInfinity plugin;
	
	List<MCILayer> spawnLayers;
	List<MCIWorld> worlds;
	
	Map<UUID, MCIWorld> playerWorldMap;
	ConcurrentMap<UUID, Set<ChunkCoord>> playerTransformChunks;
	
	public PlayerLocationManager(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		playerWorldMap = new ConcurrentHashMap<UUID, MCIWorld>();
		playerTransformChunks = new ConcurrentHashMap<UUID, Set<ChunkCoord>>();
	}
	
	public void ready() {
		spawnLayers = plugin.getSpawnLayers();
		worlds = plugin.getWorlds();
	}
	
	public void shutdown() {
	}
	
	public void updatePlayer(Player player) {
		for (MCIWorld world : worlds) {
			MCILayer layer = world.getLayer(player.getLocation());
			if (layer != null) {
				playerWorldMap.put(player.getUniqueId(), world);
				return;
			}
		}
	}

	/**
	 * Checks if the location is into a valid layer (e.g. allowed based on config)
	 * 
	 * @param respawnLocation the location to check
	 * @return true if layer supports respawn, false otherwise.
	 */
	public boolean isInSpawnLayer(Location respawnLocation) {
		for (MCILayer layer : spawnLayers) {
			if (layer.inLayer(respawnLocation)) return true;
		}
		return false;
	}

	/**
	 * TODO: Better algorithm.
	 * 
	 * @return
	 */
	public Location generateSafeRespawn() {
		int tries = 25;
		while (tries > 0) {
			MCILayer spawnLayer = plugin.getRandomSpawnLayer();
		
			Location location = spawnLayer.randomSafeLocationInLayer();
			if (location != null) {
				return location;
			}
			tries --;
		}
		return null;
	}

	public boolean handlePlayerMovement(Player player, Location prior, Location next) {
		if (!playerWorldMap.containsKey(player.getUniqueId())) {
			updatePlayer(player);
		}
		MCILayer layer = playerWorldMap.get(player.getUniqueId()).getLayer(prior);
		if (layer == null) { // WHERE ARE WE
			plugin.debug("Outside world border at {0}", prior);
			player.eject();
			player.setFlying(false);
			player.setGliding(false);
			player.teleport(generateSafeRespawn());
			return false;
		} else if (layer.inLayer(next)) {
			if (!layer.getZone(prior.getBlockX(), prior.getBlockZ()).equals(layer.getZone(next.getBlockX(), next.getBlockZ()))) {
				Bukkit.getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						if (player != null)
							plugin.getPacketListener().registerChunkRefresh(player);
					}
				});
			}
			return true;
		} else {
			// we've hit a boundary or border
			Location newLocation = layer.moveAtBorder(prior, next);
			Bukkit.getScheduler().runTask(plugin, new Runnable() {
				public void run() {
					player.teleport(newLocation, TeleportCause.PLUGIN);
				}
			});
			return false;
		}
	}
	
	public RotatingChunkCoord transformChunk(Player player, int chunkX, int chunkZ) {
		MCIWorld world = playerWorldMap.get(player.getUniqueId());
		if (world == null) {
			updatePlayer(player);
			world = playerWorldMap.get(player.getUniqueId());
		}
		
		MCILayer layer = world.getLayer(player.getLocation());
		if (layer == null) { // WHERE ARE WE
			plugin.debug("Outside world border at {0}", player.getLocation());
			return null; 
		} else {
			return layer.remapChunk(player.getLocation(), chunkX, chunkZ);
		}
	}
	
	public World getPlayerWorld(Player player) {
		MCIWorld world = playerWorldMap.get(player.getUniqueId());
		if (world == null) {
			updatePlayer(player);
			world = playerWorldMap.get(player.getUniqueId());
		}
		
		MCILayer layer = world.getLayer(player.getLocation());
		return layer.getRealWorld();
	}

	/**
	 * Gets an unmodifiable set representing the transformed chunks at the point in time of this request.
	 * 
	 * @param player
	 * @return a set of ChunkCoord representing managed / transformed chunks visible to the player.
	 */
	public Set<ChunkCoord> getTransformChunks(Player player) {
		return ImmutableSet.copyOf(playerTransformChunks.compute(player.getUniqueId(), (uuid, inset) -> {
			if (inset == null) return Sets.newConcurrentHashSet();
			return inset;
		}));
	}

	/**
	 * Clears a particular chunk from tracking.
	 * 
	 * @param player
	 * @param chunkX
	 * @param chunkZ
	 */
	public void clearTransformChunk(Player player, int chunkX, int chunkZ) {
		playerTransformChunks.computeIfPresent(player.getUniqueId(), (uuid, inset) -> {
			inset.remove(new ChunkCoord(chunkX, chunkZ));
			return inset;
		});
	}

	/**
	 * Adds a particular chunk to tracking.
	 * 
	 * @param player
	 * @param chunkX
	 * @param chunkZ
	 */
	public void addTransformChunk(Player player, int chunkX, int chunkZ) {
		playerTransformChunks.compute(player.getUniqueId(), (uuid, inset) -> {
			if (inset == null) inset = Sets.newConcurrentHashSet();
			inset.add(new ChunkCoord(chunkX, chunkZ));
			return inset;
		});
	}

}
