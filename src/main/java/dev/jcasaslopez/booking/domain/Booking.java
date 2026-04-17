package dev.jcasaslopez.booking.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="bookings")
public class Booking {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idBooking;
	private int idUser;
	private int idClassroom;
	private LocalDateTime start;
	private LocalDateTime finish;
	private LocalDateTime timestamp;
	
	@Enumerated(EnumType.STRING)
	private BookingStatus status;
	
	// Unidirectional one-to-many with no cascade: WatchAlert has its own independent
	// lifecycle (it is never created, updated, or removed through Reserva), so this
	// is an association, not a composition. WatchAlert entities are managed separately.
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "idBooking")	
	private List<WatchAlert> watchAlerts = new ArrayList<>();
	
	public Booking(long idBooking, int idUser, int idClassroom, LocalDateTime start, LocalDateTime finish,
			LocalDateTime timestamp, BookingStatus status) {
		
		this.idBooking = idBooking;
		this.idUser = idUser;
		this.idClassroom = idClassroom;
		this.start = start;
		this.finish = finish;
		this.timestamp = timestamp;
		this.status = status;

	}

	public Booking() {
	}

	public long getIdBooking() {
		return idBooking;
	}

	public void setIdBooking(long idBooking) {
		this.idBooking = idBooking;
	}

	public int getIdUser() {
		return idUser;
	}

	public void setIdUser(int idUser) {
		this.idUser = idUser;
	}

	public int getIdClassroom() {
		return idClassroom;
	}

	public void setIdClassroom(int idClassroom) {
		this.idClassroom = idClassroom;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	public LocalDateTime getFinish() {
		return finish;
	}

	public void setFinish(LocalDateTime finish) {
		this.finish = finish;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public BookingStatus getStatus() {
		return status;
	}

	public void setStatus(BookingStatus status) {
		this.status = status;
	}

	public List<WatchAlert> getWatchAlerts() {
		return watchAlerts;
	}

	public void setWatchAlerts(List<WatchAlert> watchAlerts) {
		this.watchAlerts = watchAlerts;
	}
	
}
