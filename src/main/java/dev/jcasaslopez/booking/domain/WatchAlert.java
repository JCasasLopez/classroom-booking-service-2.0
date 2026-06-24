package dev.jcasaslopez.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// idBooking references an existing booking but there is no JPA relationship with Booking.
// WatchAlert has a completely independent lifecycle: it is never created, updated,
// or deleted through Booking.
@Entity
@Table(name="watch_alerts")
public class WatchAlert {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idWatchAlert;
	private long idBooking;
	
	// We store 'userEmail' instead of 'idUser' because when a booking is cancelled, the current thread belongs 
	// to the canceller, making it impossible to fetch the watchers' emails from a ThreadLocal context.
	// This avoids executing a synchronous HTTP request to the User service for each subscriber (preventing HTTP 
	// N+1 latency and tight service coupling).
	// Safe to use because once an account is created, the user email cannot be changed.
	private String userEmail;

	public WatchAlert(long idWatchAlert, long idBooking, String userEmail) {
		this.idWatchAlert = idWatchAlert;
		this.idBooking = idBooking;
		this.userEmail = userEmail;
	}

	public WatchAlert() {
	}

	public long getIdWatchAlert() {
		return idWatchAlert;
	}

	public void setIdWatchAlert(long idWatchAlert) {
		this.idWatchAlert = idWatchAlert;
	}

	public long getIdBooking() {
		return idBooking;
	}

	public void setIdBooking(long idBooking) {
		this.idBooking = idBooking;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	
}
