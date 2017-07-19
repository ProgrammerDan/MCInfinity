package com.programmerdan.minecraft.mcinfinity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;

public class MCIWorld {

	private String worldName;
	private long distanceFromSun;
	
	private Map<String, MCILayer> layers;
	private Map<UUID, MCILayer> layersByWorld;
	
	private boolean valid;
	
	public MCIWorld(String world, long distance) {
		this.worldName = world;
		this.distanceFromSun = distance;
		
		this.valid = false;
	}

	public boolean compile() {
		try {
			for (MCILayer layer : layers.values()) {
				layer.compile();
				layersByWorld.put(layer.getRealWorld().getUID(), layer);
			}
			
			valid = true;
			return true;
		} catch (MCICompilationError mce) {
			MCInfinity.getPlugin().severe("Failed to prepare " + worldName + " for use: ", mce);
		}
		return false;
	}

	public void addLayer(MCILayer mciLayer) {
		layers.put(mciLayer.getName(), mciLayer);
	}
	
	protected MCILayer getLayer(String name) throws MCICompilationError {
		if (layers.containsKey(name)) {
			return layers.get(name);
		} else {
			throw new MCICompilationError(this, null, "Unable to resolve layer " + name + " in world " + worldName);
		}
	}

	/**
	 * Finds the current "layer" that this location inhabiting, or null if not in this world.
	 * 
	 * @param location the 
	 * @return the MCILayer that the location is in, if in this world.
	 */
	public MCILayer getLayer(Location location) {
		if (!valid) return null;
		MCILayer layer = layersByWorld.get(location.getWorld().getUID());
		return layer.inLayer(location) ? layer : null;
	}
	
	/**
	 * Finds and returns a list of all layers in this world that support spawning, or an empty list if none.
	 * 
	 * @return the list of spawn supporting layers.
	 */
	public List<MCILayer> getSpawnLayers() {
		if (!valid) return new ArrayList<>();
		ArrayList<MCILayer> spawns = new ArrayList<>(layers.size());
		for (MCILayer layer : layers.values()) {
			if (layer.isSpawn()) {
				spawns.add(layer);
			}
		}
		return spawns;
	}
}
