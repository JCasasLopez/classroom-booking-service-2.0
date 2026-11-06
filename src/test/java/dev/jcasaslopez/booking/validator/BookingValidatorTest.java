package dev.jcasaslopez.booking.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
public class BookingValidatorTest {
	
	@Mock BookingRepository bookingRepository;
	@Mock ClassroomValidator classroomValidator;
	
	private static LocalDateTime nextMondayAt9() {
	    LocalDate today = LocalDate.now();
	    LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	    return nextMonday.atTime(9, 0);
	}
	
	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	private static final int SLOT_DURATION_IN_MINUTES = SLOT_DURATION_30.minutes();
	
	private static final int BOOKING_MAX_DURATION = 120;
	private static final int MAX_NUMBER_BOOKINGS = 1;
	
	private static final LocalDateTime START_AT_9 = nextMondayAt9();
	private static final LocalDateTime FINISH_AT_10_30 = START_AT_9.plusMinutes(SLOT_DURATION_IN_MINUTES*3);
	
	private static final int USER_ID = 1;
	private static final int CLASSROOM_ID = 1;
	
	private BookingValidator bookingValidator;
	
	private WeeklySchedule buildTestWeeklySchedule() {
		Map<DayOfWeek, String> hours = Map.of(
				DayOfWeek.MONDAY, "09:00-22:00",
				DayOfWeek.TUESDAY, "09:00-22:00",
				DayOfWeek.WEDNESDAY, "09:00-22:00",
				DayOfWeek.THURSDAY, "09:00-22:00",
				DayOfWeek.FRIDAY, "09:00-22:00",
				DayOfWeek.SATURDAY, "10:00-14:00",
				DayOfWeek.SUNDAY, "CLOSED");
		return WeeklySchedule.from(hours, SLOT_DURATION_30);
	}
	
	@BeforeEach
	void setUp() {
		bookingValidator = new BookingValidator(SLOT_DURATION_30, BOOKING_MAX_DURATION, MAX_NUMBER_BOOKINGS, 
												buildTestWeeklySchedule(), bookingRepository, classroomValidator);
	}
	
	@Test
	void happy_path_does_not_throw_exceptions() {
		// Arrange
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, START_AT_9, FINISH_AT_10_30);

		// validateClassroomExists is void: it throws if the classroom does not exist and does nothing otherwise.
		// Mockito mocks already "do nothing" by default, so the tests below only need to stub it
		// when they want it to throw.
		doNothing().when(classroomValidator).validateClassroomExists(CLASSROOM_ID);
		
		// Act & Assert
		assertDoesNotThrow(() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
	}
	

	@Test
	void if_the_booking_in_the_past_throws_exception() {
		// Arrange
		LocalDateTime pastStart = LocalDateTime.of(2026, 5, 4, 9, 0);
		LocalDateTime pastFinish = LocalDateTime.of(2026, 5, 4, 10, 00);
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, pastStart, pastFinish);
		
		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
											() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	
		assertEquals("Booking a past period is not allowed", ex.getMessage());
	}
	
	
	@Test
	void if_the_classroom_is_not_available_throws_exception() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, START_AT_9, FINISH_AT_10_30);
		when(bookingRepository.findActiveBookingsForClassroomByPeriod(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
							.thenReturn(List.of(new Booking())); // List is not empty -> Classroom is not available

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
												() -> bookingValidator.validateBooking(request, USER_ID));	
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertEquals("Classroom is not available for this time period", ex.getMessage());
	}

	@Test
	void if_the_user_has_no_bookings_left_throws_exception() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, START_AT_9, FINISH_AT_10_30);
		when(bookingRepository.countBookingsByUserInPeriod(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);		
		
		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
										() -> bookingValidator.validateBooking(request, USER_ID));	
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertEquals("User has reached the maximum number of weekly bookings", ex.getMessage());
	}
	
	@Test
	void if_the_booking_is_exactly_the_maximum_length_does_not_throw_exception() {
		// Arrange
		LocalDateTime FINISH_AT_11 = START_AT_9.plusMinutes(SLOT_DURATION_IN_MINUTES*3);
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, START_AT_9, FINISH_AT_11);

		// Act & Assert
		assertDoesNotThrow(() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	

	}
	
	@Test
	void if_the_booking_exceeds_maximum_length_throws_exception() {
		// Arrange
		LocalDateTime FINISH_AT_11_30 = START_AT_9.plusMinutes(SLOT_DURATION_IN_MINUTES*5);

		
		// 5 slots -> 150' exceed the maximum length set of 120'
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, START_AT_9, FINISH_AT_11_30);

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
											() -> bookingValidator.validateBooking(request, USER_ID));	

		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertEquals("Booking exceeds maximum duration allowed", ex.getMessage());
	}
}