package dev.jcasaslopez.booking.kafka.producer;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.entity.FailedNotificationEvent;
import dev.jcasaslopez.booking.mapper.FailedNotificationEventMapper;
import dev.jcasaslopez.booking.repository.FailedNotificationEventRepository;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Component
public class NotificationEventProducer {

	private static final Logger logger = LoggerFactory.getLogger(NotificationEventProducer.class);

	@Value("${kafka.topic.name.notifications}") private String topicName;

	private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
	private final FailedNotificationEventRepository repository;
	private final FailedNotificationEventMapper mapper;

	public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
			FailedNotificationEventRepository repository, FailedNotificationEventMapper mapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.repository = repository;
		this.mapper = mapper;
	}

	public void sendNotification(NotificationEvent event) {
		try {
			publish(event);
		} catch (Exception ex) {
			logger.warn("Kafka send failed, persisting event for retry", ex);
			try {
				repository.save(mapper.toEntity(event));
			} catch (Exception saveEx) {
				logger.error("Could not persist failed event, notification lost", saveEx);
			}
		}
	}

	@Scheduled(fixedDelayString = "${kafka.retry.scheduler-fixed-delay-ms}")
	public void retryFailedNotifications() {
		List<FailedNotificationEvent> pending = repository.findAll();

		for (FailedNotificationEvent failed : pending) {
			try {
				publish(mapper.toKafkaRecord(failed));
				repository.delete(failed);
			} catch (Exception ex) {
				logger.warn("Retry failed, will try again next cycle", ex);
				return; // Kafka probably still down: stop and wait for the next cycle
			}
		}
	}

	private void publish(NotificationEvent event) {
		kafkaTemplate.send(topicName, event).join();
	}
}