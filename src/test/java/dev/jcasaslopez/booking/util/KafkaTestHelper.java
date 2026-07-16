package dev.jcasaslopez.booking.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

public final class KafkaTestHelper {

	// Wait until the classrooms Kafka topic is available before running the test.
	public static void waitForClassroomStore(List<?> classroomsStore) {
		Awaitility.await()
		.atMost(15, TimeUnit.SECONDS)
		.pollInterval(500, TimeUnit.MILLISECONDS)
		.until(() -> !classroomsStore.isEmpty());
	}

	public static void produceClassroomEvents(String bootstrapServers) throws Exception {
		try (KafkaProducer<String, ClassroomEvent> producer = new KafkaProducer<>(buildProducerProps(bootstrapServers))) {
			for (ClassroomEvent classroom : buildClassroomEvents()) {
				producer.send(new ProducerRecord<>("classrooms", String.valueOf(classroom.getIdClassroom()), classroom)).get();			}
		}
	}

	private static Properties buildProducerProps(String bootstrapServers) {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
		return props;
	}

	private static List<ClassroomEvent> buildClassroomEvents() {
		return List.of(
				new ClassroomEvent(1, "Salón de Actos", 150, true, true),
				new ClassroomEvent(2, "Aula 102", 25, false, false),
				new ClassroomEvent(3, "Aula Marie Curie", 45, false, true)
				);
	}
	
	 public static KafkaConsumer<String, NotificationEvent> createNotificationConsumer(String bootstrapServers, String topic) {
	        Map<String, Object> props = new HashMap<>();
	        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
	        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-notifications-" + UUID.randomUUID());
	        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
	        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
	        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
	        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
	        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationEvent.class.getName());

	        KafkaConsumer<String, NotificationEvent> consumer = new KafkaConsumer<>(props);
	        consumer.subscribe(List.of(topic));
	        return consumer;
	    }

	 public static List<ConsumerRecord<String, NotificationEvent>> pollRecords(
		        KafkaConsumer<String, NotificationEvent> consumer, Duration timeout) {
		    ConsumerRecords<String, NotificationEvent> records = consumer.poll(timeout);
		    List<ConsumerRecord<String, NotificationEvent>> result = new ArrayList<>();
		    records.forEach(result::add);
		    return result;
		}

}
