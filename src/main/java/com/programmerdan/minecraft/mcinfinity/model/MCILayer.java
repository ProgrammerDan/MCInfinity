package com.programmerdan.minecraft.mcinfinity.model;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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
	private int chunkEdge2;
	private int chunkMaxX;
	private int chunkMaxZ;
	
	private int blockEdge;
	private int blockEdge2;
	private int maxX;
	private int maxZ;

	private int ichunkEdge;
	private int ichunkEdge2;
	private int ichunkMaxX;
	private int ichunkMaxZ;
	
	private int iblockEdge;
	private int iblockEdge2;
	private int imaxX;
	private int imaxZ;

	
	private boolean spawn;
	private boolean launch;
	
	private boolean valid;
	
	public MCILayer(String name, MCIWorld mciWorld, int edge, boolean spawn, boolean launch, String mcWorld, String top,
			String bottom) {
		this.name = name;
		this.world = mciWorld;
		this.chunkEdge = edge;
		this.chunkEdge2 = edge * 2;
		this.chunkMaxX = edge * 4;
		this.chunkMaxZ = edge * 3;
		
		this.blockEdge = edge * 16;
		this.maxX = this.blockEdge * 4;
		this.maxZ = this.blockEdge * 3;
		this.blockEdge2 = this.blockEdge *2;
		
		this.ichunkEdge = this.chunkEdge - 1;
		this.ichunkEdge2 = this.chunkEdge2 - 1;
		this.ichunkMaxX = this.chunkMaxX - 1;
		this.ichunkMaxZ = this.chunkMaxZ - 1;
		
		this.iblockEdge = this.blockEdge - 1;
		this.iblockEdge2 = this.blockEdge2 - 1;
		this.imaxX = this.maxX - 1;
		this.imaxZ = this.maxZ - 1;
		
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

	public boolean inLayer(Chunk chunk) {
		if (!valid) return false;
		if (chunk.getWorld() == this.mcWorld) {
			return inLayer(chunk.getX(), chunk.getZ());
		}
		return false;
	}
	
	/**
	 * Chunklayer
	 * 
	 * @param x Chunk x
	 * @param z Chunk z
	 * @return true if in, false else
	 */
	private boolean inLayer(int x, int z) {
		return ( x >= 0 && x < chunkMaxX && z >= 0 && z < chunkMaxZ && (
				((x < chunkEdge || x >= chunkEdge2) && z >= chunkEdge && z < chunkEdge2)
				||
				x >= chunkEdge && x < chunkEdge2)
			);
	}
	
	// TODO: For chunks "outside" this layer, but if still in the world, how to resolve?
	// For chunks more then 1 away from edge "outside" of edge, hide somehow.
	// For chunks "along the edge", display shadow copies rotated to fit
	// perhaps leverage ChunkSnapshot?
	/**
	 * Leverages Orebfuscator technology to transform chunk data for transmission when requested by
	 * the client.
	 * 
	 * TODO: caching of some sort
	 * 
	 * @param origin
	 * @param chunkXToSend
	 * @param chunkZToSend
	 * @return a ChunkData object with remapped data to fit the "orientation" of the player, if necessary, or null if not
	 */
	public RotatingChunkCoord remapChunk(Location origin, int chunkXToSend, int chunkZToSend) {
		if (inLayer(chunkXToSend, chunkZToSend)) return null;
		
		Zone origination = getZone(origin.getBlockX(), origin.getBlockZ());
		Heading edge = getChunkDepartureEdge(origination, chunkXToSend, chunkZToSend);
		
		if (Heading.UNCLEAR.equals(edge)) return null;
		
		int rotation = 0;
		int x = 0;
		int z = 0;
		
		switch(origination) {
		case UNCLEAR:
			MCInfinity.getPlugin().info("Remap chunk is unclear");
			return null;
		case LEFT:
			switch(edge) {
			case NORTHERLY:
				// Move into TOP from the "left" side
				rotation = -90;
				x = ichunkEdge2 - chunkZToSend;
				z = chunkXToSend;
				break;
			case WESTERLY:
				// Move into BACK from the "right" side
				rotation = 0;
				x = chunkXToSend + chunkMaxX;
				z = chunkZToSend;
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "left" side
				rotation = 90;
				x = chunkZToSend - ichunkEdge;
				z = ichunkMaxZ - chunkXToSend;
				break;
			default: // moving normally
				MCInfinity.getPlugin().info("Remap chunk. Ending with no change");
				return null;
			}
			break;
		case TOP:
			switch(edge) {
			case NORTHERLY:
				// Move into BACK from the "top" side
				rotation = 180;
				x = chunkMaxZ + (ichunkEdge2 - chunkXToSend);
				z = ichunkEdge - chunkZToSend;
				break;
			case WESTERLY:
				// Move into LEFT from the "top" side
				rotation = 90;
				x = chunkZToSend;
				z = ichunkEdge2 - chunkXToSend;
				break;
			case EASTERLY:
				// Move into RIGHT from the "top" side
				rotation = -90;
				x = ichunkMaxZ - chunkZToSend;
				z = chunkXToSend - ichunkEdge;
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return null;
			}
			break;
		case FRONT:
			// ???
			return null;
		case BOTTOM:
			switch(edge) {
			case SOUTHERLY:
				// Move into BACK from "bottom" side
				rotation = -180;
				x = chunkMaxZ + ichunkEdge2 - chunkXToSend;
				z = chunkEdge + ichunkMaxX - chunkZToSend;
				break;
			case WESTERLY:
				// Move into LEFT from "bottom" side
				rotation = -90;
				x = ichunkMaxZ - chunkZToSend;
				z = chunkEdge + chunkXToSend;
				break;
			case EASTERLY:
				// Move into RIGHT from "bottom" side
				rotation = 90;
				x = chunkZToSend;
				z = ichunkMaxX - chunkXToSend;
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return null;
			}
			break;
		case BACK:
			switch(edge) {
			case NORTHERLY:
				// Move into TOP from "top" side
				rotation = -180;
				x = chunkEdge + (ichunkMaxX - chunkXToSend);
				z = ichunkEdge - chunkZToSend;
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "bottom" side
				rotation = 180;
				x = chunkEdge + (ichunkMaxX - chunkXToSend);
				z = chunkEdge2 + (ichunkMaxZ - chunkZToSend);
				break;
			case EASTERLY:
				// Move into LEFT from the "left" side
				rotation = 0;
				x = chunkXToSend - ichunkMaxX;
				z = chunkZToSend;
				break;
			default:
				MCInfinity.getPlugin().info("Remap border movement. Ending with no change");
				return null;
			}
			break;
		case RIGHT:
			switch(edge) {
			case NORTHERLY:
				// Move into TOP from "right" side
				rotation = 90;
				x = chunkEdge + chunkZToSend;
				z = ichunkMaxZ - chunkXToSend;
				break;
			case SOUTHERLY:
				// Move into BOTTOM from "right" side
				rotation = -90;
				x = ichunkMaxX - chunkZToSend;
				z = chunkXToSend;
				break;
			default:
				MCInfinity.getPlugin().info("Remap chunks. Ending with no change");
				return null;
			}
			break;
		}
		
		return new RotatingChunkCoord(x, z, rotation);
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
		return getDepartureEdge(now, x, z);
	}
	
	private Heading getDepartureEdge(Zone now, double x, double z) {
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
	 * for chunks
	 * @param now
	 * @param x
	 * @param z
	 * @return
	 */
	private Heading getChunkDepartureEdge(Zone now, int x, int z) {
		switch(now) {		
		case BACK:
			if (x >= chunkMaxX) {
				return Heading.EASTERLY;
			} else if (z < chunkEdge) {
				return Heading.NORTHERLY;
			} else if (z >= chunkEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case BOTTOM:
			if (z >= chunkMaxZ) {
				return Heading.SOUTHERLY;
			} else if (x < chunkEdge) {
				return Heading.WESTERLY;
			} else if (x >= chunkEdge2) {
				return Heading.EASTERLY;
			}
			break;
		case LEFT:
			if (x < 0) {
				return Heading.WESTERLY;
			} else if (z < chunkEdge) {
				return Heading.NORTHERLY;
			} else if (z >= chunkEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case RIGHT:
			if (z < chunkEdge) {
				return Heading.NORTHERLY;
			} else if (z >= chunkEdge2) {
				return Heading.SOUTHERLY;
			}
			break;
		case TOP:
			if (z < 0) {
				return Heading.NORTHERLY;
			} else if (x < chunkEdge) {
				return Heading.WESTERLY;
			} else if (x >= chunkEdge2) {
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
				x = iblockEdge2 - location.getZ();
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
				x = location.getZ() - iblockEdge;
				z = imaxZ - location.getX();
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
				x = maxZ + (iblockEdge2 - location.getX());
				z = iblockEdge - location.getZ();
				break;
			case WESTERLY:
				// Move into LEFT from the "top" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes z
				// z becomes blockEdge2 - x
				x = location.getZ();
				z = iblockEdge2 - location.getX();
				break;
			case EASTERLY:
				// Move into RIGHT from the "top" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// z becomes x - blockEdge
				// x becomes blockEdge3 - z
				x = imaxZ - location.getZ();
				z = location.getX() - iblockEdge;
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
				x = maxZ + iblockEdge2 - location.getX();
				z = blockEdge + imaxX - location.getZ();
				break;
			case WESTERLY:
				// Move into LEFT from "bottom" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// x becomes blockEdge3 - z
				// z becomes blockEdge + x
				x = imaxZ - location.getZ();
				z = blockEdge + location.getX();
				break;
			case EASTERLY:
				// Move into RIGHT from "bottom" side
				location.setYaw(location.getYaw() - 90f); // 90 counter
				// x becomes z
				// z becomes blockEdge4 - x
				x = location.getZ();
				z = imaxX - location.getX();
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
				x = blockEdge + (imaxX - location.getX());
				z = iblockEdge - location.getZ();
				break;
			case SOUTHERLY:
				// Move into BOTTOM from the "bottom" side
				location.setYaw(location.getYaw() + 180f); // 180 clockwise
				// x becomes blockEdge + (blockEdge4 - x)
				// z becomes blockEdge2 + (blockEdge3 - z)
				x = blockEdge + (imaxX - location.getX());
				z = blockEdge2 + (imaxZ - location.getZ());
				break;
			case EASTERLY:
				// Move into LEFT from the "left" side
				// No Yaw change
				// x becomes x - blockEdge4
				// z unchanged
				x = location.getX() - imaxX;
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
				z = imaxZ - location.getX();
				break;
			case SOUTHERLY:
				// Move into BOTTOM from "right" side
				location.setYaw(location.getYaw() + 90f); // 90 clockwise
				// x becomes blockEdge4 - z
				// z becomes x
				x = imaxX - location.getZ();
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
