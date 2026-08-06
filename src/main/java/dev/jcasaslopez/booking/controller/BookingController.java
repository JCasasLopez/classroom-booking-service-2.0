package dev.jcasaslopez.booking.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.service.BookingService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import dev.jcasaslopez.classroom.shared.utility.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@CrossOrigin(origins = {"${frontend.url}"})
@Validated
@RestController
@Tag(name = "Bookings", description = "Operations for booking, cancelling and querying classroom reservations")
public class BookingController {

	private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
	
	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@Operation(
			summary = "Books a classroom for a user",
			description = """
					Creates a new booking from a list of consecutive time slots.
					Validates that the classroom exists, the slots fall within opening hours, are consecutive,
					do not exceed the maximum allowed duration, that the classroom is free for that period,
					and that the user has not reached their maximum number of weekly bookings.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Booking created successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = """
				Bad request. Possible causes:
				- Malformed or unreadable JSON body
				- Field validation failed (e.g. missing idUser/idClassroom)
				- Requested period is in the past
				- Time slots are not consecutive
				- Time slots fall outside opening hours or are invalid
				- Booking exceeds maximum allowed duration
				- Classroom is not available for the requested period
				- User has reached the maximum number of weekly bookings
				""",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid access token",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "404", description = "Classroom does not exist",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "500", description = "Internal error while serializing/deserializing JSON, or data integrity error",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping(value=BookingEndpoints.BOOK, consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse<BookingResponseDto>> book(@Valid @NotNull @RequestBody BookingRequestDto booking){
		logger.debug("POST /bookings - idUser={}, idClassroom={}", booking.idUser(), booking.idClassroom());
		BookingResponseDto bookingConfirmed = bookingService.book(booking);

		String message = String.format("Classroom %s booked successfully", booking.idClassroom());
		StandardResponse<BookingResponseDto> response = new StandardResponse<>(message, bookingConfirmed, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@Operation(
			summary = "Cancels an existing booking",
			description = """
					Cancels the booking identified by `idBooking`, if it belongs to the authenticated user and is still ACTIVE.
					For privacy reasons, a booking that does not exist and a booking that belongs to another user
					both return the same 404 response, to prevent booking ID enumeration.
					"""
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Booking cancelled successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "400", description = "idBooking is missing or not a positive number",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid access token",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "404", description = "Booking not found, or does not belong to the authenticated user",
		content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "409", description = "Booking exists but is not ACTIVE (already cancelled or completed)",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@PatchMapping(value=BookingEndpoints.CANCEL)
	public ResponseEntity<StandardResponse<Void>> cancelBooking(@RequestParam @Positive Long idBooking) {
		logger.debug("PATCH /bookings/cancel?idBooking={}", idBooking);
		bookingService.cancel(idBooking);
		
		StandardResponse<Void> response = new StandardResponse<>("Booking cancelled successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@Operation(
			summary = "Retrieves all bookings for the authenticated user",
			description = "Returns the full booking history (active, cancelled and completed) for the currently authenticated user, resolved from the security context."
			)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Bookings retrieved successfully",
				content = @Content(schema = @Schema(implementation = StandardResponse.class))),
		@ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid access token",
		content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	})
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping(value=BookingEndpoints.USER_BOOKINGS)
	public ResponseEntity<StandardResponse<List<BookingResponseDto>>> bookingsByUser(){
		int idUser = UserContext.getIdUser();
		logger.debug("GET /bookings - idUser={}", idUser);
		List<BookingResponseDto> bookings = bookingService.bookingsByUser();
		
		String message = String.format("Bookings by user %s retrieved successfully", idUser);
		StandardResponse<List<BookingResponseDto>> response = new StandardResponse<>(message, bookings, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}