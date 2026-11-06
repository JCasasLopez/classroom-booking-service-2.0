package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;

@Component
public class BookingValidator {
	
	private final SlotDuration slotDuration; 
	private final int bookingMaxDuration; 
	private final int maxNumberBookings;
	private final WeeklySchedule weeklySchedule;
	private final BookingRepository bookingRepository;
	private final ClassroomValidator classroomValidator;
	
	public BookingValidator(SlotDuration slotDuration, 
			@Value("${booking.maximum-duration}") int bookingMaxDuration, 
			@Value("${booking.maximum-number-per-week}") int maxNumberBookings,
			WeeklySchedule weeklySchedule, 
			BookingRepository bookingRepository, 
			ClassroomValidator classroomValidator) {
		this.slotDuration = slotDuration;
		this.bookingMaxDuration = bookingMaxDuration;
		this.maxNumberBookings = maxNumberBookings;
		this.weeklySchedule = weeklySchedule;
		this.bookingRepository = bookingRepository;
		this.classroomValidator = classroomValidator;
	}

	public void validateBooking(BookingRequestDto booking, int idUser) {
		classroomValidator.validateClassroomExists(booking.idClassroom());
	
		LocalDateTime bookingStart = booking.start();
		LocalDateTime bookingFinish = booking.finish();
		
		if(bookingStart.isBefore(LocalDateTime.now())) {
			throw new InvalidBookingException("Booking a past period is not allowed");
		}
		
		if(!bookingFinish.isAfter(bookingStart)) {
			throw new InvalidBookingException("Finish must be after start");
		}
		
		// Validates alignment with the grid and the time points fall within opening hours
		TimePointValidator.validate(bookingStart, weeklySchedule, slotDuration);
		TimePointValidator.validate(bookingFinish, weeklySchedule, slotDuration);
		
		checkBookDoesNotExceedMaxAllowedTime(bookingStart, bookingFinish);
		checkClassroomHasNoOverlappingBookings(booking.idClassroom(), bookingStart, bookingFinish);
		checkUserHasBookingsLeft(idUser, bookingStart);
	}

	private void checkBookDoesNotExceedMaxAllowedTime(LocalDateTime start, LocalDateTime finish) {
		Duration bookingDuration = Duration.between(start, finish);
		if(bookingDuration.toMinutes() >  bookingMaxDuration) {
			throw new InvalidBookingException("Booking exceeds maximum duration allowed");
		}
	}
	
	private void checkClassroomHasNoOverlappingBookings(int idClassroom, LocalDateTime start, LocalDateTime finish) {
		if(!bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish).isEmpty()) {
			throw new InvalidBookingException("Classroom is not available for this time period");
		}
	}
	
	private void checkUserHasBookingsLeft(int idUser, LocalDateTime start) {		
	    LocalDateTime weekStart = start
	        .with(DayOfWeek.MONDAY)
	        .withHour(0).withMinute(0).withSecond(0).withNano(0);
	    
	    LocalDateTime nextWeekStart = weekStart.plusDays(7);

	    long bookingsThatWeek = bookingRepository.countBookingsByUserInPeriod(idUser, weekStart, nextWeekStart);

	    if (bookingsThatWeek >= maxNumberBookings) {
	        throw new InvalidBookingException("User has reached the maximum number of weekly bookings");
	    }
	}
	
}