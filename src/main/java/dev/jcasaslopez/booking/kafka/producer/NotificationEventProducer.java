package dev.jcasaslopez.booking.kafka.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Component
public class NotificationEventProducer {
	
	@Value("${kafka.topic.name.notifications}")
	private String topicName;
	private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

	public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}
	
	public void sendNotification(NotificationEvent notificationEvent) {
		try {
			kafkaTemplate.send(topicName, notificationEvent).join();
		} catch (Exception ex) {
		    throw new RuntimeException("Error sending Kafka message to topic: " + topicName, ex);
		}
		
	}

}