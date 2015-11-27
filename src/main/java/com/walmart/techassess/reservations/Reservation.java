package com.walmart.techassess.reservations;

import java.util.SortedSet;

import com.walmart.techassess.stadium.RowFragment;

/**
 * This class represents the seats being held. It contains a list of row fragments
 * @author jlalwani
 *
 */
public class Reservation {
	final SortedSet<RowFragment> rowsReserved;

	public Reservation(SortedSet<RowFragment> rowsReserved) {
		super();
		this.rowsReserved = rowsReserved;
	}

	public SortedSet<RowFragment> getRowsReserved() {
		return rowsReserved;
	}


	@Override
	public String toString() {
		StringBuilder str=new StringBuilder();
		for(RowFragment reserved: rowsReserved)
		{
			str.append(", ").append(reserved.toString());
		}
		if(str.length()>2) return str.substring(2);
		else return "";
	}
	
	
	
}
