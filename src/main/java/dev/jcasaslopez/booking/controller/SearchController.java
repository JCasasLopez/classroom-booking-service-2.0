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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
	@GetMapping(value=Endpoints.AVAILABILITY_CALENDAR)
	public ResponseEntity<StandardResponse<List<SlotStatusDto>>> availabilityCalendar(
			@RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        @RequestParam @Positive int idClassroom) {
		logger.debug("GET /searches/availability-calendar?idClassroom={}&start={}&finish={}", idClassroom, start, finish);	
		List<SlotStatusDto> availabilityCalendar = searchService.availabilityCalendarByClassroom(idClassroom, start, finish);
		String message = String.format("Availability calendar for classroom %s retrieved successfully", idClassroom);
		StandardResponse<List<SlotStatusDto>> response = new StandardResponse<>(message, availabilityCalendar, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
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
	@GetMapping(value=Endpoints.CLASSROOMS_AVAILABILITY)
	public ResponseEntity<StandardResponse<List<ClassroomEvent>>> classroomsAvailable(
	        @RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        // If you do not want to filter by seats, set at 0.
	        @RequestParam int seats,
	        @RequestParam @NotNull Boolean projector,
	        @RequestParam @NotNull Boolean speakers) {
		logger.debug("GET /searches/classrooms-available?start={}&finish={}&seats={}&projector={}&speakers={}", start, finish, seats, projector, speakers);
		List<ClassroomEvent> classroomsAvailableByPeriod = searchService.classroomsAvailableByPeriodAndFeatures(start, finish, seats, projector, speakers);
		String message = String.format("Available classrooms between %s and %s (seats: %s - projector: %s - speakers: %s) retrieved successfully", 
				start, finish, seats, projector, speakers);
		StandardResponse<List<ClassroomEvent>> response = new StandardResponse<>(message, classroomsAvailableByPeriod, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	// When creating a watch alert, user hits an already booked time slot on the front-end. This endpoint returns the idBooking
	// corresponding to that booking, which is the parameter needed to create a watch alert.
	@Operation(
			summary = "Retrieves the booking ID occupying a given time slot",
			description = """
					Returns the `idBooking` of the active booking that occupies the exact slot `start`–`finish` for the given classroom.
					Used by the front-end when a user clicks an already-booked slot, so a watch alert can be created for that booking.
					`finish - start` must match exactly the configured minimum time-slot duration.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Booking id retrieved successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = """
				Bad request. Possible causes:
				- idClassroom missing or not positive
				- start/finish missing
				- start is not before finish
				- Search range is in the past
				- start and finish are not on the same day
				- Range falls outside opening hours, or the center is closed that day
				- finish - start does not match exactly the minimum time-slot duration
				""",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "404", description = "No active booking found for that classroom and time slot",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "500", description = "Data integrity error – more than one active booking found for the same slot",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@GetMapping(value=Endpoints.BOOKING_BY_SLOT)
	public ResponseEntity<StandardResponse<Long>> bookingBySlot(
			@RequestParam @NotNull LocalDateTime start,
	        @RequestParam @NotNull LocalDateTime finish,
	        @RequestParam @Positive int idClassroom){
		logger.debug("GET /searches/booking-by-slot?idClassroom={}&start={}&finish={}", idClassroom, start, finish);	
		Long idbooking = searchService.findBookingByClassroomAndTimePeriod(idClassroom, start, finish);
		String message = String.format("Active booking for classroom %s between %s and %s retrieved successfully", idClassroom, start, finish);
		StandardResponse<Long> response = new StandardResponse<>(message, idbooking, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
			
}