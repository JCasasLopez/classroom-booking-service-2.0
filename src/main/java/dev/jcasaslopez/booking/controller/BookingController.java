package dev.jcasaslopez.booking.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.service.BookingService;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
public class BookingController {

	private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
	
	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping(value="/bookings", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> book(@Valid @NotNull @RequestBody BookingRequestDto booking){
		logger.info("POST /bookings - idUser={}, idClassroom={}", booking.idUser(), booking.idClassroom());
		Booking bookingConfirmed = bookingService.book(booking);
		
		String message = String.format("Classroom %s booked successfully", booking.idClassroom());
		StandardResponse response = new StandardResponse(message, bookingConfirmed, HttpStatus.CREATED);
		logger.info("Booking confirmed - idUser={}, idClassroom={}", booking.idUser(), booking.idClassroom());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@PatchMapping("/bookings/cancel")
	public ResponseEntity<StandardResponse> cancelBooking(@RequestParam @Positive Long idBooking) {
		logger.info("PATCH /bookings/cancel - idBooking={}", idBooking);
		bookingService.cancel(idBooking);
		
		String message = String.format("Booking %s cancelled successfully", idBooking);
		StandardResponse response = new StandardResponse(message, null, HttpStatus.OK);
		logger.info("Booking {} cancelled successfully", idBooking);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping(value="/bookings")
	public ResponseEntity<StandardResponse> bookingsByUser(@RequestParam @Positive int idUser){
		logger.info("GET /bookings - idUser={}", idUser);
		List<Booking> bookings = bookingService.bookingsByUser(idUser);
		
		String message = String.format("Bookings by user %s retrieved successfully", idUser);
		StandardResponse response = new StandardResponse(message, bookings, HttpStatus.OK);
		logger.info("Bookings retrieved for user {} - count={}", idUser, bookings.size());
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}