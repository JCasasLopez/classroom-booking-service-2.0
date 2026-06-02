package dev.jcasaslopez.booking.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.EntityManager;

@Disabled("Transactional isolation issue with MySQL - READ_COMMITTED not being applied correctly")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(isolation = Isolation.READ_COMMITTED)
public class FindOccupiedClassroomsAndUserBookingTest {

	@Autowired private BookingRepository bookingRepository;
	@Autowired private EntityManager entityManager;
	
	@ParameterizedTest
	@MethodSource("bookingPeriodsAndExpectedResult")
	void findOccupiedClassroomsbyPeriod_ReturnsExpectedResultsTest
	(List<Integer> listClassrooms, LocalDateTime queryStart, LocalDateTime queryFinish) {
		// Arrange
		setupTestBookings();

		// Act
		List<Integer> unavailableClassrooms = bookingRepository.findOccupiedClassroomsbyPeriod(queryStart, queryFinish);

		// Assert
		// To use a Set<> instead of a List<> when comparing both groups renders the order of the elements irrelevant.
		assertEquals(new HashSet<>(listClassrooms), new HashSet<>(unavailableClassrooms));
	}

	@Test
	void findBookingsByUser_ReturnsExpectedResultsTest() {
		// Arrange
		setupTestBookings();

		// Act
		int idUser = 10;
		List<Booking> bookingsFound = bookingRepository.findBookingsByUser(idUser);

		// Assert
		long activeCount = bookingsFound.stream().filter(booking -> booking.getStatus() == BookingStatus.ACTIVE).count();
		long cancelledCount = bookingsFound.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).count();

		assertAll(
				() -> assertEquals(4, bookingsFound.size()),
				() -> assertEquals(2, activeCount),
				() -> assertEquals(2, cancelledCount),
				() -> assertTrue(bookingsFound.stream().allMatch(booking -> booking.getIdUser() == idUser))
				);
	}

	// Test data for findOccupiedClassroomsbyPeriod_ReturnsExpectedResultsTest
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
	
	private void setupTestBookings() {
		
		// ┌────────────┬───────────────┬─────────┬───────────┐
		// │ Classroom  │ Hours         │ idUser  │ Status    │
		// ├────────────┼───────────────┼─────────┼───────────┤
		// │ 1          │ 14:00-15:30   │ 10      │ ACTIVE    │
		// │ 1          │ 17:00-18:00   │ 8       │ ACTIVE    │
		// │ 1          │ 19:00-20:30   │ 10      │ CANCELLED │
		// │ 2          │ 14:00-15:00   │ 8       │ ACTIVE    │
		// │ 2          │ 17:00-19:00   │ 10      │ ACTIVE    │
		// │ 2          │ 20:00-21:30   │ 10      │ CANCELLED │
		// └────────────┴───────────────┴─────────┴───────────┘
		
		Booking booking1 = new Booking(0, 1, 10, 
				LocalDateTime.of(2025, 3, 2, 14, 0),
				LocalDateTime.of(2025, 3, 2, 15, 30), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking1);

		Booking booking2 = new Booking(0, 1, 8, 
				LocalDateTime.of(2025, 3, 2, 17, 0),
				LocalDateTime.of(2025, 3, 2, 18, 0), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking2);
		
		Booking booking3 = new Booking(0, 1, 10, 
				LocalDateTime.of(2025, 3, 2, 19, 0),
				LocalDateTime.of(2025, 3, 2, 20, 30), LocalDateTime.now(), 
				BookingStatus.CANCELLED);
		bookingRepository.save(booking3);

		Booking booking4 = new Booking(0, 2, 8, 
				LocalDateTime.of(2025, 3, 2, 14, 00),
				LocalDateTime.of(2025, 3, 2, 15, 00), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking4);

		Booking booking5 = new Booking(0, 2, 10, 
				LocalDateTime.of(2025, 3, 2, 17, 00),
				LocalDateTime.of(2025, 3, 2, 19, 00), LocalDateTime.now(), 
				BookingStatus.ACTIVE);
		bookingRepository.save(booking5);

		Booking booking6 = new Booking(0, 2, 10, 
				LocalDateTime.of(2025, 3, 2, 20, 00),
				LocalDateTime.of(2025, 3, 2, 21, 30), LocalDateTime.now(), 
				BookingStatus.CANCELLED);
		bookingRepository.save(booking6);

		entityManager.flush();
		entityManager.clear();
	}
}