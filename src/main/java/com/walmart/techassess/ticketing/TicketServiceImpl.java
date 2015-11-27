package com.walmart.techassess.ticketing;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.walmart.techassess.reservations.HoldExpiredException;
import com.walmart.techassess.reservations.Reservation;
import com.walmart.techassess.reservations.ReservationService;
import com.walmart.techassess.reservations.SeatHold;
import com.walmart.techassess.stadium.RowFragment;
import com.walmart.techassess.stadium.StadiumService;

/**
 * Implementation of Teicketing Service
 * Relies on 2 other services
 * a) Stadium Service - this service is responsible for managing the allocation and deallocation of seats
 * b) Reservation Service - responsible for managing the reservations
 * 
 * Both services are injected into this service
 * @author jlalwani
 *
 */
public class TicketServiceImpl implements TicketService {

	// the stadium service
	final StadiumService stadiumService;
	
	//the reservation service
	final ReservationService reservationService;
	
	
	
	public TicketServiceImpl(StadiumService stadiumService, ReservationService reservationService) {
		super();
		assert stadiumService!=null;
		assert reservationService!=null;
		this.stadiumService = stadiumService;
		this.reservationService = reservationService;
	}

	@Override
	public int numSeatsAvailable(Optional<Integer> venueLevel) {
		return numSeatsAvailable(venueLevel, venueLevel);
	}

	@Override
	public int numSeatsAvailable(Optional<Integer> minLevel, Optional<Integer> maxLevel) {
		//validations
		if(minLevel==null) throw new IllegalArgumentException("Venue level cannot be null");
		if(maxLevel==null) throw new IllegalArgumentException("Venue level cannot be null");
		int startLevel = minLevel.orElse(stadiumService.getMinLevel());
		int endLevel = maxLevel.orElse(stadiumService.getMaxLevel());
		if(startLevel<stadiumService.getMinLevel()||endLevel>stadiumService.getMaxLevel()) throw new IllegalArgumentException("Level should be between min and max");
		
		//get the number of seats available from stadiumService
		return stadiumService.numSeatsAvailable(startLevel, endLevel);
	}

	@Override
	public SeatHold findAndHoldSeats(int numSeats, Optional<Integer> minLevel, Optional<Integer> maxLevel,
			String customerEmail) {

		//validation
		if(minLevel==null||maxLevel==null) throw new IllegalArgumentException("Venue level cannot be null");
		if(customerEmail==null||customerEmail.length()==0) throw new IllegalArgumentException("Need Customer email address");
		if(numSeats==0) throw new IllegalArgumentException("Invalid number of seats requested");
		int startLevel = minLevel.orElse(stadiumService.getMinLevel());
		int endLevel = maxLevel.orElse(startLevel);
		if(startLevel<stadiumService.getMinLevel()||endLevel>stadiumService.getMaxLevel()) throw new IllegalArgumentException("Level should be between min and max");
		
		//before we get down to business.. let's cleanup any other reservations that have expired
		deallocateExpiredReservations();
		
		//ask stadium to allocate seats in the requested level
		SortedSet<RowFragment> rowsAllocated = stadiumService.allocate(numSeats, startLevel, endLevel);
		
		//add a hold into the reservation system for those seats
		return reservationService.addHold(rowsAllocated, customerEmail);
		
	}


	@Override
	public String reserveSeats(int seatHoldId, String customerEmail) {
		//validations
		if(customerEmail==null||customerEmail.length()==0) throw new IllegalArgumentException("Need Customer email address");
		
		//housekeeping
		deallocateExpiredReservations();
		
		//check if the hold is still alive
		if(!reservationService.isValidHoldId(seatHoldId))
		{
			//oops no.. just expired.. so sad!
			throw new HoldExpiredException(seatHoldId);
		}
		
		//get the hold
		SeatHold hold = reservationService.findHold(seatHoldId);
		
		//check if the hold belongs to the user who is trying to reserve the seats
		if(!hold.getCustomerEmailAddress().equals(customerEmail)) 
			throw new IllegalArgumentException("Wrong customer trying to reserve seats");
		
		//reserve it... note that there is a slight chance that the hold might have expired
		//so this method might throw a hold expired exception too
		return reservationService.reserve(hold.getSeatHoldId());
	}

	@Override
	public Reservation getReservation(String confirmationCode) {
		//validation
		if(confirmationCode==null||confirmationCode.length()==0) throw new IllegalArgumentException("Need Customer email address");
		
		//get the reservations
		return reservationService.getReservation(confirmationCode);
		
	}
	

	private void deallocateExpiredReservations() {
		List<SeatHold> expiredHolds = reservationService.extractExpiredFragments();
		if(expiredHolds.isEmpty())
		{
			// nothing to do.. scram
			return;
		}
		SortedSet<RowFragment> deallocatables = new TreeSet<RowFragment>();
		for(SeatHold hold: expiredHolds)
		{
			deallocatables.addAll(hold.getReservation().getRowsReserved());
		}
		// deallocate the fragments.. please note that this 
		// is a costly operation so should be avoided if possible
		stadiumService.deallocate(deallocatables);
		
	}

}
