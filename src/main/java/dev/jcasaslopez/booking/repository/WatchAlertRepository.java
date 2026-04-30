package dev.jcasaslopez.booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.jcasaslopez.booking.domain.WatchAlert;

public interface WatchAlertRepository extends JpaRepository<WatchAlert, Long> {
	
	@Query("SELECT w FROM WatchAlert w WHERE w.userEmail = :userEmail")
	List<WatchAlert> findWatchAlertsByUser(String userEmail);

}