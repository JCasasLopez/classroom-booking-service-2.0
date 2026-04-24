package dev.jcasaslopez.booking.dto;

import jakarta.validation.constraints.NotNull;

public record WatchAlertRequestDto (
		@NotNull(message = "idBooking field is required") Long idBooking) {}