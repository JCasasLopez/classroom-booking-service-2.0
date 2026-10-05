package dev.jcasaslopez.booking.service;

import java.util.List;

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;

public interface BookingService {	
	BookingResponseDto book(BookingRequestDto bookingDto, int idUser, String email);
	void cancel(Long idBooking, int idUser, String email);
	List<BookingResponseDto> bookingsByUser(int idUser);
	void markBookingsAsCompleted();
}