package dev.jcasaslopez.booking.dto;

import java.time.LocalDateTime;

public record WatchAlertResponseDto(String classroomName, LocalDateTime start, LocalDateTime finish) {}
