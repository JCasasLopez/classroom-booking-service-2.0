package dev.jcasaslopez.booking.domain;

import java.time.LocalTime;

public record SlotDuration(int minutes) {
	
	public SlotDuration {
        if (minutes != 30 && minutes != 60) {
            throw new IllegalArgumentException("Slot duration must be 30 or 60 minutes, but was " + minutes);
        }
    }

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