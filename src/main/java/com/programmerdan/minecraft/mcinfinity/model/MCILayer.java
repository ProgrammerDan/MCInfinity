package com.programmerdan.minecraft.mcinfinity.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.programmerdan.minecraft.mcinfinity.MCInfinity;
import com.programmerdan.minecraft.mcinfinity.util.RandomProvider;

public class MCILayer {
	public static enum Zone {
		LEFT,
		FRONT,
		RIGHT,
		BACK,
		TOP,
		BOTTOM,
		UNCLEAR
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
			return ( x >= 0 && x < maxX && z >= 0 && z < maxZ && (
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
	 * @param x block location x
	 * @param z block location z
	 * @return one of Zone values
	 */
	public Zone getZone(int x, int z) {
		if (x >= 0 && x < blockEdge) {
			if (z >= blockEdge && z < blockEdge2) {
				return Zone.LEFT;
			}
		} else if (x < blockEdge2) {
			if (z >= 0 && z < blockEdge) {
				return Zone.TOP;
			} else if (z < blockEdge2) {
				return Zone.FRONT;
			} else if (z < maxZ){
				return Zone.BOTTOM;
			}
		} else if (z >= blockEdge && z < blockEdge2) {
			if (x < maxZ) {
				return Zone.RIGHT;
			} else if (x < maxX) {
				return Zone.BACK;
			}
		}
		return Zone.UNCLEAR;
	}
	
	/**
	 * Given a coordinate set, compute our "heading" in rough terms
	 * 
	 * @param dX delta X
	 * @param dZ delta Z
	 * @return one of Heading values
	 */
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
	 * Determines on which edge the location is leaving the mentioned zone.
	 * 
	 * @param now
	 * @param then
	 * @return
	 */
	public Heading getDepartureEdge(Zone now, Location then) {
		double x = then.getX();
		double z = then.getZ();
		switch(now) {		
		case BACK:
			if (x >= maxX) {
				return Heading.EASTERLY;
			} else if (z < blockEdge) {
				return Heading.NORTHERLY;
			} else if (z >= blockEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case BOTTOM:
			if (z >= maxZ) {
				return Heading.SOUTHERLY;
			} else if (x < blockEdge) {
				return Heading.WESTERLY;
			} else if (x >= blockEdge2) {
				return Heading.EASTERLY;
			}
			break;
		case LEFT:
			if (x < 0) {
				/*
				 * TODO
				 * if (z < blockEdge) {
				 
					if (z - blockEdge < x) { // more z then x
						return Heading.NORTHERLY;
					} else if (z - blockEdge > x) { // more x then z
						return Heading.WESTERLY;
					} else {
						return Heading.UNCLEAR;
					}
				} else if (z >= blockEdge2) {
					if ( - (z - blockEdge2) < x) { // more z then x
						return Heading.SOUTHERLY;
					} else if ( - (z - blockEdge2) > x) { // more x then z
						return Heading.WESTERLY;
					} else {
						return Heading.UNCLEAR;
					}
				} else {
					return Heading.WESTERLY;
				}
				*/
				return Heading.WESTERLY;
			} else if (z < blockEdge) {
				return Heading.NORTHERLY;
			} else if (z >= blockEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case RIGHT:
			if (z < blockEdge) {
				return Heading.NORTHERLY;
			} else if (z >= blockEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case TOP:
			if (z < 0) {
				return Heading.NORTHERLY;
			} else if (x < blockEdge) {
				return Heading.WESTERLY;
			} else if (x >= blockEdge2) {
				return Heading.EASTERLY;
			}
			break;
		case FRONT:
		case UNCLEAR:
		default:
			return Heading.UNCLEAR;
		}
		return Heading.UNCLEAR;
	}

	
	/**
	 * We figure out where to "adjust" the location based on where it is trying to leave
	 * 
	 * @param location
	 * @return
	 */
	public Location moveAtBorder(Location prior, Location loc) {
		MCInfinity.getPlugin().info("Remap border movement. Beginning with {0} to {1}", prior, loc);
		Location location = loc.clone();

		double x = location.getX();
		double z = location.getZ();
		
		Zone zone = getZone(prior.getBlockX(), prior.getBlockZ());
		Heading motion = getDepartureEdge(zone, location);
		if (Heading.UNCLEAR.equals(motion)) return prior;
		
		switch(zone) {
		case UNCLEAR:
			MCInfinity.getPlugin().info("Remap border movement. Ending with out of bounds");
			return prior;
		case LEFT:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from the "left" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// x becomes z
				// blockEdge2 - z becomes x
				x = blockEdge2 - location.getZ();
				z = location.getX();
				break;
			case WESTERLY:
				// Move into BACK from the "right" side
				// no yaw change
				// z is z
				// x is x + blockEdge4
				x = location.getX() + maxX;
				z = location.getZ();
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "left" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes z - blockEdge
				// z becomes blockEdge3-x
				x = location.getZ() - blockEdge;
				z = maxZ - location.getX();
				break;
			default: // moving normally
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return location;
			}
			break;
		case TOP:
			switch(motion) {
			case NORTHERLY:
				// Move into BACK from the "top" side
				location.setYaw(location.getYaw() + 180f); // 180 clockwise
				// x becomes blockEdge3 + (blockEdge2-x)
				// z becomes blockEdge + (- z)
				x = maxZ + (blockEdge2 - location.getX());
				z = blockEdge - location.getZ();
				break;
			case WESTERLY:
				// Move into LEFT from the "top" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes z
				// z becomes blockEdge2 - x
				x = location.getZ();
				z = blockEdge2 - location.getX();
				break;
			case EASTERLY:
				// Move into RIGHT from the "top" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// z becomes x - blockEdge
				// x becomes blockEdge3 - z
				x = maxZ - location.getZ();
				z = location.getX() - blockEdge;
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
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
				location.setYaw(location.getYaw() - 180f); // 180 counter
				// x becomes blockEdge3 + blockEdge2 - x
				// z becomes blockEdge + blockEdge4 - z
				x = maxZ + blockEdge2 - location.getX();
				z = blockEdge + maxX - location.getZ();
				break;
			case WESTERLY:
				// Move into LEFT from "bottom" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// x becomes blockEdge3 - z
				// z becomes blockEdge + x);
				x = maxZ - location.getZ();
				z = blockEdge + location.getX();
				break;
			case EASTERLY:
				// Move into RIGHT from "bottom" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes z
				// z becomes blockEdge4 - x
				x = location.getZ();
				z = maxX - location.getX();
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return location;
			}
			break;
		case BACK:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from "top" side
				location.setYaw(location.getYaw() - 180f); // 180 counter
				// x becomes blockEdge + (blockEdge4 - x)
				// z becomes blockEdge - z
				x = blockEdge + (maxX - location.getX());
				z = blockEdge - location.getZ();
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "bottom" side
				location.setYaw(location.getYaw() + 180f); // 180 clockwise
				// x becomes blockEdge + (blockEdge4 - x)
				// z becomes blockEdge2 + (blockEdge3 - z)
				x = blockEdge + (maxX - location.getX());
				z = blockEdge2 + (maxZ - location.getZ());
				break;
			case EASTERLY:
				// Move into LEFT from the "left" side
				// No Yaw change
				// x becomes x - blockEdge4
				// z unchanged
				x = location.getX() - maxX;
				z = location.getZ();
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return location;
			}
			break;
		case RIGHT:
			switch(motion) {
			case NORTHERLY:
				// Move into TOP from "right" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes blockEdge + z
				// z becomes blockEdge3 - x
				x = blockEdge + location.getZ();
				z = maxZ - location.getX();
				break;
			case SOUTHERLY:
				// Move into BOTTOM from "right" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// x becomes blockEdge4 - z
				// z becomes x
				x = maxX - location.getZ();
				z = location.getX();
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return location;
			}
			break;
		}
		location.setX(x);
		location.setZ(z);

		MCInfinity.getPlugin().info("Remap border movement. Ending with {0} to {1} from zone {2} with heading {3} into zone {4}", prior, location, zone, motion, getZone(location.getBlockX(), location.getBlockZ()));
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
