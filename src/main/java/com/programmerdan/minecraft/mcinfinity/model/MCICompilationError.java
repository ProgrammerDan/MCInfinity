package com.programmerdan.minecraft.mcinfinity.model;

public class MCICompilationError extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3383717306223756709L;

	private MCIWorld world;
	private MCILayer layer;
	
	public MCICompilationError(MCIWorld world, MCILayer layer, String error) {
		super(error);
		this.world = world;
		this.layer = layer;
	}
	
	public MCIWorld getWorld() {
		return this.world;
	}
	
	public MCILayer getLayer() {
		return this.layer;
	}
}
