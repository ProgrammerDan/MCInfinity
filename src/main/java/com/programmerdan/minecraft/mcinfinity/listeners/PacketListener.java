/**
 * Portions Copyright (C) 2011-2014 lishid, used under GNU GPL v3 terms
 * 
 * All other portions Copyright (C) 2017 ProgrammerDan, under BSD 3 term
 */
package com.programmerdan.minecraft.mcinfinity.listeners;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
/*import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.chunkmap.ChunkData;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.obfuscation.Calculations;*/
import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.manager.PlayerLocationManager;
import com.programmerdan.minecraft.mcinfinity.model.ChunkCoord;
import com.programmerdan.minecraft.mcinfinity.model.RotatingChunkCoord;
import com.programmerdan.minecraft.mcinfinity.model.MCILayer.Zone;
import com.programmerdan.minecraft.mcinfinity.nms.RotatingChunk;

import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkProviderServer;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_12_R1.PacketPlayOutUnloadChunk;
import net.minecraft.server.v1_12_R1.PlayerChunk;
import net.minecraft.server.v1_12_R1.PlayerChunkMap;
import net.minecraft.server.v1_12_R1.WorldServer;

public class PacketListener {
	
	private MCInfinity plugin;
    private ProtocolManager manager;
    private int viewDistance = Bukkit.getServer().getViewDistance();

	public PacketListener(FileConfiguration config) {
		this.plugin = MCInfinity.getPlugin();
        this.manager = ProtocolLibrary.getProtocolManager();
        
        this.manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.UNLOAD_CHUNK) {
        	@Override
        	public void onPacketSending(PacketEvent event) {
        		PacketContainer packet = event.getPacket();
        		StructureModifier<Integer> ints = packet.getIntegers();
        		
        		int chunkX = ints.read(0);
        		int chunkZ = ints.read(1);
        		
        		PlayerLocationManager locationManager = ( (MCInfinity) plugin).getPlayerLocationManager();

        		Player player = event.getPlayer();
        		
        		RotatingChunkCoord toSend = locationManager.transformChunk(player, chunkX, chunkZ);
                if (toSend == null) {
                	locationManager.clearNormalChunk(player, chunkX, chunkZ);
                	return; // no manip
                }
                
                ((MCInfinity) plugin).info("Player in {4} {5}: Informing player to unload chunk {0} {1} which we mapped to {2} {3}", 
                		chunkX, chunkZ, toSend.x, toSend.z, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
                locationManager.clearTransformChunk(player, chunkX, chunkZ);
        	}
        });

        this.manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
            	PacketContainer packet = event.getPacket();
            	
            	StructureModifier<Integer> ints = packet.getIntegers();
                StructureModifier<byte[]> byteArray = packet.getByteArrays();
                StructureModifier<Boolean> bools = packet.getBooleans();
            	List list = packet.getSpecificModifier(List.class).read(0);
            	List<NbtCompound> result = new ArrayList<NbtCompound>();
            	for(Object tag : list) {
            		result.add(NbtFactory.fromNMSCompound(tag));
            	}
            	
            	PlayerLocationManager locationManager = ( (MCInfinity) plugin).getPlayerLocationManager();
                
                int chunkX = ints.read(0);
                int chunkZ = ints.read(1);
                
                Player player = event.getPlayer();
                
                RotatingChunkCoord toSend = locationManager.transformChunk(player, chunkX, chunkZ);
                if (toSend == null) {
                	locationManager.addNormalChunk(player, chunkX, chunkZ);
                	return; // no manip
                }
                
                locationManager.addTransformChunk(player, chunkX, chunkZ);
                
                if (toSend.equals(RotatingChunkCoord.EmptyChunk)) { // clear this data.
                    ((MCInfinity) plugin).info("Player in {2} {3}: Suppressing chunk {0} {1} transmission as its out of the world", chunkX, chunkZ,
                    		player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
                } else {
	                ((MCInfinity) plugin).info("Player in {5} {6}: Replacing chunk {0} {1} transmission with chunk {2} {3} rotated {4} degrees", chunkX, chunkZ,
	                		toSend.x, toSend.z, toSend.rotation, player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
                }
                
        		WorldServer worldServer = ((CraftWorld)locationManager.getPlayerWorld(player)).getHandle();
        		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();
        		
        		Chunk chunk = chunkProviderServer.getOrLoadChunkAt(toSend.x, toSend.z);
        		
        		RotatingChunk rotChunk = new RotatingChunk(chunk, toSend.rotation);
        		
        		/*ChunkData chunkData = new ChunkData();
        		chunkData.chunkX = ints.read(0);
        		chunkData.chunkZ = ints.read(1);
        		chunkData.groundUpContinuous = bools.read(0);
        		chunkData.primaryBitMask = ints.read(2);
        		chunkData.data = byteArray.read(0);
        		chunkData.isOverworld = event.getPlayer().getWorld().getEnvironment() == World.Environment.NORMAL;
        		chunkData.blockEntities = getBlockEntities(packet, event.getPlayer());
                
				try {
					byte[] newData = Calculations.obfuscateOrUseCache(chunkData, event.getPlayer());
					
					if(newData != null) {
						byteArray.write(0, newData);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}*/
        		try {
        			PacketPlayOutMapChunk mapPacket = toSend.equals(RotatingChunkCoord.EmptyChunk) ? 
        					rotChunk.clearChunk(ints.read(2)) : rotChunk.rotate(ints.read(2));
        			byte[] newData = null;
        			try {
        				Field dField = PacketPlayOutMapChunk.class.getDeclaredField("d"); // byte[] array
        			
        				dField.setAccessible(true);
        				
        				newData = (byte[]) dField.get(mapPacket);
        			}catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
        				MCInfinity.getPlugin().debug("Failed to remap chunk {0}", chunk);
        			}
        			
        			//byte[] newData = rotChunk.rotate(ints.read(2));
        			
        			//PacketContainer newChunk = new PacketContainer(PacketType.Play.Server.MAP_CHUNK, mapPacket);
        			//newChunk.getIntegers().write(0, chunkX)
        			//		.write(1, chunkZ);
        			/*
        					.write(2, ints.read(2))
        					.write(3, newData.length)
        					.write(4, arg1)
        			*/
	        		if (newData != null) {
	        			byteArray.write(0, newData);
	        		}
        			//event.setPacket(newChunk);
        			
        		} catch (Exception e) {
        			((MCInfinity) plugin).debug("Failed to send rotated chunk {0}, {1}, {2}", toSend.x, toSend.z, toSend.rotation);
        			((MCInfinity) plugin).warning("Failure: ", e);
        		}
            }
        });

	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Rapid fire unload all transformed chunks. 
	 * 
	 * My hope is will trigger a request to reload
	 * 
	 * @param player
	 */
	public void registerChunkRefresh(Player player, Zone zone) {
		
		MCInfinity.getPlugin().debug("Player in {1} {2}: Forcing dump of chunks that we know we have messed up to {0}", player.getName(),
				player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
		
		
		Set<ChunkCoord> resend = ( (MCInfinity) plugin).getPlayerLocationManager().getTransformChunks(player);
		
		for (ChunkCoord chunk : resend) {
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutUnloadChunk(chunk.x, chunk.z));
		}

		PlayerLocationManager locationManager = ( (MCInfinity) plugin).getPlayerLocationManager();
		World world = locationManager.getPlayerWorld(player);
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

		long ticks = 0l;
		for (ChunkCoord chunk : resend) {
			if (chunkProviderServer.isLoaded(chunk.x, chunk.z)) {
				Chunk toSend = chunkProviderServer.getChunkAt(chunk.x, chunk.z);
				((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutMapChunk(toSend, 0xffff));
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						triggerChunkRefresh(chunkProviderServer, player, chunk, true, null);
					}
				}, ticks++);
			}
		}

		if (zone != null) { // have a zone, update "WB" effects
			// TODO: likely remove this at some point ... OK for now
			switch(zone) {
			case BACK:
				break;
			case BOTTOM:
				break;
			case FRONT:
				break;
			case LEFT:
				
				
				break;
			case RIGHT:
				break;
			case TOP:
				break;
			case UNCLEAR:
				break;
			default:
				break;
			
			}
		}
	}

	public void triggerEdgeUpdate(Player player, Zone zone) {
		Set<ChunkCoord> resend = plugin.getPlayerLocationManager().getTransformChunks(player);
		
		PlayerLocationManager locationManager = plugin.getPlayerLocationManager();
		World world = locationManager.getPlayerWorld(player);
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

		long ticks = 0l;
		for (ChunkCoord chunk : resend) {
			if (chunkProviderServer.isLoaded(chunk.x, chunk.z)) {
                RotatingChunkCoord toSend = locationManager.transformChunk(player, chunk.x, chunk.z);
                if (toSend == null) {
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							triggerChunkRefresh(chunkProviderServer, player, chunk, false, null);
						}
					}, ticks++);
                } else if (toSend.equals(RotatingChunkCoord.EmptyChunk)) { // clear this data.
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							triggerChunkRefresh(chunkProviderServer, player, chunk, true, null);
						}
					}, ticks++);
                } else {
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							triggerChunkRefresh(chunkProviderServer, player, chunk, true, toSend);
						}
					}, ticks++);
                }
			}
		}
	}
	
	/**
	 * This is NOT checked to see if chunk is loaded, that is responsibility of caller!
	 * 
	 * @param player the Player to send the update to
	 * @param chunk The chunk to force an update for
	 * @param fake to use real data? or fake data?
	 * @param rotChunk if fake is true, and this is supplied, sends data from this rotChunk rather then the "real" chunk.
	 * @return true if successful, false otherwise.
	 */
	public boolean triggerChunkRefresh(ChunkProviderServer chunks, Player player, ChunkCoord chunk, boolean fake, RotatingChunkCoord rotChunk) {
		// First we send bogus data, then we send valid / or different / bogus data.
		PacketContainer bulkupdate = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
		bulkupdate.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunk.x, chunk.z));
		StructureModifier<MultiBlockChangeInfo[]> blockdata = bulkupdate.getMultiBlockChangeInfoArrays();
		MultiBlockChangeInfo[] datas = new MultiBlockChangeInfo[4];

		int i = 0;
		for (short y = 4; y < 256; y+=64) {
			short loc = (short) (8 << 12 | 8 << 8 | y);
			WrappedBlockData wbd = new WrappedBlockData(Blocks.AIR.getBlockData());
			datas[i++] = new MultiBlockChangeInfo( loc, wbd, new ChunkCoordIntPair(chunk.x, chunk.z));
		}
		blockdata.write(0, datas);
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, bulkupdate);
		} catch (InvocationTargetException e) {
			plugin.warning("Failed to force a chunk update via ficticious block updates", e);
			return false;
		}

		// Second we send "real" or real bogus data.
		
		bulkupdate = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
		bulkupdate.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunk.x, chunk.z));
		blockdata = bulkupdate.getMultiBlockChangeInfoArrays();
		datas = new MultiBlockChangeInfo[4];

		i = 0;
		for (short y = 4; y < 256; y+=64) {
			short loc = (short) (8 << 12 | 8 << 8 | y);
			WrappedBlockData wbd;
			if (fake && rotChunk == null) {
				wbd = new WrappedBlockData(Blocks.BARRIER.getBlockData());
			} else if (fake) {
				Chunk rotateChunk = chunks.getOrLoadChunkAt(rotChunk.x, rotChunk.z);
				if (rotateChunk != null) {
					RotatingChunk rChunk = new RotatingChunk(rotateChunk, rotChunk.rotation);
					wbd = new WrappedBlockData(rotateChunk.a(rChunk.rotX(8, 8), y, rChunk.rotZ(8, 8)));
				} else {
					plugin.warning("Failed to force a chunk load to send a valid but extraneous block update");
					return false;
				}
			} else {
				Chunk nChunk = chunks.getChunkAt(chunk.x, chunk.z);
				wbd = new WrappedBlockData(nChunk.a(8,y,8));
			}
			datas[i++] = new MultiBlockChangeInfo( loc, wbd, new ChunkCoordIntPair(chunk.x, chunk.z));
		}
		blockdata.write(0, datas);
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, bulkupdate);
			return true;
		} catch (InvocationTargetException e) {
			plugin.warning("Failed to force a chunk update via ficticious block updates", e);
			return false;
		}
	}

/*    @SuppressWarnings("rawtypes")
	private static List<NbtCompound> getBlockEntities(PacketContainer packet, Player player) {
    	WorldConfig worldConfig = Orebfuscator.configManager.getWorld(player.getWorld());
    	
    	if(!worldConfig.isBypassObfuscationForSignsWithText()) {
    		return null;
    	}
    	
    	List list = packet.getSpecificModifier(List.class).read(0);
    	List<NbtCompound> result = new ArrayList<NbtCompound>();
    	
    	for(Object tag : list) {
    		result.add(NbtFactory.fromNMSCompound(tag));
    	}
    	
    	return result;
    }
	*/
}
