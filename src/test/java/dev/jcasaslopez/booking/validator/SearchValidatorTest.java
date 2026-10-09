package dev.jcasaslopez.booking.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class SearchValidatorTest {
	
	private SearchValidator searchValidator;
	
	private static final LocalDateTime START = next(DayOfWeek.MONDAY);

	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	
	private WeeklySchedule buildTestWeeklySchedule() {
	    List<String> hours = List.of("9:00-22:00", "9:00-22:00", "9:00-22:00", "9:00-22:00", "9:00-22:00", "10:00-14:00", "CLOSED");
	    return new WeeklySchedule(hours, SLOT_DURATION_30);
	}
	
	private static LocalDateTime next(DayOfWeek day) {
		return LocalDate.now().with(TemporalAdjusters.next(day)).atTime(11, 0);
	}
		
	@BeforeEach
	void setUp() {
		searchValidator = new SearchValidator(buildTestWeeklySchedule());
	}

	@Test
	void search_validator_does_not_allow_searches_for_the_past() {
		// Arrange
		LocalDateTime pastStart = LocalDateTime.of(2026, 5, 27, 11, 0);
		LocalDateTime pastFinish = LocalDateTime.of(2026, 5, 27, 21, 0);

		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
				() -> searchValidator.validateSearch(pastStart, pastFinish));

		assertEquals("Search range cannot be in the past", ex.getMessage());				
	}

	@ParameterizedTest
	@ValueSource(longs = {0, -30})
	void search_validator_does_not_allow_start_equal_or_after_finish(long minutesFromStart) {
		// Arrange
		LocalDateTime finish = START.plusMinutes(minutesFromStart);

		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> searchValidator.validateSearch(START, finish));

		assertEquals("Finish must be after start", ex.getMessage());
	}

	@Test
	void search_validator_does_not_allow_searches_spanning_longer_than_a_day() {
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
				() -> searchValidator.validateSearch(START, START.plusDays(2)));

		assertEquals("Start and finish have to be in the same day", ex.getMessage());			
	}

	@Test
	void search_validator_does_not_allow_searches_on_the_same_weekday_of_different_weeks() {
		// Act & Assert
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> searchValidator.validateSearch(START, START.plusDays(7)));

		assertEquals("Start and finish have to be in the same day", ex.getMessage());
	}

	@Test
	void search_validator_does_not_allow_searches_on_a_closed_day() {
		// Act & Assert
		LocalDateTime nextSunday = next(DayOfWeek.SUNDAY);
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, 
				() -> searchValidator.validateSearch(nextSunday, nextSunday.plusHours(10)));

		assertEquals("The center is closed that day", ex.getMessage());			
	}

	static Stream<Arguments> validSearches() {
		LocalDate monday = next(DayOfWeek.MONDAY).toLocalDate();
		LocalDate saturday = next(DayOfWeek.SATURDAY).toLocalDate();

		return Stream.of(
			// Monday (9:00-22:00)
			Arguments.of(monday.atTime(11, 0), monday.atTime(21, 0)),   // inside opening hours
			Arguments.of(monday.atTime(9, 0),  monday.atTime(22, 0)),   // exactly opening to closing
			// Saturday (10:00-14:00)
			Arguments.of(saturday.atTime(10, 0), saturday.atTime(14, 0))
		);
	}

	@ParameterizedTest
	@MethodSource("validSearches")
	void valid_searches_do_not_throw_exception(LocalDateTime start, LocalDateTime finish) {
		// Act & Assert
		assertDoesNotThrow(() -> searchValidator.validateSearch(start, finish));
	}

	static Stream<Arguments> searchesOutOfOpeningHours() {
		LocalDate monday = next(DayOfWeek.MONDAY).toLocalDate();
		LocalDate saturday = next(DayOfWeek.SATURDAY).toLocalDate();

		return Stream.of(
			// Monday: one minute outside each limit
			Arguments.of(monday.atTime(8, 59),  monday.atTime(12, 0)),
			Arguments.of(monday.atTime(11, 0),  monday.atTime(22, 1)),
			// Saturday: valid on weekdays, but outside Saturday's own hours
			Arguments.of(saturday.atTime(9, 0),  saturday.atTime(12, 0)),
			Arguments.of(saturday.atTime(11, 0), saturday.atTime(15, 0))
		);
	}

	@ParameterizedTest
	@MethodSource("searchesOutOfOpeningHours")
	void searches_out_of_opening_hours_throw_exception(LocalDateTime start, LocalDateTime finish) {
		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class,
				() -> searchValidator.validateSearch(start, finish));

		assertEquals("Start or finish out of opening hours", ex.getMessage());
	}

}
