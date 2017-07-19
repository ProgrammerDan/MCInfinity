package com.programmerdan.minecraft.mcinfinity.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;

public class CommandHandler implements CommandExecutor, TabCompleter {
	private MCInfinity plugin;
	
	public CommandHandler(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		
		PluginCommand base = plugin.getCommand("mcinfinity");
		base.setExecutor(this);
		base.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return null;
	}
}
