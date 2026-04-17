package dev.jcasaslopez.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="watch_alerts")
public class WatchAlert {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idWatchAlert;
	private long idBooking;
	
	// We persist the user's email address here to send notifications when a booking is cancelled.
	// Since the user must be authenticated when creating a watch alert, capturing the email at
	// creation time is straightforward. This avoids querying the User micro-service, which would
	// introduce both latency and coupling between micro-services.
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
