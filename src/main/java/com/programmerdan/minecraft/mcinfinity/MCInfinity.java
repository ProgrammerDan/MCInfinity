package com.programmerdan.minecraft.mcinfinity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.ImmutableList;
import com.programmerdan.minecraft.mcinfinity.commands.CommandHandler;
import com.programmerdan.minecraft.mcinfinity.listeners.PlayerListener;
import com.programmerdan.minecraft.mcinfinity.listeners.EntityListener;
import com.programmerdan.minecraft.mcinfinity.manager.PlayerLocationManager;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer;
import com.programmerdan.minecraft.mcinfinity.model.MCIWorld;
import com.programmerdan.minecraft.mcinfinity.util.RandomProvider;

public class MCInfinity extends JavaPlugin {
	private static MCInfinity instance;
	private CommandHandler commandHandler;
	private PlayerListener playerListener;
	private PlayerLocationManager playerLocationManager;
	private EntityListener entityListener;
	
	private Map<String, MCIWorld> worlds;
	private List<MCILayer> spawnLayers;
	
	@Override
	public void onEnable() {
		super.onEnable();

		saveDefaultConfig();
		reloadConfig();
		
		MCInfinity.instance = this;
		
		this.worlds = new HashMap<String, MCIWorld>();
		this.spawnLayers = new ArrayList<MCILayer>();
		
		process(getConfig());

		registerPlayerLocationManager();
		registerPlayerListener();
		registerEntityListener();
		registerCommandHandler();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		if (this.playerLocationManager != null) this.playerLocationManager.shutdown();
		if (this.playerListener != null) this.playerListener.shutdown();
		if (this.entityListener != null) this.entityListener.shutdown();
	}
	
	public MCILayer getRandomSpawnLayer() {
		if (spawnLayers.size() == 1) {
			return spawnLayers.get(0);
		} else {
			return spawnLayers.get(RandomProvider.random(0, spawnLayers.size()));
		}
	}
	
	public List<MCILayer> getSpawnLayers() { 
		return ImmutableList.copyOf(this.spawnLayers);
	}
	
	public List<MCIWorld> getWorlds() {
		return ImmutableList.copyOf(this.worlds.values());
	}

	public PlayerLocationManager getPlayerLocationManager() {
		return this.playerLocationManager;
	}
	
	public PlayerListener getPlayerListener() {
		return this.playerListener;
	}
	
	public EntityListener getEntityListener() {
		return this.entityListener;
	}
	
	public CommandHandler getCommandHandler() {
		return this.commandHandler;
	}
	
	private void registerPlayerLocationManager() {
		if (!this.isEnabled()) return;
		try {
			this.playerLocationManager = new PlayerLocationManager(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up player universe location handling", e);
			this.setEnabled(false);
		}
	}
	
	private void registerCommandHandler() {
		if (!this.isEnabled()) return;
		try {
			this.commandHandler = new CommandHandler(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up command handling", e);
			this.setEnabled(false);
		}
	}

	private void registerPlayerListener() {
		if (!this.isEnabled()) return;
		try {
			this.playerListener = new PlayerListener(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up player event capture / handling", e);
			this.setEnabled(false);
		}	
	}
	
	private void registerEntityListener() {
		if (!this.isEnabled()) return;
		try {
			this.entityListener = new EntityListener(getConfig());
		} catch (Exception e) {
			this.severe("Failed to set up entity event capture / handling", e);
			this.setEnabled(false);
		}	
	}
	
	/**
	 * This handles demuxxing the config into useable world configurations.
	 * Basically does all the backend math and preconfiguration so that
	 * transitions between zones and layers will be as seamless as MC lets us.
	 * 
	 * @param config
	 */
	private void process(FileConfiguration config) {
		int edge = config.getInt("edge", -1);
		if (edge < 1) {
			this.severe("Failed to set up sensible zone edge size! Be sure to set a meaningful `edge` value in the root config.");
			this.setEnabled(false);
			return;
		}
		ConfigurationSection worlds = config.getConfigurationSection("worlds");
		if (worlds == null) {
			this.severe("No worlds are defined! Check the example config, and add a `worlds` section to your root config.");
			this.setEnabled(false);
			return;
		}
		for (String world : worlds.getKeys(false)) {
			ConfigurationSection worldConfig = worlds.getConfigurationSection(world);
			if (worldConfig == null) continue;
			
			long distance = worldConfig.getLong("distance-from-sun", -1);
			if (distance < 1) {
				this.warning("Failed to configure world {}, distance from sun invalid- add a `distance-from-sun` value in your world config.", world);
				continue;
			}
			
			ConfigurationSection layers = worldConfig.getConfigurationSection("layers");
			if (layers == null) {
				this.warning("Failed to configure world {}, no layers defined- add a `layers` section to your world config.", world);
				continue;
			}
			
			MCIWorld mciWorld = new MCIWorld(world, distance);
			boolean error = false;
			for (String layer : layers.getKeys(false)) {
				ConfigurationSection layerConfig = layers.getConfigurationSection(layer);
				if (layerConfig == null) continue;
				
				boolean spawn = layerConfig.getBoolean("spawn", false); // default false.
				boolean launch = layerConfig.getBoolean("launch", false); // default false.
				
				String mcWorld = layerConfig.getString("world");
				if (mcWorld == null) {
					this.warning("Failed to configure world {}, layer {} defined without a world. Be sure each layer has a `world` defined.", world, layer);
					error = true;
					break;
				}
				
				ConfigurationSection connectConfig = layerConfig.getConfigurationSection("connect");
				if (connectConfig == null) {
					this.info("World {}, layer {} has no external connections. This might not have been your intention?", world, layer);
				}
				
				String top = connectConfig.getString("top");
				String bottom = connectConfig.getString("bottom");
				
				MCILayer mciLayer = new MCILayer(layer, mciWorld, edge, spawn, launch, mcWorld, top, bottom);
				mciWorld.addLayer(mciLayer);
			}
			if (!error) {
				if (mciWorld.compile()) {
					this.info("Configured world {} for use!", world);
					this.worlds.put(world, mciWorld);
					this.spawnLayers.addAll(mciWorld.getSpawnLayers());
				} else {
					this.warning("Failed to prepare world {} for real use, disabled!", world);
				}
			}
		}
	}

	/**
	 * 
	 * @return the static global instance. Not my fav pattern, but whatever.
	 */
	public static MCInfinity getPlugin() {
		return MCInfinity.instance;
	}

	/**
	 * Simple SEVERE level logging.
	 */
	public void severe(String message) {
		getLogger().log(Level.SEVERE, message);
	}

	/**
	 * Simple SEVERE level logging with Throwable record.
	 */
	public void severe(String message, Throwable error) {
		getLogger().log(Level.SEVERE, message, error);
	}

	/**
	 * Simple WARNING level logging.
	 */
	public void warning(String message) {
		getLogger().log(Level.WARNING, message);
	}

	/**
	 * Simple WARNING level logging with Throwable record.
	 */
	public void warning(String message, Throwable error) {
		getLogger().log(Level.WARNING, message, error);
	}

	/**
	 * Simple WARNING level logging with ellipsis notation shortcut for deferred injection argument array.
	 */
	public void warning(String message, Object... vars) {
		getLogger().log(Level.WARNING, message, vars);
	}

	/**
	 * Simple INFO level logging
	 */
	public void info(String message) {
		getLogger().log(Level.INFO, message);
	}

	/**
	 * Simple INFO level logging with ellipsis notation shortcut for deferred injection argument array.
	 */
	public void info(String message, Object... vars) {
		getLogger().log(Level.INFO, message, vars);
	}
	
	/**
	 * Toggle debug live
	 * 
	 * @param state true for on, false for off.
	 */
	public void setDebug(boolean state) {
		if (state) {
			getConfig().set("debug", true);
		} else {
			getConfig().set("debug", false);
		}
	}

	/**
	 * Live on/off debug message at INFO level.
	 *
	 * Skipped if `debug` in root config is false.
	 */
	public void debug(String message) {
		if (getConfig() != null && getConfig().getBoolean("debug", false)) {
			getLogger().log(Level.INFO, message);
		}
	}

	/**
	 * Live on/off debug message  at INFO level with ellipsis notation
	 * shortcut for deferred injection argument array.
	 *
	 * Skipped if `debug` in root config is false.
	 */
	public void debug(String message, Object... vars) {
		if (getConfig() != null && getConfig().getBoolean("debug", false)) {
			getLogger().log(Level.INFO, message, vars);
		}
	}

}
