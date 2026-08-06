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
import dev.jcasaslopez.booking.service.WatchAlertService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
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
	
	private final WatchAlertService service;
	
	public WatchAlertController(WatchAlertService service) {
		this.service = service;
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
	@PostMapping(value=BookingEndpoints.ADD_WATCH_ALERT)
	public ResponseEntity<StandardResponse<WatchAlertResponseDto>> addWatchAlert(@RequestParam @NotNull @Positive Long idBooking) {
		logger.debug("POST /watch-alerts?idBooking={}", idBooking);
		WatchAlertResponseDto watchAlert = service.addWatchAlert(idBooking);
		
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
	@GetMapping(value=BookingEndpoints.USER_WATCH_ALERTS)
	public ResponseEntity<StandardResponse<List<WatchAlertResponseDto>>> getWatchAlertsByUser(@RequestParam @NotNull LocalDateTime startSearch, 
			@RequestParam @NotNull LocalDateTime finishSearch) {
		logger.debug("GET /watch-alerts?start={}&finish={}", startSearch, finishSearch);
		
		List<WatchAlertResponseDto> watchAlerts = service.watchAlertsListByUserAndTimePeriod(startSearch, finishSearch);
		
		StandardResponse<List<WatchAlertResponseDto>> response = new StandardResponse<>("Watch alerts retrieved successfully", watchAlerts, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
		
}
