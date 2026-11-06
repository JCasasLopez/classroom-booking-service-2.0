package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

// Wrapper types (Integer, Boolean) are used instead of primitive types because primitives 
// cannot be null, and Jackson would automatically assign default values (e.g., false for 
// booleans) when deserializing JSON. This would prevent detecting missing fields and 
// correctly performing validations such as @NotNull.

public record BookingRequestDto (
		@NotNull(message = "idClassroom field is required") Integer idClassroom,
		@NotNull(message = "start field is required") LocalDateTime start,
		@NotNull(message = "finish field is required") LocalDateTime finish
		) {}