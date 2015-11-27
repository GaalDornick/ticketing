package com.walmart.techassess.reservations;

public class HoldExpiredException extends RuntimeException {

	public HoldExpiredException(int seatHoldId) {
		super("Invalid hold ID or the hold has expired");
	}

}
