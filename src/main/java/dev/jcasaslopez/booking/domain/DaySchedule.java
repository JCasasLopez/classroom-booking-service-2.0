package dev.jcasaslopez.booking.domain;

import java.time.LocalTime;
import java.util.Objects;

public sealed interface DaySchedule {
	
	DaySchedule CLOSED = new Closed();

    record Open(LocalTime openingTime, LocalTime closingTime) implements DaySchedule {
        public Open {
            Objects.requireNonNull(openingTime, "openingTime");
            Objects.requireNonNull(closingTime, "closingTime");
            if (!closingTime.isAfter(openingTime)) {
                throw new IllegalArgumentException("Closing time must be after opening time");
            }
        }
    }

    record Closed() implements DaySchedule {}

    default boolean isOpen() {
        return this instanceof Open;
    }
}