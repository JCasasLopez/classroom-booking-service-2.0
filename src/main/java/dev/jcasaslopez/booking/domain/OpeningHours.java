package dev.jcasaslopez.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record OpeningHours(LocalTime openingTime, LocalTime closingTime) {
	
	private static final Logger logger = LoggerFactory.getLogger(OpeningHours.class);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
	
	public OpeningHours {
        if (openingTime == null && closingTime != null ||
            openingTime != null && closingTime == null) {
            throw new IllegalArgumentException("Both fields have to be null (closed) or both have values (open)");
        }
    }
	
	public boolean isOpen() {
        return openingTime != null;
    }
	
    public static OpeningHours parse(String rawOpeningTimes, DayOfWeek day) {
    	if (rawOpeningTimes.equals("CLOSED")) {
			logger.info("{}: CLOSED", day);
			return new OpeningHours(null, null);
			
    	} else if (rawOpeningTimes.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
    		String[] parts = rawOpeningTimes.split("-");
    	    LocalTime openingTime = LocalTime.parse(parts[0], TIME_FORMATTER);
    	    LocalTime closingTime = LocalTime.parse(parts[1], TIME_FORMATTER);

    	    logger.info("{}: Open from {} to {}", day, openingTime, closingTime);
    	    return new OpeningHours(openingTime, closingTime);
    	
    	} else {
    		throw new IllegalArgumentException("Invalid opening hours format: %s".formatted(rawOpeningTimes));
    	}
    	    
    }

};