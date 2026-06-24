package dev.jcasaslopez.booking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.jcasaslopez.booking.domain.WatchAlert;

public interface WatchAlertRepository extends JpaRepository<WatchAlert, Long> {
	
	@Query("""
		    SELECT wa FROM WatchAlert wa
		    JOIN Booking b ON b.idBooking = wa.idBooking
		    WHERE wa.userEmail = :userEmail
		    AND b.start <= :finish
		    AND b.finish >= :start
		    """)
	List<WatchAlert> findWatchAlertsByUserAndTimePeriod(String userEmail, LocalDateTime start, LocalDateTime finish);
	
	// This query bypasses ORM-level mappings (@ManyToOne) to keep Booking and WatchAlert lifecycles
	// strictly independent, avoiding accidental cascading or overhead during booking fetches.
	@Query("SELECT w FROM WatchAlert w WHERE w.idBooking = :idBooking")
	List<WatchAlert> findWatchAlertsByBooking(long idBooking);

}