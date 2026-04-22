package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

public record BookingResponseDto (String name, LocalDateTime start, LocalDateTime finish) {}