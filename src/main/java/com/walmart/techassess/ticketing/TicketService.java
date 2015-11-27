package com.walmart.techassess.ticketing;

import java.util.Optional;

import com.walmart.techassess.reservations.Reservation;
import com.walmart.techassess.reservations.SeatHold;

/**
 * Ticket Service interface. Exposes the services provided by the ticketing service
 * @author jlalwani
 *
 */
public interface TicketService {
	/**

	* The number of seats in the requested level that are neither held nor reserved

	* 

	* @param venueLevel a numeric venue level identifier to limit the search

	* @return the number of tickets available on the provided level

	*/

	 int numSeatsAvailable(Optional<Integer> venueLevel);
	 
	 /**

		* The number of seats in the requested levels that are neither held nor reserved

		* 

		* @param minLevel, maxLevel a numeric venue level identifier to limit the search

		* @return the number of tickets available on the provided level

		*/

		 int numSeatsAvailable(Optional<Integer> minLevel, Optional<Integer> maxLevel);

	/**

	* Find and hold the best available seats for a customer

	* 

	* @param numSeats the number of seats to find and hold

	* @param minLevel the minimum venue level 

	* @param maxLevel the maximum venue level 

	* @param customerEmail unique identifier for the customer

	* @return a SeatHold object identifying the specific seats and related

	information 

	*/

	 SeatHold findAndHoldSeats(int numSeats, Optional<Integer> minLevel,

	Optional<Integer> maxLevel, String customerEmail);

	/**

	* Commit seats held for a specific customer

	* 

	* @param seatHoldId the seat hold identifier

	* @param customerEmail the email address of the customer to which the seat hold

	is assigned

	* @return a reservation confirmation code 

	*/ 

	String reserveSeats(int seatHoldId, String customerEmail);
	
	/**
	 * Get the seats reserved for the reservation. 
	 * Note that this is subject to change while reservations are been made, 
	 * and should be called after all reservations are made (will-call) 
	 * @param confirmationCode
	 * @return
	 */
	Reservation getReservation(String confirmationCode);
}
