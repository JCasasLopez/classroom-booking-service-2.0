package dev.jcasaslopez.booking.kafka.listener;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

import dev.jcasaslopez.booking.repository.ClassroomLockRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
	
@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {
	
	private static final String TOPIC_NAME = "mock-classrooms-topic";
	private static final int ID_CLASSROOM = 1;
	
	private ClassroomEvent classroom = new ClassroomEvent(ID_CLASSROOM, "Maths", 30, true, true);
	
	@Mock private ConsumerSeekCallback seekCallback;
	@Mock private ClassroomLockRepository classroomLockRepository;
	@Mock private List<ClassroomEvent> storeMock;

	@Test
	void seeks_to_beginning_when_store_empty() {
		// Arrange
		List<ClassroomEvent> emptyStore = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(emptyStore, classroomLockRepository);

		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		Map<TopicPartition, Long> assignments = Map.of(partition, 0L);

		// Act
		listener.onPartitionsAssigned(assignments, seekCallback);

		// Assert
		verify(seekCallback).seekToBeginning(assignments.keySet());
	}

	@Test
	void does_not_seek_to_beginning_when_store_populated() {
		// Arrange
		List<ClassroomEvent> populatedStore = new ArrayList<>();
		populatedStore.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore, classroomLockRepository);

		TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);
		Map<TopicPartition, Long> assignments = Map.of(partition, 0L);

		// Act
		listener.onPartitionsAssigned(assignments, seekCallback);

		// Assert
		verify(seekCallback, never()).seekToBeginning(any());
	}
	
	@Test
	void deletes_classroom_from_store_on_tombstone(){
		// Arrange
		List<ClassroomEvent> populatedStore = new ArrayList<>();
		populatedStore.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, null);
		
		// Assert
		assertTrue(populatedStore.isEmpty());
	}
	

	@Test
	void updates_classroom_on_store_if_already_exists(){
		// Arrange
		List<ClassroomEvent> store = new ArrayList<>();
		store.add(classroom);
		ClassroomsStoreListener listener = new ClassroomsStoreListener(store, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, new ClassroomEvent(ID_CLASSROOM, "Maths", 50, true, true));
		ClassroomEvent classroomStored = store.get(0);
		
		// Assert
		assertEquals(50, classroomStored.seats());
	}
	
	@Test
	void adds_classroom_to_store_if_does_not_exist(){
		// Arrange
		List<ClassroomEvent> store = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(store, classroomLockRepository);

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
	
	@Test
	void persists_lock_when_store_empty() {
		// Arrange
		List<ClassroomEvent> emptyStore = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(emptyStore, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, classroom);

		// Assert
		verify(classroomLockRepository).save(argThat(lock -> lock.getIdClassroom() == ID_CLASSROOM));

	}
	
	@Test
	void persists_lock_even_if_store_has_classroom() {
	    // Arrange
	    List<ClassroomEvent> populatedStore = new ArrayList<>(List.of(classroom));
	    ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore, classroomLockRepository);

	    // Act
	    listener.classroomListenerHandler(ID_CLASSROOM, classroom);

	    // Assert
	    verify(classroomLockRepository).save(argThat(lock -> lock.getIdClassroom() == ID_CLASSROOM));
	}
	
	@Test
	void deletes_lock_even_if_classroom_absent_from_store() {
		// Arrange
		List<ClassroomEvent> emptyStore = new ArrayList<>();
		ClassroomsStoreListener listener = new ClassroomsStoreListener(emptyStore, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, null);

		// Assert
		verify(classroomLockRepository).deleteById(ID_CLASSROOM);
	}
	
	@Test
	void deletes_lock_when_store_empty() {
		// Arrange
		List<ClassroomEvent> populatedStore = new ArrayList<>(List.of(classroom));
	    ClassroomsStoreListener listener = new ClassroomsStoreListener(populatedStore, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, null);

		// Assert
		verify(classroomLockRepository).deleteById(ID_CLASSROOM);
	}
	
	// The lock acts as a gatekeeper: if saving to the database fails, the classroom never 
	// enters the in-memory store. This ensures the application never processes a classroom without its 
	// persistence lock guaranteed first.
	@Test
	void persists_lock_first_then_adds_classroom_to_store() {
		// Arrange
		ClassroomsStoreListener listener = new ClassroomsStoreListener(storeMock, classroomLockRepository);
		
		InOrder inOrder = inOrder(classroomLockRepository, storeMock);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, classroom);

		// Assert
		inOrder.verify(classroomLockRepository).save(any());
		inOrder.verify(storeMock).add(any()); 
	}
	
	// The lock acts as a gatekeeper: ensuring the persistence lock is updated 
	// first guarantees the database stays in sync before modifying the in-memory store.
	@Test
	void persists_lock_first_then_updates_classroom_in_store () {
		// Arrange
		when(storeMock.size()).thenReturn(1);
		when(storeMock.get(0)).thenReturn(classroom);		
		
		ClassroomsStoreListener listener = new ClassroomsStoreListener(storeMock, classroomLockRepository);
		
		InOrder inOrder = inOrder(classroomLockRepository, storeMock);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, classroom);

		// Assert
		inOrder.verify(classroomLockRepository).save(any());
		inOrder.verify(storeMock).set(anyInt(), any()); 
	}
	
	// Removing it from memory first immediately hides it from the system, preventing race conditions. 
	// Deleting the database lock is just the final cleanup step.
	@Test
	void removes_classroom_from_store_first_then_deletes_lock() {
		// Arrange
		ClassroomsStoreListener listener = new ClassroomsStoreListener(storeMock, classroomLockRepository);
		
		InOrder inOrder = inOrder(storeMock, classroomLockRepository);

		// Act
		listener.classroomListenerHandler(ID_CLASSROOM, null);

		// Assert
		inOrder.verify(storeMock).removeIf(any()); 
		inOrder.verify(classroomLockRepository).deleteById(ID_CLASSROOM);
	}
}
