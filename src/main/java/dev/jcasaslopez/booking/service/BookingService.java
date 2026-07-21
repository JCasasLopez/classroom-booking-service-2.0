package dev.jcasaslopez.booking.service;

import java.util.List;

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;

public interface BookingService {	
	BookingResponseDto book(BookingRequestDto bookingDto);
	void cancel(Long idBooking);
	List<BookingResponseDto> bookingsByUser();
	void markBookingsAsCompleted();
}