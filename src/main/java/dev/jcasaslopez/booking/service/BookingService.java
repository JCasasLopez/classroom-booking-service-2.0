package dev.jcasaslopez.booking.service;

import java.util.List;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.enums.BookingStatus;

public interface BookingService {	
	Booking book(BookingRequestDto bookingDto);
	void cancel(Long idBooking, BookingStatus bookingStatus);
	List<Booking> bookingsByUser(int idUser);
	void markBookingsAsCompleted();
}