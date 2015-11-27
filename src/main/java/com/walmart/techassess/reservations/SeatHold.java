package com.walmart.techassess.reservations;

import java.util.SortedSet;

import com.walmart.techassess.stadium.RowFragment;

/**
 * This class represents a single hold
 * @author jlalwani
 *
 */
public class SeatHold {

	final int seatHoldId; // the id of the hold
	final String customerEmailAddress;
	final Reservation reservation;
	final long timestampHold;// time at which the hold was made

	public SeatHold(int seatHoldId, String customerEmailAddress, SortedSet<RowFragment> rowsAllocated, long timestampHold) {
		super();
		assert customerEmailAddress!=null && customerEmailAddress.length()>0;
		this.seatHoldId = seatHoldId;
		this.customerEmailAddress = customerEmailAddress;
		this.reservation = new Reservation(rowsAllocated);
		this.timestampHold = timestampHold;
	}

	public int getSeatHoldId() {
		return seatHoldId;
	}

	public String getCustomerEmailAddress() {
		return customerEmailAddress;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public long getTimestampHold() {
		return timestampHold;
	}
	
	
}
