package dev.jcasaslopez.booking.domain;

import java.time.LocalTime;

// Minimum bookable unit of time in the system. It defines the size of every slot,
// so all times (room availability, bookings, searches...) must be aligned with it.
// E.g. with 20-minute slots, only 10:00-10:20 or 11:20-11:40 would be valid.
// Allowed values are restricted to 30 or 60 minutes by business rules.
public record SlotDuration(int minutes) {
	
	public SlotDuration {
        if (minutes != 30 && minutes != 60) {
            throw new IllegalArgumentException("Slot duration must be 30 or 60 minutes, but was " + minutes);
        }
    }

	// This implementation is specific to the 30 and 60-minute durations allowed by business rules,
	// so that slot boundaries coincide with natural clock times: 30-minute slots align on the hour
	// and on the half hour; 60-minute slots align only on the hour.
	public boolean isAligned(LocalTime time) {
		// Specific for a slot duration of 30 or 60 min
		
		// On the hour: always aligned 
		if(time.getMinute() == 0){
			return true;
		}
		
		// On the half hour: aligned only with 30-minute slots
		if(time.getMinute() == 30 && this.minutes == 30) {
			return true;
		}
		
		// Any other time is not aligned
		return false;
	}
	
}