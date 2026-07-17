package dev.jcasaslopez.booking.entity;

import java.time.LocalDateTime;

import dev.jcasaslopez.booking.enums.BookingStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// No JPA relationship with WatchAlert, even though WatchAlert references this entity
// via idBooking. The relationship is purely at the data level, not at the ORM level.
// The only use case that links them (fetching WatchAlerts for a given booking)
// is handled in WatchAlertRepository via an explicit query.
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
}
