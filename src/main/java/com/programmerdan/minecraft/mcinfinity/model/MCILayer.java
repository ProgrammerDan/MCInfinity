package com.programmerdan.minecraft.mcinfinity.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.programmerdan.minecraft.mcinfinity.util.RandomProvider;

public class MCILayer {
	public static enum Zone {
		LEFT,
		FRONT,
		RIGHT,
		BACK,
		TOP,
		BOTTOM
	}
	public static enum Heading {
		NORTHERLY,
		WESTERLY,
		EASTERLY,
		SOUTHERLY,
		UNCLEAR
	}
	private String name;

	private MCIWorld world;
	private MCILayer top;
	private MCILayer bottom;
	private World mcWorld;

	private String mcWorldDefer;
	private String topDefer;
	private String bottomDefer;
	
	private int chunkEdge;
	private int blockEdge;
	private int blockEdge2;
	private int maxX;
	private int maxZ;
	
	private boolean spawn;
	private boolean launch;
	
	private boolean valid;
	
	public MCILayer(String name, MCIWorld mciWorld, int edge, boolean spawn, boolean launch, String mcWorld, String top,
			String bottom) {
		this.name = name;
		this.world = mciWorld;
		this.chunkEdge = edge;
		this.blockEdge = edge * 16;
		this.maxX = this.blockEdge * 4;
		this.maxZ = this.blockEdge * 3;
		this.blockEdge2 = this.blockEdge *2;
		this.spawn = spawn;
		this.launch = launch;
		this.mcWorldDefer = mcWorld;
		this.topDefer = top;
		this.bottomDefer = bottom;
		this.valid = false;
	}
	
	protected void compile() throws MCICompilationError {
		if (this.topDefer != null) {
			this.top = this.world.getLayer(this.topDefer);
		}
		
		if (this.bottomDefer != null) {
			this.bottom = this.world.getLayer(this.bottomDefer);
		}
		
		if (this.mcWorldDefer == null) {
			throw new MCICompilationError(this.world, this, "No valid Minecraft world defined for this layer!");
		}
		
		this.mcWorld = Bukkit.getWorld(this.mcWorldDefer);
		
		if (this.mcWorld == null) {
			throw new MCICompilationError(this.world, this, "No Minecraft world found matching requested by name " + this.mcWorldDefer);
		}
		this.valid = true;
	}
	
	/* Accessors */
	
	public String getName() {
		return this.name;
	}
	
	public MCIWorld getWorld() {
		if (!valid) return null;
		return this.world;
	}
	
	public World getRealWorld() {
		if (!valid) return null;
		return this.mcWorld;
	}
	
	public boolean isSpawn() {
		if (!valid) return false;
		return this.spawn;
	}
	
	public boolean isLaunch() {
		if (!valid) return false;
		return this.launch;
	}
	
	public MCILayer above() {
		if (!valid) return null;
		return this.top;
	}
	
	public MCILayer below() {
		if (!valid) return null;
		return this.bottom;
	}
	
	/* Helper methods */

	public boolean inLayer(Player player) {
		if (!valid) return false;
		return inLayer(player.getLocation());
	}
	
	public boolean inLayer(Block block) {
		if (!valid) return false;
		return inLayer(block.getLocation());
	}
	
	public boolean inLayer(Entity entity) {
		if (!valid) return false;
		return inLayer(entity.getLocation());
	}
	
	public boolean inLayer(Location location) {
		if (!valid) return false;
		if (location.getWorld() == this.mcWorld) {
			int x = location.getBlockX();
			int z = location.getBlockZ();
			return ( x >= 0 && x < maxX && z >= 0 && z <= maxZ && (
					((x < blockEdge || x >= blockEdge2) && z >= blockEdge && z < blockEdge2)
					||
					x >= blockEdge && x < blockEdge2)
				);
		}
		return false;
	}

	/**
	 * Compute our "zone" location.
	 * 
	 * @param x
	 * @param z
	 * @return
	 */
	public Zone getZone(int x, int z) {
		if (x >= 0 && x < blockEdge) { // always assume valid
			return Zone.LEFT;
		} else if (x < blockEdge2) {
			if (z >= 0 && z < blockEdge) {
				return Zone.TOP;
			} else if (z < blockEdge2) {
				return Zone.FRONT;
			} else {
				return Zone.BOTTOM;
			}
		} else if (x < maxZ) {
			return Zone.RIGHT;
		} else {
			return Zone.BACK;
		}
	}
	
	public Heading getHeading(double dX, double dZ) {
		double aX = Math.abs(dX);
		double aZ = Math.abs(dZ);
		if (dX < 0) {
			if (aX > aZ) {
				return Heading.WESTERLY;
			} else if (aX == aZ) {
				return Heading.UNCLEAR;
			}
		} else if (dX > 0) {
			if (aX > aZ) {
				return Heading.EASTERLY;
			} else if (aX == aZ) {
				return Heading.UNCLEAR;
			}
		} 
		
		if (dZ < 0) {
			return Heading.NORTHERLY;
		} else if (dZ > 0) {
			return Heading.SOUTHERLY;
		} else {
			return Heading.UNCLEAR;
		}
	}

	
	/**
	 * We figure out where to "adjust" the location based on where it is trying to leave
	 * 
	 * @param location
	 * @return
	 */
	public Location moveAtBorder(Location prior, Location location) {
		Vector direction = new Vector(prior.getX() - location.getX(), prior.getY() - location.getY(), prior.getZ() - location.getZ());
		
		Heading motion = getHeading(direction.getX(), direction.getZ()); 
		if (Heading.UNCLEAR.equals(motion)) return prior;
		
		switch(getZone(prior.getBlockX(), prior.getBlockZ())) {
		case LEFT:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from the "left" side
				break;
			case WESTERLY:
				// Move into BACK from the "right" side
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "left" side
				break;
			default: // moving normally
				return location;
			}
			break;
		case TOP:
			switch(motion) {
			case NORTHERLY:
				// Move into BACK from the "top" side
				break;
			case WESTERLY:
				// Move into LEFT from the "top" side
				break;
			case EASTERLY:
				// Move into RIGHT from the "top" side
				break;
			default:
				return location;
			}
			break;
		case FRONT:
			// ???
			return location;
		case BOTTOM:
			switch(motion) {
			case SOUTHERLY:
				// Move into BACK from "bottom" side
				break;
			case WESTERLY:
				// Move into LEFT from "bottom" side
				break;
			case EASTERLY:
				// Move into RIGHT from "bottom" side
				break;
			default:
				return location;
			}
			break;
		case BACK:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from "top" side
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "bottom" side
				break;
			case EASTERLY:
				// Move into LEFT from the "left" side
				break;
			default:
				return location;
			}
			break;
		case RIGHT:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from "right" side
				break;
			case SOUTHERLY:
				// Move into BOTTOM from "right" side
				break;
			default:
				return location;
			}
			break;
		}
		return location;
	}

	/**
	 * This takes the defined geometry and tries a few times to find a safe place to put a player.
	 * 
	 * TODO: safe / unsafe block recognition, etc.
	 * 
	 * @return a location to spawn on or null if none found in a timely fashion.
	 */
	public Location randomSafeLocationInLayer() {
		int tries = 10;
		while (tries > 0) {
			int x = RandomProvider.random(0, maxX);
			int z = 0;
			if (x >= blockEdge && x < blockEdge2) { 
				z = RandomProvider.random(0, maxZ);
			} else {
				z = RandomProvider.random(blockEdge, blockEdge2);
			}
			
			int y = mcWorld.getHighestBlockYAt(x, z);
			if (y < mcWorld.getMaxHeight() - 2 && y > 0) {
				if (mcWorld.getBlockAt(x, y + 1, z).isEmpty() && mcWorld.getBlockAt(x, y + 2, z).isEmpty()) {
					return new Location(mcWorld, x+0.5, y+1.01, z+0.5);
				}
			}
			tries --;
		}
		return null;
	}
	
	// TODO hash
}
