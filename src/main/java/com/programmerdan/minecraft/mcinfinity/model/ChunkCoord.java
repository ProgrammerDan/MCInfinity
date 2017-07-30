package com.programmerdan.minecraft.mcinfinity.model;

public class ChunkCoord implements Comparable<ChunkCoord>{

	public int x;
	public int z;
	
	public ChunkCoord(int x, int z) {
		this.x = x;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChunkCoord) {
			ChunkCoord cc = (ChunkCoord) o;
			return cc.x == x && cc.z == z;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 73 + 17 * x + 31 * z;
	}

	@Override
	public int compareTo(ChunkCoord o) {
		return o.x < this.x ? -1 : o.x > this.x ? 1 : o.z < this.z ? -1 : o.z > this.z ? 1 : 0;
	}

}
