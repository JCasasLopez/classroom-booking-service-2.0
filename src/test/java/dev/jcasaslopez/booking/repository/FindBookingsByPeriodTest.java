package dev.jcasaslopez.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.EntityManager;

@Disabled("Transactional isolation issue with MySQL - READ_COMMITTED not being applied correctly")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

// MySQL defaults to REPEATABLE_READ, which takes a snapshot at transaction start,
// making inserts done within the test invisible to subsequent queries. READ_COMMITTED
// ensures each query sees the latest flushed data within the same transaction.
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FindBookingsByPeriodTest {

	@Autowired private BookingRepository bookingRepository;
	@Autowired private EntityManager entityManager;
	
	private void setupTestBookings() {
		Booking booking1 = new Booking(0, 1, 10, 
										LocalDateTime.of(2025, 3, 2, 14, 0),
										LocalDateTime.of(2025, 3, 2, 15, 30), LocalDateTime.now(), 
										BookingStatus.ACTIVE);
		bookingRepository.save(booking1);

		Booking booking2 = new Booking(0, 1, 10, 
										LocalDateTime.of(2025, 3, 2, 17, 0),
										LocalDateTime.of(2025, 3, 2, 18, 0), LocalDateTime.now(), 
										BookingStatus.ACTIVE);
		bookingRepository.save(booking2);

		Booking booking3 = new Booking(0, 1, 10, 
										LocalDateTime.of(2025, 3, 2, 19, 0),
										LocalDateTime.of(2025, 3, 2, 20, 30), LocalDateTime.now(), 
										BookingStatus.CANCELLED);
		bookingRepository.save(booking3);

		Booking booking4 = new Booking(0, 4, 10, 
										LocalDateTime.of(2025, 3, 2, 17, 30),
										LocalDateTime.of(2025, 3, 2, 18, 30), LocalDateTime.now(), 
										BookingStatus.ACTIVE);
		bookingRepository.save(booking4);

		entityManager.flush();
		entityManager.clear();
	}
	
	@ParameterizedTest
	@MethodSource("bookingPeriodsAndExpectedResults")
	void repository_finds_active_bookings_for_classroom_by_period_correctly(int idClassroom, 
			int expectedValidBookings, LocalDateTime queryStart, LocalDateTime queryFinish) {
		// Arrange
		setupTestBookings();
		
		// Act
		List<Booking> bookingsFound = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, queryStart, queryFinish);

		// Assert
		assertEquals(expectedValidBookings, bookingsFound.size());
	}

	// Test data for findActiveBookingsForClassroomByPeriod_ReturnsExpectedResultsTest.
	// (Classroom, expected result, start, finish).
	private static Stream<Arguments> bookingPeriodsAndExpectedResults() {

		// Bookings set in setupTestBookings():
		// ┌────────────┬───────────────┬──────────┐
		// │ Classroom  │  Hours        │ Status   │
		// ├────────────┼───────────────┼──────────┤
		// │ 1          │ 14:00-15:30   │ ACTIVE   │
		// │ 1          │ 17:00-18:00   │ ACTIVE   │
		// │ 1          │ 19:00-20:30   │ CANCELLED│
		// │ 4          │ 17:30-18:30   │ ACTIVE   │
		// └────────────┴───────────────┴──────────┘

		return Stream.of(
				// 13:00 - 22:00 → No edge cases, all active bookings fully fit within the period.
				Arguments.of(1, 2, LocalDateTime.of(2025, 3, 2, 13, 0), LocalDateTime.of(2025, 3, 2, 22, 00)),

				// 14:30 - 17:30 → Edge case: The search period partially overlaps with two active bookings.
				Arguments.of(1, 2, LocalDateTime.of(2025, 3, 2, 14, 30), LocalDateTime.of(2025, 3, 2, 17, 30)),

				// 14:30 - 15:00 → Edge case: The search period falls entirely within an active booking.
				Arguments.of(1, 1, LocalDateTime.of(2025, 3, 2, 14, 30), LocalDateTime.of(2025, 3, 2, 15, 00)),

				// 15:30 - 17:00 → Edge case: The search period falls exactly between two bookings.
				Arguments.of(1, 0, LocalDateTime.of(2025, 3, 2, 15, 30), LocalDateTime.of(2025, 3, 2, 17, 00)),

				// 13:00 - 22:00 (classroom 2) → Searching in a different classroom with NO active bookings.
				Arguments.of(2, 0, LocalDateTime.of(2025, 3, 2, 13, 0), LocalDateTime.of(2025, 3, 2, 22, 00))
				);
	}
}