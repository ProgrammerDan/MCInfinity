package com.programmerdan.minecraft.mcinfinity.util;

import org.apache.commons.lang3.RandomUtils;

public class RandomProvider {
	/**
	 * Returns [min, max) randomly.
	 * 
	 * @param min Inclusive lower bound
	 * @param max Exclusive higher bound -- at most returns max - 1
	 * @return the random pick
	 */
	public static int random(int min, int max) {
		// TODO: improve randomness
		return RandomUtils.nextInt(min, max);
	}
}
