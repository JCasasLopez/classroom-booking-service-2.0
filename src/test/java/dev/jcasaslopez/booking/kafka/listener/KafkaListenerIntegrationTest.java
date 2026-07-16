package dev.jcasaslopez.booking.kafka.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class KafkaListenerIntegrationTest extends BaseIntegrationTest {
	
	@Value("${kafka.topic.name.classrooms}") private String topicName;
	
	@Autowired private KafkaTemplate<String, ClassroomEvent> kafkaTemplate;
	
	private static final int ID_CLASSROOM = 1;
	private static final String NEW_CLASSROOM_NAME = "Updated name";
	
	@Test
	void classroomsStore_gets_updated_when_a_new_classroom_is_persisted_in_Kafka_broker() {
		// Arrange
		ClassroomEvent updatedClassroom = new ClassroomEvent(ID_CLASSROOM, NEW_CLASSROOM_NAME, 150, true, true);
		
		// Act
		kafkaTemplate.send(topicName, String.valueOf(ID_CLASSROOM), updatedClassroom).join();
		
		// Assert
		waitForClassroomUpdate(classroomsStore, ID_CLASSROOM, NEW_CLASSROOM_NAME);
	}
	
	private void waitForClassroomUpdate(List<ClassroomEvent> classroomsStore, int classroomId, String expectedName) {
		Awaitility.await()
		.atMost(15, TimeUnit.SECONDS)
		.pollInterval(500, TimeUnit.MILLISECONDS)
		.untilAsserted(() -> {
			ClassroomEvent foundClassroom = classroomsStore.stream()
					.filter(c -> c.getIdClassroom() == classroomId)
					.findAny()
					.orElseThrow();

			assertEquals(expectedName, foundClassroom.getName());
		});
	}
}