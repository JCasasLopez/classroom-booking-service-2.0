package dev.jcasaslopez.booking.util;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.springframework.kafka.support.serializer.JsonSerializer;

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

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
				producer.send(new ProducerRecord<>("classrooms", classroom)).get();
			}
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

}
