package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.BookingPeriod;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;

@Component
public class BookingValidator {
	
	private final int slotDuration; 
	private final int bookingMaxDuration; 
	private final int maxNumberBookings;
	private final WeeklySchedule weeklySchedule;
	private final BookingRepository bookingRepository;
	private final ClassroomValidator classroomValidator;
	
	public BookingValidator(@Value("${time-slot.duration}") int slotDuration, 
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

	public BookingPeriod validateBooking(BookingRequestDto booking, int idUser) {
		classroomValidator.validateClassroomExists(booking.idClassroom());

		List<LocalDateTime> sortedStartTimes = booking.startTimeSlotList().stream()
		        .sorted()
		        .toList();

		// The list of  start time slots in BookingRequestDto is annotated with @NotEmpty, so this line is safe
		LocalDateTime bookingStart = sortedStartTimes.get(0);
		LocalDateTime bookingFinish = sortedStartTimes.get(sortedStartTimes.size()-1).plusMinutes(slotDuration);
		
		checkTimeStartTimesAreValid(sortedStartTimes, weeklySchedule, slotDuration);
		
		if(bookingStart.isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Booking a past period is not allowed");
		}
		
		checkSlotsAreConsecutive(sortedStartTimes);
		checkBookDoesNotExceedMaxAllowedTime(sortedStartTimes);
		checkClassroomHasNoOverlappingBookings(booking.idClassroom(), bookingStart, bookingFinish);
		checkUserHasBookingsLeft(idUser, bookingStart);
		
		return new BookingPeriod(bookingStart, bookingFinish);
	}
	
	private void checkTimeStartTimesAreValid(List<LocalDateTime> startTimes,  WeeklySchedule weeklySchedule, int slotDuration) {
	    startTimes.forEach(start -> SlotValidator.validate(start, weeklySchedule, slotDuration));
	}
	
	private void checkSlotsAreConsecutive(List<LocalDateTime> listSlots) {
		for(int i=0; i < listSlots.size() - 1; i++) {
			if(!listSlots.get(i).plusMinutes(slotDuration).equals(listSlots.get(i+1))) {
				throw new InvalidBookingException("Booking slots are not consecutive");
			}
		}
	}
	
	private void checkBookDoesNotExceedMaxAllowedTime(List<LocalDateTime> listSlots) {
		int intendedBookingDuration = listSlots.size() * slotDuration;
		if(intendedBookingDuration >  bookingMaxDuration) {
			throw new InvalidBookingException("Booking exceeds maximum duration allowed");
		}
	}
	
	private void checkClassroomHasNoOverlappingBookings(int idClassroom, LocalDateTime start, LocalDateTime finish) {
		if(bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish).size() != 0) {
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