package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public interface SearchService {
	
	// Returns a list of SlotStatusDto, that represents the status (Available/Booked) of a single time slot in the weekly classroom grid.
	List<SlotStatusDto> availabilityCalendarByClassroom(int idClassroom, LocalDateTime start, LocalDateTime finish);
	List<ClassroomEvent> classroomsAvailableByPeriod(LocalDateTime start, LocalDateTime finish);
	List<ClassroomEvent> classroomsAvailableByPeriodAndFeatures(LocalDateTime start, LocalDateTime finish,
																int seats, boolean projector, boolean speakers);
}