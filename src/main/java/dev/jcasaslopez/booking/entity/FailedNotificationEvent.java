package dev.jcasaslopez.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="failed_notification_events")
public class FailedNotificationEvent {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long idNotification;
	private String subject;
	@Column(columnDefinition = "TEXT")
	private String emailBody;
	private String emailAddress;

	public FailedNotificationEvent(long idNotification, String subject, String emailBody, String emailAddress) {
		this.idNotification = idNotification;
		this.subject = subject;
		this.emailBody = emailBody;
		this.emailAddress = emailAddress;
	}

	public FailedNotificationEvent() {
	}

	public long getIdNotification() {
		return idNotification;
	}

	public String getSubject() {
		return subject;
	}

	public String getEmailBody() {
		return emailBody;
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

	public void setEmailBody(String emailBody) {
		this.emailBody = emailBody;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
}