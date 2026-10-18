package dev.jcasaslopez.booking.kafka.producer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.entity.NotificationOutbox;
import dev.jcasaslopez.booking.mapper.NotificationOutboxMapper;
import dev.jcasaslopez.booking.repository.OutboxRepository;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Component
public class NotificationEventProducer {

	private static final Logger logger = LoggerFactory.getLogger(NotificationEventProducer.class);

	@Value("${kafka.topic.name.notifications}") private String topicName;

	private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
	private final OutboxRepository repository;
	private final NotificationOutboxMapper mapper;

	public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
			OutboxRepository repository, NotificationOutboxMapper mapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.repository = repository;
		this.mapper = mapper;
	}

	@Scheduled(fixedDelayString = "${notification.outbox.poll-delay-ms}")
	public void processOutboxEvents() {
		List<NotificationOutbox> pending = repository.findAll();

		for (NotificationOutbox failed : pending) {
			try {
				NotificationEvent event = mapper.toKafkaRecord(failed);
				kafkaTemplate.send(topicName, event).get(1, java.util.concurrent.TimeUnit.SECONDS);

				repository.delete(failed);
			} catch (Exception ex) {
				logger.warn("Kafka still down or send failed, will retry next cycle", ex);
				return; 
			}
		}
	}

}