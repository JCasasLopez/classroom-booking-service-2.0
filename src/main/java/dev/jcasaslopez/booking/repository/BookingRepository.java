package dev.jcasaslopez.booking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	@Modifying
	@Query("UPDATE Booking b SET b.status = :status WHERE b.idBooking = :idBooking")
	void modifyBookingStatus(Long idBooking, BookingStatus status);

	@Modifying
	@Query("UPDATE Booking b SET b.status = 'COMPLETED' WHERE b.status = 'ACTIVE' AND b.finish < :now")
	void markCompletedBookings(LocalDateTime now);

	// This method retrieves active bookings for a classroom within a specified period, 
	// including those that partially or fully overlap with the given time frame.
	@Query("""
			    SELECT b FROM Booking b
			    WHERE b.idClassroom = :queryIdClassroom
			    AND b.status = 'ACTIVE'
			    AND (b.start < :queryFinish  AND b.finish > :queryStart)
			""")
	List<Booking> findActiveBookingsForClassroomByPeriod(int queryIdClassroom, LocalDateTime queryStart,
			LocalDateTime queryFinish);

	// Returns a list of occupied classrooms within a given period, based on active bookings.
	@Query(value = """
			SELECT DISTINCT b.idClassroom
			FROM bookings b
			WHERE b.status = 'ACTIVE'
			AND (
			(b.start < :queryFinish AND b.finish > :queryStart)
			)
			""", nativeQuery = true)
	List<Integer> findOccupiedClassroomsbyPeriod(LocalDateTime queryStart, LocalDateTime queryFinish);

	// Here, we are after the COMPLETE history of booking for a user, including active, cancelled, and complete ones,
	// Later, we will filter depending on our needs.
	@Query("""
			SELECT b FROM Booking b
			WHERE b.idUser = :queryIdUser
			ORDER BY b.start DESC
			""")
	List<Booking> findBookingsByUser(int queryIdUser);

}
