package com.programmerdan.minecraft.mcinfinity.manager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer;
import com.programmerdan.minecraft.mcinfinity.model.MCIWorld;

public class PlayerLocationManager {
	
	private MCInfinity plugin;
	
	List<MCILayer> spawnLayers;
	List<MCIWorld> worlds;
	
	Map<UUID, MCIWorld> playerWorldMap;
	
	public PlayerLocationManager(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		playerWorldMap = new ConcurrentHashMap<UUID, MCIWorld>();
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
			return false;
		} else if (layer.inLayer(next)) {
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
	
}
