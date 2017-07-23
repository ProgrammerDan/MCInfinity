package com.programmerdan.minecraft.mcinfinity.model;

public class RotatingChunkCoord {

	public int x;
	public int z;
	public int rotation;
	
	public RotatingChunkCoord(int x, int z, int rot) {
		if (rot % 90 != 0) {
			throw new IllegalArgumentException("Cannot chunks rotate on an angle not divisible by 90");
		}
		this.x = x;
		this.z = z;
		this.rotation = rot;
	}

}
