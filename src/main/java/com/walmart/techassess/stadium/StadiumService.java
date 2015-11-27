package com.walmart.techassess.stadium;

import java.util.SortedSet;

import com.walmart.techassess.stadium.RowFragment;

/**
 * This interface represents the stadium
 * The implmenting class is responsible foe managing the seats
 * @author jlalwani
 *
 */
public interface StadiumService {

	/**
	 * 
	 * @return the minimum level in the stadium. This is the costliest level
	 */
	int getMinLevel();

	/**
	 * 
	 * @return the maximum level in the stadiunm.. this is the cheapest level
	 */
	Integer getMaxLevel();

	/**
	 * Get the number of seats available. 
	 * This excludes all seats that are already allocated
	 * Please note that this is not a thread-safe operation, 
	 * so in a concurrent environment this number might be off 
	 * @param minLevel
	 * @param maxLevel
	 * @return
	 */
	int numSeatsAvailable(int minLevel, int maxLevel);

	/**
	 * Allocate the seats
	 * @param numSeats
	 * @param startLevel
	 * @param endLevel
	 * @return
	 */
	SortedSet<RowFragment> allocate(int numSeats, int startLevel, int endLevel);
	
	/**
	 * Deallocates the fragments
	 * @param fragments
	 */
	void deallocate(SortedSet<RowFragment> fragments);

}
