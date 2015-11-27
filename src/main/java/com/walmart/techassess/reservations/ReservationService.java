package com.walmart.techassess.reservations;

import java.util.List;
import java.util.SortedSet;

import com.walmart.techassess.stadium.RowFragment;

/**
 * This interface describe the services provided by the reservation module
 * The reservation module is responsible for keeping track of holds and reservations
 * @author jlalwani
 *
 */
public interface ReservationService {


	/**
	 * Add a hold on this rows. 
	 * @param rowsAllocated
	 * @param customerEmail
	 * @return
	 */
	SeatHold addHold(SortedSet<RowFragment> rowsAllocated, String customerEmail);

	/**
	 * find the hold by it's id
	 * @param seatHoldId
	 * @return
	 */
	SeatHold findHold(int seatHoldId);

	/**
	 * Reserve the hold
	 * @param seatHoldId
	 * @return
	 */
	String reserve(int seatHoldId);

	/**
	 * Get the details of the reservation 
	 * @param confirmationCode
	 * @return
	 */
	Reservation getReservation(String confirmationCode);

	/**
	 * Checks if the seat hold id is valid
	 * @param seatHoldId
	 * @return
	 */
	boolean isValidHoldId(int seatHoldId);

	/**
	 * removes and returns the fragments whose hold has expired
	 * @return
	 */
	List<SeatHold> extractExpiredFragments();

}
