package com.programmerdan.minecraft.mcinfinity.model;

public class RotatingChunkCoord {

	public static RotatingChunkCoord EmptyChunk = new RotatingChunkCoord(-1, - 1, -360);
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

	@Override
	public boolean equals(Object o) {
		if (o instanceof RotatingChunkCoord) {
			RotatingChunkCoord rcc = (RotatingChunkCoord) o;
			return rcc.x == x && rcc.z == z && rcc.rotation == rotation;
		}
		return false;
	}
}
