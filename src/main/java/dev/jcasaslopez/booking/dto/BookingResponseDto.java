package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

import dev.jcasaslopez.booking.enums.BookingStatus;

public record BookingResponseDto (String name, LocalDateTime start, LocalDateTime finish, BookingStatus status) {}