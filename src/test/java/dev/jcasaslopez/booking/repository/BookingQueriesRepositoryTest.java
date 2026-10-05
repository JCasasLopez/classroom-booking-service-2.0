package dev.jcasaslopez.booking.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import dev.jcasaslopez.booking.base.BaseRepositoryTest;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.EntityManager;

public class BookingQueriesRepositoryTest extends BaseRepositoryTest{

	@Autowired private EntityManager entityManager;
	
	@ParameterizedTest
	@MethodSource("bookingPeriodsAndExpectedResult")
	void findOccupiedClassroomsbyPeriod_returns_expected_result
	(List<Integer> listClassrooms, LocalDateTime queryStart, LocalDateTime queryFinish) {
		// Arrange
		setupTestBookings();

		// Act
		List<Integer> unavailableClassrooms = bookingRepository.findOccupiedClassroomsbyPeriod(queryStart, queryFinish);

		// Assert
		// To use a Set<> instead of a List<> when comparing both groups renders the order of the elements irrelevant.
		assertEquals(new HashSet<>(listClassrooms), new HashSet<>(unavailableClassrooms));
	}

	@ParameterizedTest
    @CsvSource({
        "10,   4",
        "8,   3"
        })
	void findBookingsByUser_returns_expected_result(int idUser, int totalNumberOfBookings) {
		// Arrange
		setupTestBookings();

		// Act
		List<Booking> bookingsFound = bookingRepository.findBookingsByUser(idUser);

		// Assert	
		assertAll(
				() -> assertEquals(totalNumberOfBookings, bookingsFound.size())
				);
	}
	
	@ParameterizedTest
	@MethodSource("countBookingsByUserInPeriodCases")
	void countBookingsByUserInPeriod_returns_expected_result
	(int idUser, LocalDateTime queryStart, LocalDateTime queryFinish, long expectedCount) {
		// Arrange
		setupTestBookings();

		// Act
	    long count = bookingRepository.countBookingsByUserInPeriod(idUser, queryStart, queryFinish);

	    // Assert
	    assertEquals(expectedCount, count);
	}

	// Test data for findOccupiedClassroomsbyPeriod_returns_expected_result
	// (Expected result, start, finish).
	private static Stream<Arguments> bookingPeriodsAndExpectedResult() {
		// ┌────────────┬───────────────┬───────────┐
		// │ Classroom  │  Hours        │ Status    │
		// ├────────────┼───────────────┼───────────┤
		// │ 1          │ 14:00-15:30   │ ACTIVE    │
		// │ 1          │ 17:00-18:00   │ ACTIVE    │
		// │ 1          │ 19:00-20:30   │ CANCELLED │
		// │ 2          │ 14:00-15:00   │ ACTIVE    │
		// │ 2          │ 17:00-19:00   │ ACTIVE    │
		// │ 2          │ 20:00-21:30   │ CANCELLED │
		// └────────────┴───────────────┴───────────┘

		return Stream.of(
				// 16:00 - 16:30 → No edge cases, the searched period does not match either the start
				// or the end of any booking. All classrooms are available.
				Arguments.of(List.of(), LocalDateTime.of(2025, 3, 2, 16, 00), LocalDateTime.of(2025, 3, 2, 16, 30)),

				// 15:30 - 17:00 → Edge case: The search starts and ends exactly at the edges of two active bookings.
				//  All classrooms are available.
				Arguments.of(List.of(), LocalDateTime.of(2025, 3, 2, 15, 30), LocalDateTime.of(2025, 3, 2, 17, 00)),

				// 14:00 - 20:00 → Period with various active bookings. No classrooms available.
				Arguments.of(List.of(1, 2), LocalDateTime.of(2025, 3, 2, 14, 00), LocalDateTime.of(2025, 3, 2, 22, 00)),

				// 15:00 - 16:00 → The search starts within a classroom 1 active booking. 
				// Classroom 1 is unavailable.
				Arguments.of(List.of(1), LocalDateTime.of(2025, 3, 2, 15, 00), LocalDateTime.of(2025, 3, 2, 16, 00)),

				// 18:00 - 19:30 → The search starts within a classroom 2 active booking. 
				// Classroom 2 is unavailable.
				Arguments.of(List.of(2), LocalDateTime.of(2025, 3, 2, 18, 00), LocalDateTime.of(2025, 3, 2, 19, 30))
				);
	}
	
	// Test data for countBookingsByUserInPeriod_returns_expected_result
	// (idUser, start, finish, expected result).
	private static Stream<Arguments> countBookingsByUserInPeriodCases() {
	    return Stream.of(
	        // User 8: Full period. 
	        // Includes: 10:00 (COMPLETED), 14:00 (ACTIVE), 17:00 (ACTIVE) -> 3
	        Arguments.of(8, LocalDateTime.of(2025, 3, 2, 10, 0), LocalDateTime.of(2025, 3, 2, 21, 30), 3L),

	        // User 10: Full period. 
	        // Includes: 14:00 (ACTIVE), 17:00 (ACTIVE). Ignores: CANCELLED (19:00, 20:00) -> 2
	        Arguments.of(10, LocalDateTime.of(2025, 3, 2, 10, 0), LocalDateTime.of(2025, 3, 2, 21, 30), 2L),

	        // User 10: Afternoon period.
	        // Includes: 17:00 (ACTIVE). Ignores: CANCELLED (19:00, 20:00) -> 1
	        Arguments.of(10, LocalDateTime.of(2025, 3, 2, 17, 0), LocalDateTime.of(2025, 3, 2, 21, 30), 1L),

	        // Lower bound (>=): User 8's booking starts EXACTLY at 10:00. Must be included -> 1
	        Arguments.of(8, LocalDateTime.of(2025, 3, 2, 10, 0), LocalDateTime.of(2025, 3, 2, 12, 0), 1L),

	        // Upper bound (<): User 8's booking starts EXACTLY at 17:00. 
	        // Searching up to 17:00 excludes it. Includes only 10:00 and 14:00 -> 2
	        Arguments.of(8, LocalDateTime.of(2025, 3, 2, 10, 0), LocalDateTime.of(2025, 3, 2, 17, 0), 2L)
	    );
	}
	
	private void setupTestBookings() {
		
		// ┌────────────┬───────────────┬─────────┬───────────┐
		// │ Classroom  │ Hours         │ idUser  │ Status    │
		// ├────────────┼───────────────┼─────────┼───────────┤
		// │ 1          │ 10:00-11:00   │ 8       │ COMPLETED │    
		// │ 1          │ 14:00-15:30   │ 10      │ ACTIVE    │
		// │ 1          │ 17:00-18:00   │ 8       │ ACTIVE    │
		// │ 1          │ 19:00-20:30   │ 10      │ CANCELLED │
		// │ 2          │ 14:00-15:00   │ 8       │ ACTIVE    │
		// │ 2          │ 17:00-19:00   │ 10      │ ACTIVE    │
		// │ 2          │ 20:00-21:30   │ 10      │ CANCELLED │
		// └────────────┴───────────────┴─────────┴───────────┘
		
		Booking booking0 = new Booking(0, 8, 1, 
				LocalDateTime.of(2025, 3, 2, 10, 0),
				LocalDateTime.of(2025, 3, 2, 11, 0), LocalDateTime.now(), 
				BookingStatus.COMPLETED);
		bookingRepository.save(booking0);
		
		Booking booking1 = new Booking(0, 10, 1, 
				LocalDateTime.of(2025, 3, 2, 14, 0),
				LocalDateTime.of(2025, 3, 2, 15, 30), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking1);

		Booking booking2 = new Booking(0, 8, 1, 
				LocalDateTime.of(2025, 3, 2, 17, 0),
				LocalDateTime.of(2025, 3, 2, 18, 0), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking2);
		
		Booking booking3 = new Booking(0, 10, 1, 
				LocalDateTime.of(2025, 3, 2, 19, 0),
				LocalDateTime.of(2025, 3, 2, 20, 30), LocalDateTime.now(), 
				BookingStatus.CANCELLED);
		bookingRepository.save(booking3);

		Booking booking4 = new Booking(0, 8, 2, 
				LocalDateTime.of(2025, 3, 2, 14, 00),
				LocalDateTime.of(2025, 3, 2, 15, 00), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking4);

		Booking booking5 = new Booking(0, 10, 2, 
				LocalDateTime.of(2025, 3, 2, 17, 00),
				LocalDateTime.of(2025, 3, 2, 19, 00), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking5);

		Booking booking6 = new Booking(0, 10, 2, 
				LocalDateTime.of(2025, 3, 2, 20, 00),
				LocalDateTime.of(2025, 3, 2, 21, 30), LocalDateTime.now(), 
				BookingStatus.CANCELLED);
		bookingRepository.save(booking6);

		entityManager.flush();
		entityManager.clear();
	}
}