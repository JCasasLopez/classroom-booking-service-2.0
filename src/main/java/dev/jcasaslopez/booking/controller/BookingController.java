package dev.jcasaslopez.booking.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
public class BookingController {

	private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
	private BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping(value="/bookings", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> book(@Valid @RequestBody BookingRequestDto booking){
		logger.info("POST /bookings - idUser={}, idClassroom={}", booking.idUser(), booking.idClassroom());
		bookingService.book(booking);
		StandardResponse response = new StandardResponse("Classroom booked successfully", null, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@PatchMapping("/bookings/cancel")
	public ResponseEntity<StandardResponse> cancelBooking(@RequestParam Long idBooking) {
		logger.info("PATCH /bookings/cancel - idBooking={}", idBooking);
		bookingService.cancel(idBooking);
		StandardResponse response = new StandardResponse("Booking cancelled successfully", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping(value="/bookings")
	public ResponseEntity<StandardResponse> bookingsByUser(@RequestParam int idUser){
		logger.info("GET /bookings - idUser={}", idUser);
		List<Booking> bookings = bookingService.bookingsByUser(idUser);
		StandardResponse response = new StandardResponse(LocalDateTime.now(), "List of bookings by user retrieved successfully", bookings, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
