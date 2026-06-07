package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

import dev.jcasaslopez.booking.enums.BookingStatus;

public record BookingResponseDto (long idBooking, String classroomName, LocalDateTime start, LocalDateTime finish, 
		BookingStatus status) {}