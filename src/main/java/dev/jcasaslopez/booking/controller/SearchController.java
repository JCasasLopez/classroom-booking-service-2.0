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

	@GetMapping(value="/searches/availability-calendar")
	public ResponseEntity<StandardResponse> availabilityCalendar(
			@RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        @RequestParam @Positive int idClassroom) {
		logger.debug("GET /searches/availability-calendar - idClassroom={}, start={}, finish={}", idClassroom, start, finish);
		
		if (!start.isBefore(finish)) {
	        throw new IllegalArgumentException("start must be before finish");
	    }
		
		List<SlotStatusDto> classroomsAvailable = searchService.availabilityCalendarByClassroom(idClassroom, start, finish);
		String message = String.format("Availability calendar for classroom %s retrieved successfully", idClassroom);
		StandardResponse response = new StandardResponse (message, classroomsAvailable, HttpStatus.OK);
		logger.info("Availability calendar retrieved for classroom {}", idClassroom);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping(value="/searches/classrooms-available")
	public ResponseEntity<StandardResponse> classroomsAvailable(
	        @RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        // If you do not want to filter by seats, set at 0.
	        @RequestParam int seats,
	        @RequestParam @NotNull boolean projector,
	        @RequestParam @NotNull boolean speakers) {
		logger.debug("GET /searches/classrooms-available - start={}, finish={}, seats={}, projector={}, speakers={}", start, finish, seats, projector, speakers);
		
		if (!start.isBefore(finish)) {
	        throw new IllegalArgumentException("start must be before finish");
	    }
		
		List<ClassroomEvent> classroomsAvailableByPeriod = searchService.classroomsAvailableByPeriodAndFeatures(start, finish, seats, projector, speakers);
		String message = String.format("Available classrooms between %s and %s retrieved successfully", start, finish);
		StandardResponse response = new StandardResponse (message, classroomsAvailableByPeriod, HttpStatus.OK);
		logger.info("Classrooms available retrieved - count={}", classroomsAvailableByPeriod.size());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}