package dev.jcasaslopez.booking.validator;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.domain.BookingPeriod;
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
	
	private static final int SLOT_DURATION = 30;
	private static final int BOOKING_MAX_DURATION = 120;
	private static final int MAX_NUMBER_BOOKINGS = 1;
	
	private static final LocalDateTime START = nextMondayAt9();
	private static final LocalDateTime SLOT_2 = START.plusMinutes(SLOT_DURATION);
	private static final LocalDateTime SLOT_3 = START.plusMinutes(SLOT_DURATION*2);
	
	private static final int USER_ID = 1;
	private static final int CLASSROOM_ID = 1;
	
	private BookingValidator bookingValidator;
	
	private WeeklySchedule buildTestWeeklySchedule() {
	    List<String> hours = new ArrayList<> (List.of("09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "10:00-14:00", "CLOSED"));
	    return new WeeklySchedule(hours);
	}
	
	@BeforeEach
	void setUp() {
		bookingValidator = new BookingValidator(SLOT_DURATION, BOOKING_MAX_DURATION, MAX_NUMBER_BOOKINGS, 
												buildTestWeeklySchedule(), bookingRepository, classroomValidator);
	}
	
	@Test
	void happy_path_does_not_throw_exceptions() {
		// Arrange
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2));

		// validateClassroomExists is void: it throws if the classroom does not exist and does nothing otherwise.
		// Mockito mocks already "do nothing" by default, so the tests below only need to stub it
		// when they want it to throw.
		doNothing().when(classroomValidator).validateClassroomExists(CLASSROOM_ID);
		
		// Act & Assert
		assertDoesNotThrow(() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
	}
	
	@Test
	void happy_path_returns_correct_BookingPeriod() {
		// Arrange
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2));
		
		// Act & Assert
		BookingPeriod bookingPeriod = bookingValidator.validateBooking(bookingRequest, USER_ID);	
		assertAll(() -> assertEquals(START, bookingPeriod.start()),
				() -> assertEquals(SLOT_2.plusMinutes(SLOT_DURATION), bookingPeriod.finish())
				);
	}
	
	@Test
	void if_slots_are_not_sorted_the_flow_follows_happy_path() {
		// Arrange
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, List.of(SLOT_3, SLOT_2, START));

		// Act & Assert
		assertDoesNotThrow(() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	
	}

	@Test
	void if_the_booking_in_the_past_throws_exception() {
		// Arrange
		LocalDateTime pastStart = LocalDateTime.of(2026, 5, 4, 9, 0);
		LocalDateTime pastSlot2 = LocalDateTime.of(2026, 5, 4, 10, 0);
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, List.of(pastStart, pastSlot2));
		
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
											() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	
		assertEquals("Booking a past period is not allowed", ex.getMessage());
	}
	
	@Test
	void if_the_booking_slots_are_not_consecutive_throws_exception() {
		// Arrange
		// Skips SLOT 2, so slots are not consecutive
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_3));
				
		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
											() -> bookingValidator.validateBooking(request, USER_ID));	
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException		
		assertEquals("Booking slots are not consecutive", ex.getMessage());
	}
	
	@Test
	void if_the_classroom_is_not_available_throws_exception() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2, SLOT_3));
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
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2, SLOT_3));
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
		LocalDateTime SLOT_4 = START.plusMinutes(SLOT_DURATION*3);
		BookingRequestDto bookingRequest = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2, SLOT_3, SLOT_4));

		// Act & Assert
		assertDoesNotThrow(() -> bookingValidator.validateBooking(bookingRequest, USER_ID));	

	}
	
	@Test
	void if_the_booking_exceeds_maximum_length_throws_exception() {
		// Arrange
		LocalDateTime SLOT_4 = START.plusMinutes(SLOT_DURATION*3);
		LocalDateTime SLOT_5 = START.plusMinutes(SLOT_DURATION*4);
		
		// 5 slots -> 150' exceed the maximum length set of 120'
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, List.of(START, SLOT_2, SLOT_3, SLOT_4, SLOT_5));

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, 
											() -> bookingValidator.validateBooking(request, USER_ID));	

		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertEquals("Booking exceeds maximum duration allowed", ex.getMessage());
	}
	
	@Test
	void the_weekly_limit_is_checked_against_monday_midnight_and_the_following_monday() {
		// Arrange: a Wednesday booking, so Monday is not the day of the booking itself
		LocalDateTime wednesday = START.plusDays(2);
		BookingRequestDto request = new BookingRequestDto(CLASSROOM_ID, List.of(wednesday, wednesday.plusMinutes(SLOT_DURATION)));
		when(bookingRepository.findActiveBookingsForClassroomByPeriod(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(List.of());
		when(bookingRepository.countBookingsByUserInPeriod(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(0L);
		doNothing().when(classroomValidator).validateClassroomExists(CLASSROOM_ID);

		// Act
		bookingValidator.validateBooking(request, USER_ID);

		// Assert
		ArgumentCaptor<LocalDateTime> weekStart = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> nextWeekStart = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(bookingRepository).countBookingsByUserInPeriod(eq(USER_ID), weekStart.capture(), nextWeekStart.capture());

		LocalDateTime expectedMonday = START.toLocalDate().atStartOfDay();
		assertAll(
				() -> assertEquals(expectedMonday, weekStart.getValue()),
				() -> assertEquals(expectedMonday.plusDays(7), nextWeekStart.getValue())
		);
	}

}
