package com.programmerdan.minecraft.mcinfinity.listeners;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.manager.PlayerLocationManager;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer;

public class PlayerListener implements Listener {
	
	private MCInfinity plugin;
	
	public PlayerListener(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void playerRespawnEvent(PlayerRespawnEvent spawn) {
		PlayerLocationManager manager = plugin.getPlayerLocationManager();
		if (spawn.isBedSpawn() == true) {
			if (manager.isInSpawnLayer(spawn.getRespawnLocation())) {
				return; // let it go through.
			} else {
				// issue!
				Player player = spawn.getPlayer();
				player.setBedSpawnLocation(null);
				player.sendMessage(ChatColor.RED + "Your bed has been lost, you cannot return there!");
			}
		}
	
		spawn.setRespawnLocation(manager.generateSafeRespawn());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void playerSpawnEvent(PlayerSpawnLocationEvent event) {
		if (event.getPlayer().hasPlayedBefore()) return;
		
		PlayerLocationManager manager = plugin.getPlayerLocationManager();
		if (manager.isInSpawnLayer(event.getSpawnLocation())) {
			return;
		} else {
			event.setSpawnLocation(manager.generateSafeRespawn());
		}
	}
	
	public void shutdown() {
		
	}
}
