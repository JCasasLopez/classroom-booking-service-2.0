package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

// Represents the status of a single time slot in the weekly classroom grid.
// idBooking is null when the slot is available, or holds the associated booking ID
// when occupied — allowing the front-end to create a WatchAlert on that booking.

public record SlotStatusDto(LocalDateTime start, LocalDateTime finish, boolean available, Long idBooking) {}