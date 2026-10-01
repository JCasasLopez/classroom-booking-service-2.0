package dev.jcasaslopez.booking.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.service.SearchService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@CrossOrigin(origins = {"${frontend.url}"})
@Validated
@RestController
@Tag(name = "Search", description = "Operations for querying classroom availability and existing bookings")	
public class SearchController {

	private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@Operation(
			summary = "Retrieves the availability calendar for a classroom",
			description = """
					Returns the time-slot grid (booked/free) for the given classroom between `start` and `finish`.
					Both must fall on the same day and within opening hours.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Availability calendar retrieved successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = """
				Bad request. Possible causes:
				- idClassroom missing or not positive
				- start/finish missing
				- start is not before finish
				- Search range is in the past
				- start and finish are not on the same day
				- Range falls outside opening hours, or the center is closed that day
				""",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "404", description = "Classroom does not exist",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@GetMapping(value=BookingEndpoints.CLASSROOM_AVAILABILITY)
	public ResponseEntity<StandardResponse<List<SlotStatusDto>>> availabilityCalendar(
			@PathVariable @Positive int idClassroom,
			@RequestParam @NotNull LocalDateTime start,
			@RequestParam @NotNull LocalDateTime finish) {
		logger.debug("GET /classrooms/{}/availability - Retrieving availability calendar with start={}, finish={}", idClassroom, start, finish);

		List<SlotStatusDto> availabilityCalendar = searchService.availabilityCalendarByClassroom(idClassroom, start, finish);

		String message = String.format("Availability calendar for classroom %s retrieved successfully", idClassroom);
		StandardResponse<List<SlotStatusDto>> response = new StandardResponse<>(message, availabilityCalendar, HttpStatus.OK);

		return ResponseEntity.ok(response);
	}

	@Operation(
			summary = "Retrieves classrooms available for a period, optionally filtered by features",
			description = """
					Returns all classrooms with no active booking overlapping `start`–`finish`.
					Filters are optional: set `seats` to 0 to skip the seats filter, and `projector`/`speakers` to false to skip those filters.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Available classrooms retrieved successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = """
				Bad request. Possible causes:
				- start/finish/projector/speakers missing
				- start is not before finish
				- Search range is in the past
				- start and finish are not on the same day
				- Range falls outside opening hours, or the center is closed that day
				""",
				content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@GetMapping(value=BookingEndpoints.CLASSROOMS_AVAILABLE)
	public ResponseEntity<StandardResponse<List<ClassroomEvent>>> classroomsAvailable(
			@RequestParam @NotNull LocalDateTime start,
			@RequestParam @NotNull LocalDateTime finish,
			// If you do not want to filter by seats, set at 0.
			@RequestParam @PositiveOrZero int seats,
			@RequestParam @NotNull Boolean projector,
			@RequestParam @NotNull Boolean speakers) {
		logger.debug("GET /classrooms/available - Searching available classrooms with start={}, finish={}, seats={}, projector={}, speakers={}", 
				start, finish, seats, projector, speakers);

		List<ClassroomEvent> classroomsAvailableByPeriod = searchService.classroomsAvailableByPeriodAndFeatures(start, finish, seats, projector, speakers);

		String message = String.format("Available classrooms between %s and %s (seats: %s - projector: %s - speakers: %s) retrieved successfully", 
				start, finish, seats, projector, speakers);
		StandardResponse<List<ClassroomEvent>> response = new StandardResponse<>(message, classroomsAvailableByPeriod, HttpStatus.OK);

		return ResponseEntity.ok(response);
	}			
}