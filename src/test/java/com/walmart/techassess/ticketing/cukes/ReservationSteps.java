package com.walmart.techassess.ticketing.cukes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import com.walmart.techassess.reservations.Reservation;
import com.walmart.techassess.reservations.ReservationService;
import com.walmart.techassess.reservations.ReservationServiceImpl;
import com.walmart.techassess.reservations.SeatHold;
import com.walmart.techassess.stadium.StadiumService;
import com.walmart.techassess.stadium.StadiumServiceImpl;
import com.walmart.techassess.ticketing.TicketService;
import com.walmart.techassess.ticketing.TicketServiceImpl;

import cucumber.api.DataTable;
import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ReservationSteps {

	int timeout = 1;
	ReservationService reservationService;
	StadiumService stadiumService; 
	TicketService ticketService;
	
	public class State 
	{

		boolean seatsAvailable = false;
		SeatHold hold = null;
		String confirmationCode = "";
		public boolean isSeatsAvailable() {
			return seatsAvailable;
		}
		public void setSeatsAvailable(boolean seatsAvailable) {
			this.seatsAvailable = seatsAvailable;
		}
		public SeatHold getHold() {
			return hold;
		}
		public void setHold(SeatHold hold) {
			this.hold = hold;
		}
		public String getConfirmationCode() {
			return confirmationCode;
		}
		public void setConfirmationCode(String confirmationCode) {
			this.confirmationCode = confirmationCode;
		}
		
		
	}
	
	Map<String, State> state = new HashMap<String, State>();

	@Given("^a timeout of (\\d+) sec$")
	public void a_timeout_of_sec(int timeout) throws Throwable {
		this.timeout = timeout;
	}

	@Given("^a performance with the following levels$")
	public void a_performance_with_the_following_levels(List<StadiumInfo> stadiumInfos) throws Throwable {
		// Write code here that turns the phrase above into concrete actions
		// For automatic transformation, change DataTable to one of
		// List<YourType>, List<List<E>>, List<Map<K,V>> or Map<K,V>.
		// E,K,V must be a scalar (String, Integer, Date, enum etc)
		int minLevel = stadiumInfos.get(0).getId();
		int maxLevel = stadiumInfos.get(stadiumInfos.size()-1).getId();
		List<Integer> numRows = new ArrayList<Integer>();
		List<Integer> numSeats = new ArrayList<Integer>();
		List<List<Integer>> filledSeats = new ArrayList<List<Integer>>();
		for(StadiumInfo stadiumInfo: stadiumInfos)
		{
			String[] seats = stadiumInfo.getSeats().split(",");
			numRows.add(seats.length);
			numSeats.add(seats[0].length());
			List<Integer> filledSeatsForLevel = new ArrayList<Integer>();
			for(String seatsInRow: seats)
			{
				filledSeatsForLevel.add(StringUtils.countMatches(seatsInRow, "R"));
			}
			filledSeats.add(filledSeatsForLevel);
		}
		stadiumService = new StadiumServiceImpl(minLevel, maxLevel, numRows, numSeats);
		for(int level=minLevel; level<=maxLevel; level++)
		{
			List<Integer> filledSeatsForLevel = filledSeats.get(level-minLevel);
			for(int row=0; row<filledSeatsForLevel.size(); row++)
			{
				if(filledSeatsForLevel.get(row)>0)
					((StadiumServiceImpl)stadiumService).allocateInRow(filledSeatsForLevel.get(row), level, row);
			}
		}
		reservationService = new ReservationServiceImpl(timeout*1000);
		ticketService = new TicketServiceImpl(stadiumService, reservationService);
	}

	@When("^User \"([^\"]*)\" holds (\\d+) seats for level (\\d+) - (\\d+)$")
	public void user_holds_seats_for_level(String customerEmail, int numSeats, int minLevel, int maxLevel)
			throws Throwable {
		state.put(customerEmail, new State());
		state.get(customerEmail).setSeatsAvailable(ticketService.numSeatsAvailable(Optional.of(minLevel), Optional.of(maxLevel))>=numSeats);
		if(state.get(customerEmail).isSeatsAvailable())
		{
			state.get(customerEmail).setHold(ticketService.findAndHoldSeats(numSeats, Optional.of(minLevel), Optional.of(maxLevel), customerEmail));
		}
	}

	@When("^User \"([^\"]*)\" reserves seats$")
	public void user_reserves_seats(String customerEmail) throws Throwable {
		state.get(customerEmail).setConfirmationCode(ticketService.reserveSeats(state.get(customerEmail).getHold().getSeatHoldId(), customerEmail));
	}

	@Then("^User \"([^\"]*)\" gets \"([^\"]*)\" seats$")
	public void user_gets_seats(String customerEmail, String expectedSeats) throws Throwable {
		Reservation reservation = ticketService.getReservation(state.get(customerEmail).getConfirmationCode());
		Assert.assertEquals(expectedSeats, reservation.toString());

	}

	@Then("^User \"([^\"]*)\" gets error$")
	public void user_gets_error(String customerEmail) throws Throwable {
		Assert.assertFalse(state.get(customerEmail).isSeatsAvailable());
	}

	@When("^User \"([^\"]*)\" waits for (\\d+) sec$")
	public void user_waits_for_sec(String customerEMail, int secs) throws Throwable {

		Thread.sleep(secs * 1000);
	}

}
