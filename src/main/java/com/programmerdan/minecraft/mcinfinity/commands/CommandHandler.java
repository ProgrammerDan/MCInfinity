package com.programmerdan.minecraft.mcinfinity.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.model.ChunkCoord;

public class CommandHandler implements CommandExecutor, TabCompleter {
	private MCInfinity plugin;
	
	PluginCommand base;
	PluginCommand map;
	
	public CommandHandler(FileConfiguration config) {
		plugin = MCInfinity.getPlugin();
		
		base = plugin.getCommand("mcinfinity");
		base.setExecutor(this);
		base.setTabCompleter(this);
		
		map = plugin.getCommand("mcinfinity.map");
		map.setExecutor(this);
		map.setTabCompleter(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		if (cmd.equals(map)) {
			Player player = null;
			if (args.length == 0) {
				// return for self
				if (sender instanceof Player) {
					player = (Player) sender;
				} else {
					sender.sendMessage("Console must provide a player name");
				}
			} else {
				player = Bukkit.getPlayer(args[0]);
			}
			
			if (player != null) {
				Set<ChunkCoord> normal = plugin.getPlayerLocationManager().getNormalChunks(player);
				Set<ChunkCoord> transform = plugin.getPlayerLocationManager().getTransformChunks(player);
				
				TreeSet<ChunkCoord> arranged = new TreeSet<>();
				int minX = 0;
				int minZ = 0;
				int maxX = 0;
				int maxZ = 0;
				for (ChunkCoord n : normal) {
					arranged.add(n);
					if (n.x > maxX) maxX = n.x;
					if (n.x < minX) minX = n.x;
					if (n.z > maxZ) maxZ = n.z;
					if (n.z < minZ) minZ = n.z;
				}
				
				for (ChunkCoord n : transform) {
					arranged.add(n);
					if (n.x > maxX) maxX = n.x;
					if (n.x < minX) minX = n.x;
					if (n.z > maxZ) maxZ = n.z;
					if (n.z < minZ) minZ = n.z;
				}
				
				int spreadX = maxX - minX + 1;
				int spreadZ = maxZ - minZ + 1;
				if (spreadX > 30 || spreadZ > 30) {
					sender.sendMessage("Too many to send a pretty list. Sorry.");
					int cap = 0;
					StringBuffer sb = new StringBuffer();
					for (ChunkCoord next : arranged) {
						boolean inNormal = normal.contains(next);
						boolean inTransform = transform.contains(next);
						
						sb.append(" ").append(inNormal && inTransform ? ChatColor.DARK_PURPLE : inNormal ? ChatColor.GREEN : ChatColor.RED);
						sb.append(next.x).append(",").append(next.z);
						cap++;
						if (cap > 20) {
							sender.sendMessage(sb.toString());
							cap = 0;
							sb = new StringBuffer();
						}
					}
					sender.sendMessage(sb.toString());
				} else {
					StringBuffer header = new StringBuffer();
					StringBuffer content = new StringBuffer();
					String format = "%s%3s";
					for (int i = minZ; i <= maxZ; i++) {
						if (i == minZ) {
							header.append(String.format(format, ChatColor.RESET, " "));
						}
						content.append(String.format(format, ChatColor.AQUA, Integer.toString(i)));
						for (int j = minX; j <= maxX; j++) {
							if (i == minZ) {
								header.append(String.format(format, ChatColor.AQUA, Integer.toString(j)));
							}
							ChunkCoord test = new ChunkCoord(j,i);
							boolean inNormal = normal.contains(test);
							boolean inTransform = transform.contains(test);
							
							content.append(String.format(format, 
									inNormal && inTransform ? ChatColor.DARK_PURPLE : inNormal ? ChatColor.GREEN : inTransform ? ChatColor.RED : ChatColor.BLACK,
									"[|]"));
						}
						if (i == minZ) {
							sender.sendMessage(header.toString());
						}
						sender.sendMessage(content.toString());
						content = new StringBuffer();
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String arg2, String[] args) {
		if (cmd.equals(map)) {
			if (args.length <= 1){
				String almost = (args.length == 1) ? args[0] : null;
				
				List<String> names = new ArrayList<String>();
				for (Player online : Bukkit.getOnlinePlayers()) {
					if (almost == null || almost.equals("") || online.getName().contains(almost)) {
						names.add(online.getName());
					}
				}
				return names;
			}
		}
		return null;
	}
}
