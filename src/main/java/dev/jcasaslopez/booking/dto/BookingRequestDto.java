package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;

// Wrapper types (Integer, Boolean) are used instead of primitive types because primitives 
// cannot be null, and Jackson would automatically assign default values (e.g., false for 
// booleans) when deserializing JSON. This would prevent detecting missing fields and 
// correctly performing validations such as @NotNull.

public record BookingRequestDto (
		@NotNull(message = "idUser field is required") Integer idUser, 
		@NotNull(message = "idClassroom field is required") Integer idClassroom,
		// Front-end sends a list with the start time of every time slot
		@NotNull(message = "startTimeSlotList field is required") List<LocalDateTime> startTimeSlotList
		) {}