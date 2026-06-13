package dev.jcasaslopez.booking.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.service.SearchService;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
public class SearchController {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
	
	private final SearchService searchService;
	
	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@GetMapping(value=Endpoints.AVAILABILITY_CALENDAR)
	public ResponseEntity<StandardResponse<List<SlotStatusDto>>> availabilityCalendar(
			@RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        @RequestParam @Positive int idClassroom) {
		logger.debug("GET /searches/availability-calendar - idClassroom={}, start={}, finish={}", idClassroom, start, finish);	
		List<SlotStatusDto> availabilityCalendar = searchService.availabilityCalendarByClassroom(idClassroom, start, finish);
		String message = String.format("Availability calendar for classroom %s retrieved successfully", idClassroom);
		StandardResponse<List<SlotStatusDto>> response = new StandardResponse<>(message, availabilityCalendar, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping(value=Endpoints.CLASSROOMS_AVAILABILITY)
	public ResponseEntity<StandardResponse<List<ClassroomEvent>>> classroomsAvailable(
	        @RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        // If you do not want to filter by seats, set at 0.
	        @RequestParam int seats,
	        @RequestParam @NotNull boolean projector,
	        @RequestParam @NotNull boolean speakers) {
		logger.debug("GET /searches/classrooms-available - start={}, finish={}, seats={}, projector={}, speakers={}", start, finish, seats, projector, speakers);
		List<ClassroomEvent> classroomsAvailableByPeriod = searchService.classroomsAvailableByPeriodAndFeatures(start, finish, seats, projector, speakers);
		String message = String.format("Available classrooms between %s and %s (seats: %s - projector: %s - speakers: %s) retrieved successfully", 
				start, finish, seats, projector, speakers);
		StandardResponse<List<ClassroomEvent>> response = new StandardResponse<>(message, classroomsAvailableByPeriod, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}