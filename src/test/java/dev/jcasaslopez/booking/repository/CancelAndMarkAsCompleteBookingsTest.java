package dev.jcasaslopez.booking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.jcasaslopez.booking.base.BaseRepositoryTest;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.EntityManager;

public class CancelAndMarkAsCompleteBookingsTest extends BaseRepositoryTest{
	
	@Autowired private BookingRepository bookingRepository;
	@Autowired private EntityManager entityManager;
	
	@Test
    void repository_cancels_bookings_successfully() {
        // Arrange
		Booking savedBooking = createAndSaveBooking(LocalDateTime.of(2025, 3, 1, 10, 0), LocalDateTime.of(2025, 3, 1, 12, 0), BookingStatus.ACTIVE);
        Long bookingId = savedBooking.getIdBooking();

        // Act
        executeFlushAndClear(() -> bookingRepository.modifyBookingStatus(bookingId, BookingStatus.CANCELLED));
        
        // Assert
        assertBookingStatus(bookingId, BookingStatus.CANCELLED, "The booking status should be CANCELLED");
    }
	
	@Test
    void repository_marks_complete_bookings_correctly() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
        Booking savedBooking = createAndSaveBooking(now.minusHours(2), 
        											now.minusHours(1), 
        											BookingStatus.ACTIVE);
        Long bookingId = savedBooking.getIdBooking();

        // Act
        executeFlushAndClear(() -> bookingRepository.markCompletedBookings(LocalDateTime.now()));

        // Assert
        assertBookingStatus(bookingId, BookingStatus.COMPLETED, "The booking status should be COMPLETED");
    }
	

	@Test
	void repository_does_not_mark_future_bookings_as_complete() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
		Booking savedBooking = createAndSaveBooking(now.plusHours(1), 
													now.plusHours(2), 
													BookingStatus.ACTIVE);
		Long bookingId = savedBooking.getIdBooking();

		// Act
        executeFlushAndClear(() -> bookingRepository.markCompletedBookings(LocalDateTime.now()));

		// Assert
        assertBookingStatus(bookingId, BookingStatus.ACTIVE, "The booking status should be ACTIVE");
	}
	
	@Test
	void repository_does_not_mark_ongoing_bookings_as_complete() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
		Booking savedBooking = createAndSaveBooking(now.minusHours(1),
													now.plusHours(1), 
													BookingStatus.ACTIVE);
		Long bookingId = savedBooking.getIdBooking();

		// Act
        executeFlushAndClear(() -> bookingRepository.markCompletedBookings(LocalDateTime.now()));

		// Assert
        assertBookingStatus(bookingId, BookingStatus.ACTIVE, "The booking status should be ACTIVE");
	}
	
	@Test
	void repository_does_not_change_status_for_already_complete_bookings() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
		Booking savedBooking = createAndSaveBooking(now.minusHours(2),
													now.minusHours(1), 
													BookingStatus.COMPLETED);
		Long bookingId = savedBooking.getIdBooking();

		// Act
        executeFlushAndClear(() -> bookingRepository.markCompletedBookings(LocalDateTime.now()));

		// Assert
        assertBookingStatus(bookingId, BookingStatus.COMPLETED, "The booking status should be COMPLETED");
	}
	
	@Test
	void repository_does_not_change_status_for_cancelled_bookings() {
		// Arrange
		LocalDateTime now = LocalDateTime.now();
		Booking savedBooking = createAndSaveBooking(now.plusHours(1),
													now.plusHours(2), 
													BookingStatus.CANCELLED);
		Long bookingId = savedBooking.getIdBooking();

		// Act
        executeFlushAndClear(() -> bookingRepository.markCompletedBookings(LocalDateTime.now()));

		// Assert
        assertBookingStatus(bookingId, BookingStatus.CANCELLED, "The booking status should be CANCELLED");

	}
	
	
	private Booking createAndSaveBooking(LocalDateTime start, LocalDateTime finish, BookingStatus status) {
		Booking booking = new Booking(0L, 100, 200, start, finish, LocalDateTime.now(), status);
		Booking savedBooking = bookingRepository.save(booking);
		entityManager.flush();
		return savedBooking;
	}

	private void executeFlushAndClear(Runnable action) {
		action.run();
		entityManager.flush();
		entityManager.clear();
	}

	private void assertBookingStatus(Long bookingId, BookingStatus expectedStatus, String message) {
		Optional<Booking> updatedBooking = bookingRepository.findById(bookingId);
		assertTrue(updatedBooking.isPresent(), "The booking should exist");
		assertEquals(expectedStatus, updatedBooking.get().getStatus(), message);
	}
}