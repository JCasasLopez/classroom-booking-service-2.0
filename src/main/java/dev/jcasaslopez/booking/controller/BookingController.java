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

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.service.BookingService;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import dev.jcasaslopez.classroom.shared.utility.UserContext;
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

	@PostMapping(value=Endpoints.BOOK, consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse<BookingResponseDto>> book(@Valid @NotNull @RequestBody BookingRequestDto booking){
		logger.debug("POST /bookings - idUser={}, idClassroom={}", booking.idUser(), booking.idClassroom());
		BookingResponseDto bookingConfirmed = bookingService.book(booking);
		
		String message = String.format("Classroom %s booked successfully", booking.idClassroom());
		StandardResponse<BookingResponseDto> response = new StandardResponse<>(message, bookingConfirmed, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@PatchMapping(value=Endpoints.CANCEL)
	public ResponseEntity<StandardResponse<Void>> cancelBooking(@RequestParam @Positive Long idBooking) {
		logger.debug("PATCH /bookings/cancel?idBooking={}", idBooking);
		bookingService.cancel(idBooking);
		
		String message = String.format("Booking %s cancelled successfully", idBooking);
		StandardResponse<Void> response = new StandardResponse<>(message, null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	@GetMapping(value=Endpoints.USER_BOOKINGS)
	public ResponseEntity<StandardResponse<List<BookingResponseDto>>> bookingsByUser(){
		int idUser = UserContext.getIdUser();
		logger.debug("GET /bookings - idUser={}", idUser);
		List<BookingResponseDto> bookings = bookingService.bookingsByUser();
		
		String message = String.format("Bookings by user %s retrieved successfully", idUser);
		StandardResponse<List<BookingResponseDto>> response = new StandardResponse<>(message, bookings, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}