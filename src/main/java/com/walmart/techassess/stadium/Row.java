package com.walmart.techassess.stadium;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Row represents a row within the stadium
 * It contains seats
 * The seats are stored in row fragments.. A row fragment represent a set of contigious
 * seats. One fragment is unassigned, other fragments are assigned
 * Initially, the row starts with a single fragment that is unassigned and spans the entire row
 * As seats get allocated the unassigned fragment is broken off into 2: one assinged and one unassigned
 * When all the seats are allocated, the unassigned fragment dissapears
 * 
 * During deallocation, the deallocated fragment is removed and all fragments after it are moved up
 * If required a new unassinged fragment is created or the existing unassinged fragment is stretched
 * @author jlalwani
 *
 */
public class Row implements Comparable<Row>{

	final int level;
	final int rowNum;
	final int numSeats;
	// the fragments
	final SortedSet<RowFragment> fragments = new TreeSet<RowFragment>();
	// the unassigned fragment
	Optional<RowFragment> unassignedFragment;

	// a function that returns the number of seats in a fragment
	private class NumSeatsFunction implements Function<RowFragment, Integer> {

		@Override
		public Integer apply(RowFragment r) {
			return r.getNumSeats();
		}
		
	}
	
	/**
	 * Constructor
	 * Creates a single unassigned fragment
	 * @param level
	 * @param rowNum
	 * @param numSeats
	 */
	public Row(int level, int rowNum, int numSeats) {
		this.level = level;
		this.rowNum = rowNum;
		this.numSeats = numSeats;
		unassignedFragment = Optional.of(new RowFragment(level, rowNum, 1, numSeats));
		fragments.add(unassignedFragment.get());
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		result = prime * result + rowNum;
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Row other = (Row) obj;
		if (level != other.level)
			return false;
		if (rowNum != other.rowNum)
			return false;
		return true;
	}



	/**
	 * THis returns the number of unallocated seats in the row
	 * @return
	 */
	public int numUnallocatedSeats() {
		// sinply find the number of seats in the unassigned fragment
		// if no unassigned fragment, 0 seats available
		return unassignedFragment.map(new NumSeatsFunction()).orElse(0);
	}

	/**
	 * Allocate the seats in the row
	 * @param numSeats
	 * @param result
	 * @return
	 */
	public synchronized int allocate(int numSeats, SortedSet<RowFragment> result) {

		assert unassignedFragment.isPresent();
		RowFragment uaFrag = unassignedFragment.get();
		assert numSeats<=uaFrag.getNumSeats();
		
		//create a new assigned fragment and add it to list of fragments
		RowFragment assignedFragment = new RowFragment(level, rowNum, uaFrag.getStartSeat(), uaFrag.getStartSeat()+numSeats-1);
		result.add(assignedFragment);
		fragments.add(assignedFragment);
		
		// remove currently unassigned fragment and create a new one that represents the currently unassigned seats
		fragments.remove(uaFrag);
		if(numSeats==uaFrag.getNumSeats())
		{
			// no unassigned fragment left
			unassignedFragment=Optional.empty();
		}
		else
		{
			// new unassigned fragment
			unassignedFragment = Optional.of(new RowFragment(numSeats, rowNum, uaFrag.getStartSeat()+numSeats, uaFrag.getEndSeat()));
			fragments.add(unassignedFragment.get());
			
		}
		return numSeats;
	}



	@Override
	public int compareTo(Row o) {
		if(level<o.level) return -1;
		if(level>o.level) return 1;
		if(rowNum<o.rowNum) return -1;
		if(rowNum>o.rowNum) return 1;
		return 0;
	}



	/**
	 * deallocates this fragment
	 */
	public synchronized void  deallocate(RowFragment fragment) {
		
		if(!fragments.contains(fragment))
		{
			// someone already deallocated.. no op
			return;
		}
		// contains the list of new framgents
		SortedSet<RowFragment> newFragments = new TreeSet<RowFragment>();
		
		// go through existing fragments
		for(RowFragment existingFragment: fragments)
		{
			if (existingFragment.compareTo(fragment)<0)
			{
				// fragment before the fragment to be removed
				newFragments.add(existingFragment);// add as is
			}
			else if (existingFragment.compareTo(fragment)>0)
			{
				// fragment after fragment being removed
				if(unassignedFragment.isPresent()&&unassignedFragment.get().equals(existingFragment))
				{
					// unassigned fragment, stretch it out
					unassignedFragment = Optional.of(unassignedFragment.get().stretchStart(fragment.getNumSeats()));
					newFragments.add(unassignedFragment.get());
				}
				else
				{
					newFragments.add(existingFragment.shiftUp(fragment.getNumSeats()));
				}
			}
				
		}
		if(!unassignedFragment.isPresent())
		{
			// create new unassigned fragment
			unassignedFragment = Optional.of(new RowFragment(level, rowNum, newFragments.last().getEndSeat()+1, numSeats));
			newFragments.add(unassignedFragment.get());
		}
		// replace fragments with new fragments
		fragments.clear();
		fragments.addAll(newFragments);
	}
	
}
