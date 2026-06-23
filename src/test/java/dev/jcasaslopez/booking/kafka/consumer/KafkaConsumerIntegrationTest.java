package dev.jcasaslopez.booking.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.util.KafkaTestHelper;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

public class KafkaConsumerIntegrationTest extends BaseIntegrationTest {
	
	@Value("${kafka.topic.name.notifications}") private String topicName;
	
	private KafkaConsumer<String, NotificationEvent> consumer;
	
	private static final int SLOT_DURATION = 30;
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;

	@Test
    void kafka_producer_successfully_publishes_notification_event() {
        // Arrange
        consumer = KafkaTestHelper.createNotificationConsumer(kafkaContainer.getBootstrapServers(), topicName);
        TestHelper.createBooking(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);

        // Act
        List<ConsumerRecord<String, NotificationEvent>> records = KafkaTestHelper.pollRecords(consumer, Duration.ofSeconds(10));

        // Assert
        NotificationEvent event = records.get(0).value();

        // We only validate that the event payload has the correct structure (it is a NotificationEvent object).
        // We do not need to verify the specific content of every field, as: 
        // 1) That is already done in the BookingService unit test, and
        // 2) Coupling with NotificationType enumeration fields would creep into test.
        assertAll(
        	    () -> assertFalse(event.subject().isBlank()),
        	    () -> assertFalse(event.message().isBlank()),
        	    () -> assertFalse(event.emailAddress().isBlank()),
        	    () -> assertFalse(event.log().isBlank())
        	);

        consumer.close();
    }
}