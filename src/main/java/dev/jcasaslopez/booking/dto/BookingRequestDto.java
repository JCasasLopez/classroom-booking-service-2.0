package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// Wrapper types (Integer, Boolean) are used instead of primitive types because primitives 
// cannot be null, and Jackson would automatically assign default values (e.g., false for 
// booleans) when deserializing JSON. This would prevent detecting missing fields and 
// correctly performing validations such as @NotNull.

public record BookingRequestDto (
		@NotNull(message = "idClassroom field is required") Integer idClassroom,
		// Front-end sends a list with the start time of every time slot
		@NotEmpty(message = "startTimeSlotList must contain at least one time slot") List<@NotNull LocalDateTime> startTimeSlotList
		) {}