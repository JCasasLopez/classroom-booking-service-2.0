package dev.jcasaslopez.booking.kafka.listener;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
	
@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {
	
	private static final String TOPIC_NAME = "mock-classrooms-topic";
	private static final int ID_CLASSROOM = 1;
	
	private ClassroomEvent classroom = new ClassroomEvent(ID_CLASSROOM, "Maths", 30, true, true);
	
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
		populatedStore.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore);

		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		Map<TopicPartition, Long> assignments = Map.of(partition, 0L);

		// Act
		listener.onPartitionsAssigned(assignments, seekCallback);

		// Assert
		verify(seekCallback, never()).seekToBeginning(any());
	}
	
	@Test
	void classroom_store_listener_deletes_classroom_when_receives_tombstone(){
		// Arrange
		List<ClassroomEvent> populatedStore = new ArrayList<>();
		populatedStore.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, null);
		
		// Assert
		assertTrue(populatedStore.isEmpty());
	}
	

	@Test
	void classroom_store_listener_updates_classroom_if_already_exists(){
		// Arrange
		List<ClassroomEvent> store = new ArrayList<>();
		store.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(store);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, new ClassroomEvent(ID_CLASSROOM, "Maths", 50, true, true));
		ClassroomEvent classroomStored = store.get(0);
		
		// Assert
		assertEquals(50, classroomStored.seats());
	}
	
	@Test
	void classroom_store_listener_adds_classroom_if_does_not_exist(){
		// Arrange
		List<ClassroomEvent> store = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(store);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, classroom);
		ClassroomEvent classroomStored = store.get(0);
		
		// Assert
		assertAll(
				() -> assertFalse(store.isEmpty()),
				() -> assertEquals(classroom.name(), classroomStored.name()),
				() -> assertEquals(classroom.seats(), classroomStored.seats()),
				() -> assertEquals(classroom.projector(), classroomStored.projector()),
				() -> assertEquals(classroom.speakers(), classroomStored.speakers())
				);
	}
}
