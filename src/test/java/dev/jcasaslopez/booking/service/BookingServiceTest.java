package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

// NOTE: time slot validity (opening hours, slot alignment) is tested in TimeSlotTest. No need to duplicate those tests here.
@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
	
	@Mock BookingRepository bookingRepository;
	@Mock WatchAlertRepository watchAlertRepository;
	@Mock WeeklySchedule weeklySchedule;
	@Mock ClassroomValidator classroomValidator;
	@Mock EventPublisher eventPublisher;
	@InjectMocks BookingServiceImpl bookingService;
	
	private static final int USER_ID = 1;
	private static final int CLASSROOM_ID = 1;
	private static final String USER_EMAIL = "test@gmail.com";
	private static final LocalDateTime START = LocalDateTime.of(2026, 5, 4, 9, 0);
	private static final LocalDateTime SLOT_2 = LocalDateTime.of(2026, 5, 4, 9, 30);
	private static final LocalDateTime SLOT_3 = LocalDateTime.of(2026, 5, 4, 10, 0);
	private static final LocalDateTime EXPECTED_FINISH = SLOT_3.plusMinutes(30);
	
	private WeeklySchedule buildTestWeeklySchedule() {
	    List<String> hours = new ArrayList<> (List.of("09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "10:00-14:00", "CLOSED"));
	    return new WeeklySchedule(hours);
	}
	
	// Must run AFTER Mockito has injected the mocks (i.e. not at field-initialization time),
	// otherwise bookingService would still be null.
	@BeforeEach
	void setUp() {
	    ReflectionTestUtils.setField(bookingService, "slotDuration", 30);
	    ReflectionTestUtils.setField(bookingService, "bookingMaxDuration", 120);
	    ReflectionTestUtils.setField(bookingService, "maxNumberBookings", 3);
	    ReflectionTestUtils.setField(bookingService, "weeklySchedule", buildTestWeeklySchedule());
	    UserContext.setEmail(USER_EMAIL);
	}
	
	@Test
	void if_the_booking_is_valid_persists_the_correct_booking_entity() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, 
															new ArrayList<> (List.of(START, SLOT_2, SLOT_3)));

		// Act
		Booking booking = bookingService.book(request);
		
		// Assert
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.BOOKING_CONFIRMED, booking, USER_EMAIL);	
		
		ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
		verify(bookingRepository).save(bookingCaptor.capture());
		Booking savedBooking = bookingCaptor.getValue();
		
		assertAll(
				() -> assertEquals(USER_ID, savedBooking.getIdUser()),
				() -> assertEquals(CLASSROOM_ID, savedBooking.getIdClassroom()),
				() -> assertEquals(START, savedBooking.getStart()),
				() -> assertEquals(EXPECTED_FINISH, savedBooking.getFinish()),
				() -> assertEquals(BookingStatus.ACTIVE, savedBooking.getStatus())
				);
	}
	
	@Test
	void if_the_booking_slots_are_not_consecutive_throws_exception() {
		// Arrange
		// Skips SLOT 2, so slots are not consecutive
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, 
														new ArrayList<> (List.of(START, SLOT_3)));
		
		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, () -> bookingService.book(request));	
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException		
		assertTrue(ex.getMessage().equals("Booking slots are not consecutive"));
	}
	
	@Test
	void if_the_booking_exceeds_maximum_length_throws_exception() {
		// Arrange
		LocalDateTime SLOT_4 = LocalDateTime.of(2026, 5, 4, 10, 30);
		LocalDateTime SLOT_5 = LocalDateTime.of(2026, 5, 4, 11, 0);
		
		// 5 slots -> 150' exceed the maximum length set of 120'
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, 
						new ArrayList<> (List.of(START, SLOT_2, SLOT_3, SLOT_4, SLOT_5)));

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, () -> bookingService.book(request));	

		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertTrue(ex.getMessage().equals("Booking exceeds maximum duration allowed"));
	}
	
	@Test
	void if_the_classroom_is_not_available_throws_exception() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, new ArrayList<>(List.of(START, SLOT_2, SLOT_3)));

		when(bookingRepository.findActiveBookingsForClassroomByPeriod(anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)))
							.thenReturn(List.of(new Booking())); // List is not empty -> Classroom is not available

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, () -> bookingService.book(request));

		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertTrue(ex.getMessage().equals("Classroom is not available for this time period"));
	}
	
	@Test
	void if_the_user_has_no_bookings_left_throws_exception() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID,new ArrayList<>(List.of(START, SLOT_2, SLOT_3)));

		Booking activeBooking = new Booking(0, USER_ID, CLASSROOM_ID,
				START.plusDays(1),
				START.plusDays(1).plusMinutes(30),
				LocalDateTime.now(),
				BookingStatus.ACTIVE);

		when(bookingRepository.findBookingsByUser(USER_ID))
									.thenReturn(new ArrayList<>(List.of(activeBooking, activeBooking, activeBooking)));

		// Act & Assert
		InvalidBookingException ex = assertThrows(InvalidBookingException.class, () -> bookingService.book(request));

		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException	
		assertTrue(ex.getMessage().equals("User has reached the maximum number of weekly bookings"));
	}
	
	@Test
	void cancels_booking_if_the_booking_exists_in_the_database() {
		// Arrange
		long idBooking = 3L;
		Booking booking = new Booking(idBooking, 1, 9, LocalDateTime.of(2026, 5, 11, 10, 0), 
				LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 
		when(bookingRepository.findById(idBooking)).thenReturn(Optional.of(booking));
		when(watchAlertRepository.findWatchAlertsByBooking(idBooking)).thenReturn(
			    List.of(
			        new WatchAlert(1L, idBooking, USER_EMAIL),
			        new WatchAlert(2L, idBooking, "other@gmail.com")
			    ));
		
		// Act
		bookingService.cancel(idBooking);

		// Assert
		verify(bookingRepository).modifyBookingStatus(idBooking, BookingStatus.CANCELLED);
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.BOOKING_CANCELLED, booking, UserContext.getEmail());
	    verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_TRIGGERED, booking, USER_EMAIL);
	    verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_TRIGGERED, booking, "other@gmail.com");
	}
	
	@Test
	void if_trying_to_cancel_a_booking_that_does_not_exist_throws_exception() {
		// Arrange
		long idBooking = 3L;
		when(bookingRepository.findById(idBooking)).thenReturn(Optional.empty());
		
		// Act & Assert
		assertThrows(NoSuchBookingException.class, () -> bookingService.cancel(idBooking));
	}
}