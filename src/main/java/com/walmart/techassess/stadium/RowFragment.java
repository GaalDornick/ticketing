package com.walmart.techassess.stadium;

/**
 * A row fragment is a part of a row. It represetns a set of contigious seats
 * @author jlalwani
 *
 */

public class RowFragment implements Comparable<RowFragment>{

	final int level;
	final int rowNum;
	int startSeat;
	int endSeat;
	public RowFragment(int level, int rowNum, int startSeat, int endSeat) {
		super();
		this.level = level;
		this.rowNum = rowNum;
		this.startSeat = startSeat;
		this.endSeat = endSeat;
	}
	public int getRowNum() {
		return rowNum;
	}
	public int getStartSeat() {
		return startSeat;
	}
	public int getEndSeat() {
		return endSeat;
	}
	public int getLevel() {
		return level;
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
		RowFragment other = (RowFragment) obj;
		if (endSeat != other.endSeat)
			return false;
		if (level != other.level)
			return false;
		if (rowNum != other.rowNum)
			return false;
		if (startSeat != other.startSeat)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return String.format("%d#%c{%d-%d}", level, ('A'+rowNum), startSeat, endSeat);
	}
	@Override
	public int compareTo(RowFragment o) {
		if(level<o.level) return -1;
		if(level>o.level) return 1;
		if(rowNum<o.rowNum) return -1;
		if(rowNum>o.rowNum) return 1;
		if(startSeat<o.startSeat) return -1;
		if(startSeat>o.startSeat) return 1;
		if(endSeat<o.endSeat) return -1;
		if(endSeat>o.endSeat) return 1;
		
		return 0;
	}
	public int getNumSeats() {
		
		return endSeat-startSeat+1;
	}
	/**
	 * shift the fragment up by given number of seats
	 * @param numSeats
	 * @return
	 */
	public RowFragment shiftUp(int numSeats) {
		startSeat-=numSeats;
		endSeat-=numSeats;
		return this;
	}
	/**
	 * shift the fragment down by given number of seats
	 * @param numSeats
	 * @return
	 */
	public RowFragment shiftDown(int numSeats) {
		startSeat+=numSeats;
		endSeat+=numSeats;
		return this;
	}
	/**
	 * moves the start seat up by given number of seats
	 * @param numSeats
	 * @return
	 */
	public RowFragment stretchStart(int numSeats) {
		startSeat-=numSeats;
		return this;
	}
	/**
	 * moves the end seat up by given number of seats
	 * @param numSeats
	 * @return
	 */
	public RowFragment stretchEnd(int numSeats) {
		endSeat+=numSeats;
		return this;
	}
}
