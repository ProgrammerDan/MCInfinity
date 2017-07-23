/**
 * Portions Copyright (C) 2011-2014 lishid, used under GNU GPL v3 terms
 * 
 * All other portions Copyright (C) 2017 ProgrammerDan, under BSD 3 term
 */
package com.programmerdan.minecraft.mcinfinity.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
/*import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.chunkmap.ChunkData;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.obfuscation.Calculations;*/
import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.manager.PlayerLocationManager;
import com.programmerdan.minecraft.mcinfinity.model.RotatingChunkCoord;

/*import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkProviderServer;
import net.minecraft.server.v1_12_R1.PlayerChunk;
import net.minecraft.server.v1_12_R1.PlayerChunkMap;
import net.minecraft.server.v1_12_R1.WorldServer;*/

public class PacketListener {
	
	private MCInfinity plugin;
    private ProtocolManager manager;

	public PacketListener(FileConfiguration config) {
		this.plugin = MCInfinity.getPlugin();
        this.manager = ProtocolLibrary.getProtocolManager();

        this.manager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
            	PacketContainer packet = event.getPacket();
            	
            	StructureModifier<Integer> ints = packet.getIntegers();
                StructureModifier<byte[]> byteArray = packet.getByteArrays();
                StructureModifier<Boolean> bools = packet.getBooleans();
                
                PlayerLocationManager locationManager = ( (MCInfinity) plugin).getPlayerLocationManager();
                
                int chunkX = ints.read(0);
                int chunkZ = ints.read(1);
                RotatingChunkCoord toSend = locationManager.transformChunk(event.getPlayer(), chunkX, chunkZ);
                if (toSend == null) return; // no manip
                
                ((MCInfinity) plugin).info("Suppressing chunk {0} {1} transmission", chunkX, chunkZ);
                //TODO
                byteArray.write(0, new byte[byteArray.read(0).length]);
                //event.setCancelled(true);
                
        		/*WorldServer worldServer = ((CraftWorld)locationManager.getPlayerWorld(event.getPlayer())).getHandle();
        		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();
        		
        		Chunk chunk = chunkProviderServer.getOrLoadChunkAt(toSend.x, toSend.z);
        		
        		PlayerChunkMap chunkMap = worldServer.getPlayerChunkMap();
        		
        		PlayerChunk playerChunk = chunkMap.getChunk(toSend.x, toSend.z);
        		
        		playerChunk.
        		
        		
                ChunkData chunkData = new ChunkData();
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
            }
        });

	}

	public void shutdown() {
		// TODO Auto-generated method stub
		
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