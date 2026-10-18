package dev.jcasaslopez.booking.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="notification_outbox")
public class NotificationOutbox {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idNotification;
	private String subject;
	@Column(columnDefinition = "TEXT")
	private String message;
	private String emailAddress;
	private LocalDateTime timestamp;

	public NotificationOutbox(long idNotification, String subject, String message, String emailAddress) {
		this.idNotification = idNotification;
		this.subject = subject;
		this.message = message;
		this.emailAddress = emailAddress;
		this.timestamp = LocalDateTime.now();
	}

	public NotificationOutbox() {
	}

	public long getIdNotification() {
		return idNotification;
	}

	public String getSubject() {
		return subject;
	}

	public String getMessage() {
		return message;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setIdNotification(long idNotification) {
		this.idNotification = idNotification;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}