package dev.jcasaslopez.booking.dto;

import dev.jcasaslopez.booking.domain.TimeSlot;

// Represents the status of a single time slot in the weekly classroom grid.
// idBooking is null when the slot is available, or holds the associated booking ID
// when occupied — allowing the front-end to create a WatchAlert on that booking.

public record SlotStatusDto(TimeSlot timeSlot, boolean available, Long idBooking) {}