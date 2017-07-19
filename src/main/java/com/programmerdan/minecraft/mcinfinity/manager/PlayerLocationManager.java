package com.programmerdan.minecraft.mcinfinity.manager;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer;
import com.programmerdan.minecraft.mcinfinity.model.MCIWorld;

public class PlayerLocationManager {
	
	private MCInfinity plugin;
	
	List<MCILayer> spawnLayers;
	List<MCIWorld> worlds;
	
	public PlayerLocationManager(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
	}
	
	public void ready() {
		spawnLayers = plugin.getSpawnLayers();
		worlds = plugin.getWorlds();
	}
	
	public void shutdown() {
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
	
}
