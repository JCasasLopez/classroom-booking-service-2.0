package dev.jcasaslopez.booking.domain;

import java.time.LocalTime;
import java.util.Objects;

public sealed interface DaySchedule {
	
	DaySchedule CLOSED = new Closed();

    record Open(LocalTime openingTime, LocalTime closingTime) implements DaySchedule {
        public Open {
            Objects.requireNonNull(openingTime, "openingTime");
            Objects.requireNonNull(closingTime, "closingTime");

            // This validation also works for the case where closing time exceeds midnight (which is against the rules,
            // for instance opening time: 9am and closing time: 1am (next day), closing time would be before opening time.
            if (!openingTime.isBefore(closingTime)) {
            	throw new IllegalArgumentException("Invalid opening times");
            }
        }
    }

    record Closed() implements DaySchedule {}

    default boolean isOpen() {
        return this instanceof Open;
    }
}