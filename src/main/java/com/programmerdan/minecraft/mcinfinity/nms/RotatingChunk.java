package com.programmerdan.minecraft.mcinfinity.nms;

import java.lang.reflect.Field;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;

import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkSnapshot;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_12_R1.World;

public class RotatingChunk {
	
	private Chunk origin;
	private int rotation;
	
	public RotatingChunk(Chunk origin, int rotation) {
		if (rotation % 90 != 0) {
			throw new IllegalArgumentException("Rotation must be a multiple of 90 degrees");
		}
		this.origin = origin;
		this.rotation = rotation;
	}
	
	public PacketPlayOutMapChunk rotate(int i) {
		ChunkSnapshot snapshot = new ChunkSnapshot();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					snapshot.a(rotX(x, z), y, rotZ(x, z), origin.a(x, y, z));
				}
			}
		}
		Chunk rotChunk = new Chunk(origin.getWorld(), snapshot, origin.locX, origin.locZ);
		
		PacketPlayOutMapChunk newOut = new PacketPlayOutMapChunk(rotChunk, i);
		
		return newOut;
	}
	
	public PacketPlayOutMapChunk clearChunk(int i) {
		ChunkSnapshot snapshot = new ChunkSnapshot();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					snapshot.a(x,y,z, Blocks.BARRIER.getBlockData());
				}
			}
		}
		Chunk rotChunk = new Chunk(origin.getWorld(), snapshot, origin.locX, origin.locZ);
		
		PacketPlayOutMapChunk newOut = new PacketPlayOutMapChunk(rotChunk, i);
		
		return newOut;		
	}
	
	public int rotX(int x, int z) {
		if (rotation == -90 || rotation == 270) {
			// x becomes z, z becomes 16-x
			return z;
		} else if (rotation == 90) {
			// x becomes 16-z, z becomes x
			return 15-z;
		} else if (rotation == 180 || rotation == -180) {
			// x becomes 16-x, z becomes 16-z
			return 15-x;
		}
		
		return x;
	}
	public int rotZ(int x, int z) {
		if (rotation == -90 || rotation == 270) {
			// x becomes z, z becomes 16-x
			return 15-x;
		} else if (rotation == 90) {
			// x becomes 16-z, z becomes x
			return x;
		} else if (rotation == 180 || rotation == -180) {
			return 15-z;
		}
		
		return z;
	}
}
