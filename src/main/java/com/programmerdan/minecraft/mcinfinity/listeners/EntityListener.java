package com.programmerdan.minecraft.mcinfinity.listeners;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;

public class EntityListener  implements Listener {
	
	private MCInfinity plugin;
	
	public EntityListener(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	
	public void shutdown() {
		
	}
}
