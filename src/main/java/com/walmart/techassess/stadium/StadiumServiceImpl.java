package com.walmart.techassess.stadium;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A memory based implementation of Stadium service
 * 
 * A Stadium is made up of levels. Levels are made up of rows and rows are made up of seats
 * This service encapsulates these structures
 * It contains a set of levels. 
 * 
 * Doesn't persist the data
 * Eventually this will need to be backed by a persistent store 
 * 
 * Please note that because this is a memory based implementations, it's not transactional
 * so there is a slight chance that it will be left in an incorrect state in case of exception
 * 
 * Real implementations would use a persistent store that provides some notion of
 * consistency
 * 
 * 
 * @author jlalwani
 *
 */
public class StadiumServiceImpl implements StadiumService {

	
	final int minLevel, maxLevel;
	
	//the stadium is made up of levels
	final List<Level> levels;
	
	
	/**
	 * Constructor
	 * @param minLevel - the minimum level
	 * @param maxLevel
	 * @param numRows
	 * @param numSeats
	 */
	public StadiumServiceImpl(int minLevel, int maxLevel, List<Integer> numRows, List<Integer> numSeats) {
		super();
		assert minLevel<=maxLevel;
		assert(numSeats.size()==(maxLevel-minLevel+1));
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		// create the levels
		this.levels = new ArrayList<Level>();
		for(int i=0; i<numSeats.size(); i++)
		{
			levels.add(new Level(minLevel+i, numRows.get(i), numSeats.get(i)));
		}
	}

	@Override
	public int getMinLevel() {
		
		return minLevel;
	}

	@Override
	public Integer getMaxLevel() {
		
		return maxLevel;
	}

	@Override
	public int numSeatsAvailable(int startLevel, int endLevel) {
		int result = 0;
		assert startLevel>=minLevel;
		assert endLevel<=maxLevel;
		
		//simply add up unallocated seat in each level
		for(int i=startLevel; i<=endLevel; i++)
		{
			result +=levels.get(i-minLevel).numUnallocatedSeats();
		}
		return result;
	}

	@Override
	public SortedSet<RowFragment> allocate(int numSeats, int startLevel, int endLevel) {

		SortedSet<RowFragment> result = new TreeSet<RowFragment>();
		assert startLevel>=minLevel;
		assert endLevel<=maxLevel;
		assert startLevel<=endLevel;
		assert numSeats>0;
		
		// start from the lowest level and allocate seats to lower levels first
		// allocate the remaining seats to higher levels
		for(int i=startLevel; i<=endLevel&&numSeats>0; i++)
		{
			numSeats-=levels.get(i-minLevel).allocate(numSeats, result);
		}
		if(numSeats>0)
		{
			//Couldn't allocate all seats
			deallocate(result);
			throw new IllegalArgumentException("Out of seats!");			
		}
		return result;
	}

	@Override
	public void deallocate(SortedSet<RowFragment> fragments) {
		// simply deallocate each fragment from each level
		for(RowFragment fragment: fragments)
		{
			Level level = levels.get(fragment.getLevel()-minLevel);
			level.deallocate(fragment);
		}
		
	}
	
	/**
	 * A back door method to allocate seats in a sepcific row.. use only for testing
	 * @param numSeats
	 * @param level
	 * @param row
	 */

	public void allocateInRow(Integer numSeats, int level, int row) {
		Level l = levels.get(level-minLevel);
		l.allocateInRow(numSeats, row);
		
	}

}
