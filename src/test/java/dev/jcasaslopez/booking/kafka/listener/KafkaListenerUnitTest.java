package dev.jcasaslopez.booking.kafka.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
	
@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {
	
	private static final String TOPIC_NAME = "mock-classrooms-topic";
	
	@Mock
	private ConsumerSeekCallback seekCallback;

	@Test
	void classroom_store_listener_should_seek_to_beginning_when_store_is_empty() {
		// Arrange
		List<ClassroomEvent> emptyStore = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(emptyStore);

		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		Map<TopicPartition, Long> assignments = Map.of(partition, 0L);

		// Act
		listener.onPartitionsAssigned(assignments, seekCallback);

		// Assert
		verify(seekCallback).seekToBeginning(assignments.keySet());
	}

	@Test
	void classroom_store_listener_should_not_seek_to_beginning_when_store_already_has_data() {
		// Arrange
		List<ClassroomEvent> populatedStore = new ArrayList<>();
		populatedStore.add(new ClassroomEvent(1, "Maths", 30, true, true));
		ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore);

		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		Map<TopicPartition, Long> assignments = Map.of(partition, 0L);

		// Act
		listener.onPartitionsAssigned(assignments, seekCallback);

		// Assert
		verify(seekCallback, never()).seekToBeginning(any());
	}
}
