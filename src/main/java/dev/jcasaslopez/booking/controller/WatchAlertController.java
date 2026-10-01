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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.service.SearchService;
import dev.jcasaslopez.booking.service.WatchAlertService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@CrossOrigin(origins = {"${frontend.url}"})
@Validated
@RestController
@Tag(name = "Watch Alerts", description = "Operations for subscribing to notifications when a booked classroom slot becomes free")
public class WatchAlertController {

	private static final Logger logger = LoggerFactory.getLogger(WatchAlertController.class);

	private final WatchAlertService watchAlertService;
	private final SearchService searchService;

	public WatchAlertController(WatchAlertService watchAlertService, SearchService searchService) {
		this.watchAlertService = watchAlertService;
		this.searchService = searchService;
	}

	@Operation(
			summary = "Creates a watch alert for a booked slot",
			description = """
					Subscribes the authenticated user to be notified if the given booking is cancelled.
					The booking must exist and be currently ACTIVE.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Watch alert created successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = """
				Bad request. Possible causes:
				- idBooking missing or not positive
				- Booking exists but is not ACTIVE
				""",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid access token",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "404", description = "Booking not found, or its classroom no longer exists",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping(value=BookingEndpoints.WATCH_ALERTS)
	public ResponseEntity<StandardResponse<WatchAlertResponseDto>> addWatchAlert(@RequestParam @NotNull @Positive Long idBooking) {

		logger.debug("POST /watch-alerts - Creating watch alert for idBooking={}", idBooking);

		WatchAlertResponseDto watchAlert = watchAlertService.addWatchAlert(idBooking);

		StandardResponse<WatchAlertResponseDto> response = new StandardResponse<>("Watch alert created successfully", watchAlert, HttpStatus.CREATED);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// No need to pass any user information as a parameter, as the end-point needs the user to be authenticated, 
	// and the user's email is held in UserContext.
	@Operation(
			summary = "Retrieves the authenticated user's watch alerts for a time period",
			description = "Returns the watch alerts created by the authenticated user (resolved from the security context) between `startSearch` and `finishSearch`."
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Watch alerts retrieved successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = "startSearch/finishSearch missing, or startSearch is not before finishSearch",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid access token",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping(value=BookingEndpoints.WATCH_ALERTS)
	public ResponseEntity<StandardResponse<List<WatchAlertResponseDto>>> getWatchAlertsByUser(
			@RequestParam @NotNull LocalDateTime startSearch, 
			@RequestParam @NotNull LocalDateTime finishSearch) {

		logger.debug("GET /watch-alerts - Retrieving user watch alerts with startSearch={}, finishSearch={}", startSearch, finishSearch);

		List<WatchAlertResponseDto> watchAlerts = watchAlertService.watchAlertsListByUserAndTimePeriod(startSearch, finishSearch);

		StandardResponse<List<WatchAlertResponseDto>> response = new StandardResponse<>("Watch alerts retrieved successfully", watchAlerts, HttpStatus.OK);

		return ResponseEntity.ok(response);
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
	@GetMapping(value=BookingEndpoints.TARGET_BOOKING)
	public ResponseEntity<StandardResponse<Long>> bookingBySlot(
			@RequestParam @NotNull LocalDateTime start,
			@RequestParam @NotNull LocalDateTime finish,
			@RequestParam @Positive int idClassroom){

		logger.debug("GET /watch-alerts/target-booking - Retrieving target booking for classroom={}, start={}, finish={}", 
				idClassroom, start, finish);

		Long idbooking = searchService.findBookingByClassroomAndTimePeriod(idClassroom, start, finish);

		String message = String.format("Active booking for classroom %s between %s and %s retrieved successfully", idClassroom, start, finish);
		StandardResponse<Long> response = new StandardResponse<>(message, idbooking, HttpStatus.OK);

		return ResponseEntity.ok(response);
	}		
}