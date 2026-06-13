package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

// Flat Data Transfer Object (DTO) representing a time slot for network transport.
// This DTO decouples the public API contract from the internal 'TimeSlot' domain model.
// By using flat primitive types, it prevents Jackson serialization/deserialization failures 
// in external clients or test environments that lack domain-specific dependencies 
// and business rules configuration (e.g., WeeklySchedule).
public record TimeSlotDto(LocalDateTime start, LocalDateTime finish) {}