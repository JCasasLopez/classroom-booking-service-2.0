package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.exception.DataIntegrityException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.validator.ClassroomValidator;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
	
	@Mock BookingRepository bookingRepository;
	@Mock ClassroomValidator classroomValidator;
	@Mock SlotAvailabilityMapper slotAvailabilityMapper;
	SearchServiceImpl searchService;
	
	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	
	private WeeklySchedule buildTestWeeklySchedule() {
	    List<String> hours = new ArrayList<> (List.of("09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "09:00-22:00", "10:00-14:00", "CLOSED"));
	    return new WeeklySchedule(hours, SLOT_DURATION_30);
	}
	
	private static LocalDateTime nextMonday() {
	    LocalDate today = LocalDate.now();
	    LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
	    return nextMonday.atTime(11, 0);
	}
	
	private static final LocalDateTime START = nextMonday();
	private static final LocalDateTime FINISH = START.plusHours(10);
	private static final int ID_CLASSROOM = 1;
	private static final long ID_BOOKING = 5;
	private static final LocalDateTime SLOT_FINISH = START.plusMinutes(30);

	private Booking bookingInSlot(long idBooking) {
	    return new Booking(idBooking, 1, ID_CLASSROOM, START, SLOT_FINISH,
	            START.minusDays(1), BookingStatus.ACTIVE);
	}
	
	private static LocalDateTime nextSunday() {
	    LocalDate today = LocalDate.now();
	    LocalDate nextSunday = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
	    return nextSunday.atTime(11, 0);
	}

	private static List<ClassroomEvent> allClassrooms = List.of(
		    new ClassroomEvent(1, "Main Auditorium", 150, true, true),
		    new ClassroomEvent(2, "Standard Seminar Room", 30, true, false),
		    new ClassroomEvent(3, "Advanced Tech Lab", 25, true, true),
		    new ClassroomEvent(4, "Small Study Group", 10, false, false),
		    new ClassroomEvent(5, "Conference Hall A", 80, true, true),
		    new ClassroomEvent(6, "Creative Workshop Space", 40, false, true),
		    new ClassroomEvent(7, "Lecture Theatre B", 120, true, true),
		    new ClassroomEvent(8, "Quiet Reading Room", 15, false, false)
		);
	
	@BeforeEach
	void setUp() {
		searchService = new SearchServiceImpl(
				bookingRepository,
				classroomValidator,
				slotAvailabilityMapper,
				allClassrooms,
				buildTestWeeklySchedule(),
				SLOT_DURATION_30
		);
	}
	
	@Test
	void classroomsAvailableByPeriod_returns_the_expected_list() {
		// Arrange
		List<Integer> occupiedClassrooms = List.of(1, 2, 4, 6);
		when(bookingRepository.findOccupiedClassroomsbyPeriod(START, FINISH)).thenReturn(occupiedClassrooms);
		
		// Act
		List<ClassroomEvent> availableClassrooms = searchService.classroomsAvailableByPeriod(START, FINISH);
		
		// Assert
		assertAll(
			    () -> assertEquals(4, availableClassrooms.size()),
			    () -> assertTrue(availableClassrooms.stream()
			            .map(ClassroomEvent::idClassroom)
			            .toList()
			            .containsAll(List.of(3, 5, 7, 8)))
			);
	}
	
	@ParameterizedTest
	@MethodSource("featuresFilterProvider")
	void classroomsAvailableByPeriodAndFeatures_returns_the_expected_list(
	        int seats, boolean projector, boolean speakers, List<Integer> expectedIds) {
		// Arrange
		when(bookingRepository.findOccupiedClassroomsbyPeriod(START, FINISH)).thenReturn(List.of());
		
		// Act
		List<ClassroomEvent> availableClassrooms = searchService.classroomsAvailableByPeriodAndFeatures
				(START, FINISH, seats, projector, speakers);
		
		// Assert
		assertAll(
			    () -> assertEquals(expectedIds.size(), availableClassrooms.size()),
			    () -> assertTrue(availableClassrooms.stream()
			            .map(ClassroomEvent::idClassroom)
			            .toList()
			            .containsAll(expectedIds))
			);
	}
	
	static Stream<Arguments> featuresFilterProvider() {
	    return Stream.of(
	    		// Conditions -> Seats, projector, speakers, idClassrooms of classrooms available.
	            Arguments.of(100, true,  false, List.of(1, 7)),
	            Arguments.of(30, true, false,  List.of(1, 2, 5, 7)),
	            Arguments.of(40, true,  true,  List.of(1, 5, 7)),
	            Arguments.of(20, false, true, List.of(1, 3, 5, 6, 7))
	    );
	}
	
	@Test
	void findBooking_returns_the_id_of_the_only_active_booking() {
	    when(bookingRepository.findActiveBookingsForClassroomByPeriod(ID_CLASSROOM, START, SLOT_FINISH))
	            .thenReturn(List.of(bookingInSlot(ID_BOOKING)));

	    Long result = searchService.findBookingByClassroomAndTimePeriod(ID_CLASSROOM, START, SLOT_FINISH);

	    assertEquals(ID_BOOKING, result);
	}

	@Test
	void findBooking_without_active_booking_throws_not_found() {
	    when(bookingRepository.findActiveBookingsForClassroomByPeriod(ID_CLASSROOM, START, SLOT_FINISH))
	            .thenReturn(List.of());

	    assertThrows(BookingNotFoundExceptions.class,
	            () -> searchService.findBookingByClassroomAndTimePeriod(ID_CLASSROOM, START, SLOT_FINISH));
	}

	@Test
	void findBooking_with_more_than_one_booking_throws_data_integrity() {
	    when(bookingRepository.findActiveBookingsForClassroomByPeriod(ID_CLASSROOM, START, SLOT_FINISH))
	            .thenReturn(List.of(bookingInSlot(ID_BOOKING), bookingInSlot(ID_BOOKING + 1)));

	    assertThrows(DataIntegrityException.class,
	            () -> searchService.findBookingByClassroomAndTimePeriod(ID_CLASSROOM, START, SLOT_FINISH));
	}

	@Test
	void findBooking_with_period_longer_than_a_slot_throws_illegal_argument() {
	    LocalDateTime tooLong = START.plusMinutes(60);

	    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
	            () -> searchService.findBookingByClassroomAndTimePeriod(ID_CLASSROOM, START, tooLong));

	    assertEquals("The search period must match exactly the minimum time slot duration", ex.getMessage());
	}
	
	@Test
	void search_service_does_not_allow_searches_for_the_past() {
		// Arrange
		LocalDateTime pastStart = LocalDateTime.of(2026, 5, 27, 11, 0);
		LocalDateTime pastFinish = LocalDateTime.of(2026, 5, 27, 21, 0);
		
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
				() -> searchService.classroomsAvailableByPeriod(pastStart, pastFinish));
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw IllegalArgumentException	
		assertTrue(ex.getMessage().equals("Search range cannot be in the past"));				
	}
	
	@ParameterizedTest
	@ValueSource(longs = {0, -30})
	void search_service_does_not_allow_start_equal_or_after_finish(long minutesFromStart) {
		// Arrange
		LocalDateTime finish = START.plusMinutes(minutesFromStart);

		// Act & Assert
	    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
	            () -> searchService.classroomsAvailableByPeriod(START, finish));

	    assertEquals(String.format("Finish must be after start", START, finish), ex.getMessage());
	}
	
	@Test
	void search_service_does_not_allow_searches_spanning_longer_than_a_day() {
		// Arrange
		
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
				() -> searchService.classroomsAvailableByPeriod(START, START.plusDays(2)));
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw IllegalArgumentException	
		assertTrue(ex.getMessage().equals("Start and finish have to be in the same day"));			
	}
	
	@Test
	void search_service_does_not_allow_searches_on_the_same_weekday_of_different_weeks() {
		// Arrange

		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> searchService.classroomsAvailableByPeriod(START, START.plusDays(7)));

		assertEquals("Start and finish have to be in the same day", ex.getMessage());
	}

	@Test
	void search_service_does_not_allow_searches_on_a_closed_day() {
		// Search on a SUNDAY
		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, 
				() -> searchService.classroomsAvailableByPeriod(nextSunday(), nextSunday().plusHours(10)));
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw IllegalArgumentException	
		assertTrue(ex.getMessage().equals("The center is closed that day"));			
	}
	
	@Test
	void search_service_does_not_allow_searches_before_opening_time() {
		// Arrange
		// Start at 6am
		LocalDateTime startBeforeOpeningtime = START.minusHours(5);
		
		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, 
				() -> searchService.classroomsAvailableByPeriod(startBeforeOpeningtime, FINISH));
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw IllegalArgumentException	
		assertTrue(ex.getMessage().equals("Start or finish out of opening hours"));			
	}
	
	@Test
	void search_service_does_not_allow_searches_after_closing_time() {
		// Arrange
		// Finish at 23h
		LocalDateTime finishAfterClosingTime = FINISH.plusHours(2);
		
		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, 
				() -> searchService.classroomsAvailableByPeriod(START, finishAfterClosingTime));
		
		// The exception message must be checked to identify the exact cause, since all validation failures throw IllegalArgumentException	
		assertTrue(ex.getMessage().equals("Start or finish out of opening hours"));			
	}
}