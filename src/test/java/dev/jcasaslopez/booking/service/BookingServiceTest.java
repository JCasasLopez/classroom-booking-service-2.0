package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.mapper.BookingMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

// NOTE: time slot validity (opening hours, slot alignment) is tested in TimeSlotTest. No need to duplicate those tests here.
@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
	
	@Mock BookingRepository bookingRepository;
	@Mock WatchAlertRepository watchAlertRepository;
	@Mock ClassroomValidator classroomValidator;
	@Mock EventPublisher eventPublisher;
	@Mock BookingMapper mapper;

	private BookingServiceImpl bookingService;
	
	private static final int USER_ID = 1;
	private static final int CLASSROOM_ID = 1;
	private static final String USER_EMAIL = "test@gmail.com";
	
	private static LocalDateTime nextMonday() {
	    LocalDate today = LocalDate.now();
	    LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	    return nextMonday.atTime(9, 0);
	}
	
	private static final LocalDateTime START = nextMonday();
	private static final LocalDateTime SLOT_2 = START.plusMinutes(30);
	private static final LocalDateTime SLOT_3 = START.plusMinutes(60);
	private static final LocalDateTime EXPECTED_FINISH = SLOT_3.plusMinutes(30);
	
	private WeeklySchedule buildTestWeeklySchedule() {
	    List<String> hours = new ArrayList<> (List.of("09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "10:00-14:00", "CLOSED"));
	    return new WeeklySchedule(hours);
	}
	
	private static List<ClassroomEvent> allClassrooms = List.of(
		    new ClassroomEvent(1, "Main Auditorium", 150, true, true),
		    new ClassroomEvent(2, "Standard Seminar Room", 30, true, false)
		);
	
	@BeforeEach
	void setUp() {
	    bookingService = new BookingServiceImpl(
	        bookingRepository,
	        watchAlertRepository,
	        buildTestWeeklySchedule(), 
	        classroomValidator,
	        eventPublisher,
	        mapper,
	        allClassrooms,           
	        30,                        
	        120,                       
	        3                          
	    );
        
	    UserContext.setContext(USER_EMAIL, USER_ID);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	void if_the_booking_is_valid_persists_the_correct_booking_entity() {
		// Arrange
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, new ArrayList<> (List.of(START, SLOT_2, SLOT_3)));
		Booking bookingEntity = new Booking(0, USER_ID, CLASSROOM_ID, START, EXPECTED_FINISH, LocalDateTime.now(), BookingStatus.ACTIVE);
		String classroomName = allClassrooms.get(CLASSROOM_ID).getName();
		when(bookingRepository.save(any(Booking.class))).thenReturn(bookingEntity);
		when(mapper.toResponseDto(any(Booking.class), any(List.class))).thenReturn
								(new BookingResponseDto(4, classroomName, START, EXPECTED_FINISH, BookingStatus.ACTIVE));
		
		// Act
		BookingResponseDto booking = bookingService.book(request);
		
		// Assert
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.BOOKING_CONFIRMED, bookingEntity, USER_EMAIL);		
		assertAll(
				() -> assertEquals(classroomName, booking.classroomName()),
				() -> assertEquals(START, booking.start()),
				() -> assertEquals(EXPECTED_FINISH, booking.finish())
				);
	}
	
	@Test
	void if_the_booking_slots_are_in_the_past_throws_exception() {
		// Arrange
		LocalDateTime pastStart = LocalDateTime.of(2026, 5, 4, 9, 0);
		LocalDateTime pastSlot2 = LocalDateTime.of(2026, 5, 4, 10, 0);
		BookingRequestDto request = new BookingRequestDto(USER_ID, CLASSROOM_ID, 
														new ArrayList<> (List.of(pastStart, pastSlot2)));
		
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> bookingService.book(request));	
		verify(classroomValidator).validateClassroomExists(CLASSROOM_ID);	
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw InvalidBookingException		
		assertTrue(ex.getMessage().equals("Booking a past period is not allowed"));
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
		LocalDateTime SLOT_4 = START.plusMinutes(90);
		LocalDateTime SLOT_5 = START.plusMinutes(120);
		
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