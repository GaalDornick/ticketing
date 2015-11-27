package com.walmart.techassess.stadium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class contains the functionality for managing a level
 * A level is made up of rows. THis class is responsibble for allocating and deallocating seats in it's rows
 * 
 * This class also does an important job of providing thread-safety to the applications
 * Rows are thread-safe. However, to reduce contention, it's better that 2 concurrent threads 
 * don't try to reserve seats in the same row. In case of highly concurrent winds, contentding on 
 * Row might lead to starvation of some threads, and will also increase the load on the system
 * It is better for some of the requests to fail than to contend for resources
 * 
 * When the Level class is allocating seats in one row, it "checks out" the row
 * this prevents other concurrent requests from allocating to the same row. Once seats
 * are allocated in the row, the row is checked back in for use of other requests
 * 
 * THis class is also responsible for deallocating seats
 * Since deallocation of seats requires compaction of rows, it cannot run concurrently with allocation
 * ALso, since deallocation occurs over multiple rows, this class freezes all allocations
 * when it deallocates seats
 * @author jlalwani
 *
 */
public class Level {
	
	// all the rows in the level
	final List<Row> allRows = Collections.synchronizedList(new ArrayList<Row>());
	
	//the next few members contains the same row objects as in the list above
	// we have split them according to their status. this makes it easy to find them
	// please note that this implementation was backed by a persistent store, we wouldn't need 
	// to duplicate the data structures.. we would just use the search and indexing facility of the store
	// Please note that the following elements will not contain the rows that are in the process of
	// being held. When a row is checked out it dissapears from the following members
	
	// all the empty rows sorted by their natural ordering 
	final SortedSet<Row> emptyRows = Collections.synchronizedSortedSet(new TreeSet<Row>());
	// the rows that are completely filled
	final SortedSet<Row> filledRows = Collections.synchronizedSortedSet(new TreeSet<Row>());
	// sthis set stores all the half filled rows sorted by number of unallocated seats
	// makes it easy to find the seat of correct size
	final SortedSet<Row> halfFilledRows = Collections.synchronizedSortedSet(new TreeSet<Row>(new Comparator<Row>() {

		@Override
		public int compare(Row arg0, Row arg1) {
			if (arg0.numUnallocatedSeats()<arg1.numUnallocatedSeats()) return -1;
			if (arg0.numUnallocatedSeats()>arg1.numUnallocatedSeats()) return 1;
			return arg0.compareTo(arg1);
		}
	}));
	final int seatsPerRow;
	final int level;
	final int numRows;
	

	//this latch is used to halt the allocators when a fragment in the level is being deallocated
	// the assumption is that deallocations will be less frequent than allocations
	// however a deallocation should halt all allocations whereas multiple allocations
	// can go in parallel
	// to enable this we use a ReadWriteLock.. Allocations will use the read lock and deallocations will use a write lock
	ReadWriteLock latch = new ReentrantReadWriteLock(true);
	Lock allocationLock = latch.readLock();
	Lock deallocationLock = latch.writeLock();
	
	/**
	 * Constructor.. creates the required rows
	 * @param level
	 * @param numRows
	 * @param seatsPerRow
	 */
	public Level(int level, int numRows, int seatsPerRow)
	{
		assert numRows>0;
		assert seatsPerRow > 0;
		this.level = level;
		this.numRows = numRows;
		this.seatsPerRow = seatsPerRow;
		for(int i=0; i<numRows; i++)
		{
			emptyRows.add(new Row(level, i, seatsPerRow));
		}
		allRows.addAll(emptyRows);
	}

	/**
	 * returns the number of seats that are available to be held
	 * Please note that this method will exclude all the seats that are held and reserved
	 * ALso, it will exclude all the seats in the rows that are in the process of being allocated
	 * right now
	 * 
	 * WHen the system is running in highly concurrent winds, this number should be taken with
	 * a grain of salt
	 * @return
	 */
	public int numUnallocatedSeats() {
		int result=0;
		for(Row row:halfFilledRows)
		{
			result+=row.numUnallocatedSeats();
		}
		return result+seatsPerRow*emptyRows.size();
	}

	/**
	 * Allocates seats in this level's rows
	 * THis method will check out rows as it allocates them
	 * Note that this means that it will not be able to allocate rows that are checked
	 * out by other concurrent requests
	 * In times of high contention, it may allocate less seats than available
	 * @param numSeats - number of seats to be allocated
	 * @param result - set that stored the allocated fragments
	 * @return number of seats allocated
	 */
	public int allocate(int numSeats, SortedSet<RowFragment> result) {
		allocationLock.lock();
		try
		{
			SortedSet<RowFragment> frags = new TreeSet<RowFragment>();
			// check out an empty row at a time and allocate it till we have 
			// either filled all the empty rows, or the number of seats can 
			// fill a half filled row
			// note that we "check out" the row which means no one else
			// can use this row concurrently. this prevents other threads from trying to allocate
			// to the same row. The advantage of this is that the synchronization points are
			// limited to check out and check in methods, and we don't have to do synchronization 
			// on the row itself
			int seatsAllocated = 0;
			SortedSet<Row> checkedOutRows = new TreeSet<Row>();// hold the checked out rows here
			
			try
			{
				Row checkedOutRow; // the currentl checkout row
				//if number of seats requested is more than seats per row
				// try to allocate completely empty rows
				while(numSeats>=seatsPerRow&&(checkedOutRow=checkoutEmptyRow())!=null)
				{
					checkedOutRows.add(checkedOutRow);
					checkedOutRow.allocate(seatsPerRow, result);
					numSeats-=seatsPerRow;
					seatsAllocated+=seatsPerRow;
				}
				
				//now fill the half filled rows bigger than current row
				while(numSeats>0&&(checkedOutRow=checkoutHalfFilledRow(numSeats))!=null)
				{
					checkedOutRows.add(checkedOutRow);
					int seatsToAllocate = checkedOutRow.numUnallocatedSeats();
					seatsToAllocate = (seatsToAllocate>numSeats)?numSeats:seatsToAllocate;
					checkedOutRow.allocate(seatsToAllocate, result);
					numSeats-=seatsToAllocate;
					seatsAllocated+=seatsToAllocate;
				}
				
				// if we still have seats left, let's put them in an empty row if available
				if(numSeats>0&&(checkedOutRow=checkoutEmptyRow())!=null)
				{
					checkedOutRows.add(checkedOutRow);
					int seatsToAllocate = checkedOutRow.numUnallocatedSeats();
					seatsToAllocate = (seatsToAllocate>numSeats)?numSeats:seatsToAllocate;
					checkedOutRow.allocate(seatsToAllocate, result);
					numSeats-=seatsToAllocate;
					seatsAllocated+=seatsToAllocate;
				}
				
				//now try to fit people in the remaining fill the half filled rows than current row
				while(numSeats>0&&(checkedOutRow=checkoutHalfFilledRow(0))!=null)
				{
					checkedOutRows.add(checkedOutRow);
					int seatsToAllocate = checkedOutRow.numUnallocatedSeats();
					seatsToAllocate = (seatsToAllocate>numSeats)?numSeats:seatsToAllocate;
					checkedOutRow.allocate(seatsToAllocate, result);
					numSeats-=seatsToAllocate;
					seatsAllocated+=seatsToAllocate;
				}
				
				return seatsAllocated;
			}
			finally
			{
			
				//check the rows back in
				for(Row checkedOutRow: checkedOutRows)
				{
		
					if(checkedOutRow.numUnallocatedSeats()==0)
					{
						filledRows.add(checkedOutRow);
					}
					else
					{
						halfFilledRows.add(checkedOutRow);
					}
				}
			}
		}
		finally
		{
			allocationLock.unlock();
		}
		
	}

	private Row checkoutHalfFilledRow(int minSize) {
		return checkoutRow(halfFilledRows, minSize);
	}

	private Row checkoutEmptyRow() {
		return checkoutRow(emptyRows, 0);
	}
	
	private Row checkoutRow(SortedSet<Row> rows, int minSize)
	{
		synchronized(rows)
		{
			if(rows.isEmpty()) return null;
			for(Row candidate: rows)
			{
				if(candidate.numUnallocatedSeats()>=minSize)
				{
					rows.remove(candidate);
					return candidate;
				}
			}
			return null;
		}
	}

	/**
	 * Allocates seats in a particular row.. use only for testing.. not for real code
	 * @param numSeats
	 * @param row
	 */
	public int allocateInRow(Integer numSeats, int row) {
		Row r = allRows.get(row);
		int n = r.allocate(numSeats, new TreeSet<RowFragment>());
		emptyRows.remove(r);
		if(r.numUnallocatedSeats()==0)
		{
			filledRows.add(r);
		}
		else
		{
			halfFilledRows.add(r);
		}
		return n;
		
	}

	/**
	 * deallocates a fragment from this level
	 * @param fragment
	 */
	public void deallocate(RowFragment fragment) {

		assert fragment!=null;
		assert level == fragment.getLevel(); 
		
		deallocationLock.lock();
		try
		{
			Row row = allRows.get(fragment.getRowNum());
			row.deallocate(fragment);
			
			// remove the row from wherver it is now
			emptyRows.remove(row);
			halfFilledRows.remove(row);
			filledRows.remove(row);
			
			// add it to it's new place
			if(row.numUnallocatedSeats()==0)
			{
				filledRows.add(row);
			}
			else if(row.numUnallocatedSeats()==seatsPerRow)
			{
				emptyRows.add(row);
			}
			else 
			{
				halfFilledRows.add(row);
			}
		}
		finally
		{
			deallocationLock.unlock();
		}
		
		
	}
}
