package dev.jcasaslopez.booking.domain;

import java.time.LocalDateTime;

public record BookingPeriod(LocalDateTime start, LocalDateTime finish) {

}
