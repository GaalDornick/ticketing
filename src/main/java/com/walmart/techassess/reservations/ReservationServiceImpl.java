package com.walmart.techassess.reservations;

import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.function.BiConsumer;
import java.util.function.LongUnaryOperator;

import com.walmart.techassess.stadium.RowFragment;

/**
 * A memory based implementation of reservation service
 * @author jlalwani
 *
 */
public class ReservationServiceImpl implements ReservationService {
	// this map contains all the holds keyed by hold ID
	Map<Integer, SeatHold> holds = Collections.synchronizedMap(new HashMap<Integer, SeatHold>());
	// this map contains all the reservations keyed by reservation confirmation code
	Map<String, SeatHold> reservations = Collections.synchronizedMap(new HashMap<String, SeatHold>());
	// this keeps a time stamp of the oldest hold
	final AtomicLong oldestHoldTimestamp = new AtomicLong(Long.MAX_VALUE);
	// the hold timeout in milliseconds
	final int timeout;
	
	//this latch is used to halt the reservations when a hold is being expired
	// the assumption is that expirations will be less frequent than reservations
	// however a expiry should halt all reservations whereas multiple reservations
	// can go in parallel
	// to enable this we use a ReadWriteLock.. Reservations will use the read lock and expirations will use a write lock
		
	final ReadWriteLock latch = new ReentrantReadWriteLock();
	final Lock reserveLock = latch.readLock();
	final Lock expireLock = latch.writeLock();
		
	
	public ReservationServiceImpl(int timeout) {
		super();
		this.timeout = timeout;
	}

	@Override
	public SeatHold addHold(SortedSet<RowFragment> rowsAllocated, String customerEmail) {
		final long timestampHold = System.currentTimeMillis();
		// generate an ID and put it in holds
		int seatHoldId = generateSeatHoldId(rowsAllocated, customerEmail);
		SeatHold hold = new SeatHold(seatHoldId, customerEmail, rowsAllocated, timestampHold);
		holds.put(seatHoldId, hold);
		// update the time stamp of the earliest hold if required
		oldestHoldTimestamp.getAndUpdate(new LongUnaryOperator() {
			
			@Override
			public long applyAsLong(long operand) {
				return (operand>timestampHold)?timestampHold:operand;
			}
		});
		return hold;
	}

	private int generateSeatHoldId(SortedSet<RowFragment> rowsAllocated, String customerEmail) {

		final int prime = 31;
		int result = 1;
		for(RowFragment rf: rowsAllocated)
		{
			result = prime * result + rf.hashCode();
		}
		result = prime *result+customerEmail.hashCode();
		return result;
	}

	@Override
	public SeatHold findHold(int seatHoldId) {
		// look up the hold
		return holds.get(seatHoldId);
	}

	@Override
	public String reserve(int seatHoldId) {

		reserveLock.lock();
		try
		{
			// remove the hold from holds
			SeatHold hold = holds.remove(seatHoldId);
			if(hold==null)
			{
				throw new HoldExpiredException(seatHoldId);
			}
			// put it in reservations
			String confirmationCode = generateConfirmationCode();
			reservations.put(confirmationCode, hold);
			return confirmationCode;
		}
		finally 
		{
			reserveLock.unlock();
		}
	}

	private String generateConfirmationCode() {
		final int confCodeSize = 20;
		final char[] charsForCOnfCode= "QWERTYUIOPASDFGHJKLZXCVBNM".toCharArray();
		StringBuilder strBuilder = new StringBuilder();
		Random rand = new Random(System.currentTimeMillis());
		for(int i=0; i<confCodeSize; i++)
		{
			strBuilder.append(charsForCOnfCode[rand.nextInt(charsForCOnfCode.length)]);
		}
		return strBuilder.toString();
	}

	@Override
	public Reservation getReservation(String confirmationCode) {
		
		return reservations.get(confirmationCode).getReservation();
	}

	@Override
	public boolean isValidHoldId(int seatHoldId) {
		return holds.containsKey(seatHoldId);
	}

	@Override
	public List<SeatHold> extractExpiredFragments() {
		long expiryTime = System.currentTimeMillis() - timeout;
		if(expiryTime<oldestHoldTimestamp.get())
		{
			// none of the holds have expired.. no op
			return new ArrayList<SeatHold>();
		}
		long newOldestHoldTimestamp = Long.MAX_VALUE;
		expireLock.lock();
		try
		{
			List<SeatHold> expired = new ArrayList<SeatHold>();
			for(SeatHold hold: holds.values())
			{
				if(hold.getTimestampHold()<expiryTime)
				{
					expired.add(hold);
				}
				else
				{
					if(newOldestHoldTimestamp>hold.getTimestampHold())
					{
						newOldestHoldTimestamp = hold.getTimestampHold();
					}
				}
			}
			for(SeatHold hold: expired) holds.remove(hold.getSeatHoldId());
			oldestHoldTimestamp.set(newOldestHoldTimestamp);

			
			return expired;
		}
		finally
		{
			expireLock.unlock();
		}
	}

}
