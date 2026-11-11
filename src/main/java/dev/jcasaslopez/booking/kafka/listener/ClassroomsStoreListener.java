package dev.jcasaslopez.booking.kafka.listener;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.entity.ClassroomLock;
import dev.jcasaslopez.booking.repository.ClassroomLockRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Component
@KafkaListener(topics = "${kafka.topic.name.classrooms}")
public class ClassroomsStoreListener implements ConsumerSeekAware {
	
	private static final Logger logger = LoggerFactory.getLogger(ClassroomsStoreListener.class);

	// This bean represents the list of classrooms.
	private List<ClassroomEvent> classroomsStore;
	private ClassroomLockRepository classroomLockRepository;

	public ClassroomsStoreListener(List<ClassroomEvent> classroomsStore,
			ClassroomLockRepository classroomLockRepository) {
		this.classroomsStore = classroomsStore;
		this.classroomLockRepository = classroomLockRepository;
	}

	@Override
	public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
		// If classroomsStore is empty, the bean has just started or recovered from a crash. Since Kafka remembers
		// the last committed offset, setting 'auto.offset.reset=earliest' is not enough, as this is only used
		// when there is no offset recorded.  We must explicitly force a seek-to-beginning to re-hydrate 
		// classroomsStore by replaying all historical events (classrooms) up to the latest state.
		if (classroomsStore.isEmpty()) {
			callback.seekToBeginning(assignments.keySet());
		}
	}

	@KafkaHandler
	public void classroomListenerHandler(
	        @Header(KafkaHeaders.RECEIVED_KEY) Integer classroomId,
	        @Payload(required = false) ClassroomEvent classroom) {

	    if (classroom == null) {   // tombstone
	        removeFromStore(classroomId);
	        logger.info("Removing classroom {} from table ClassroomLock", classroomId);
	        classroomLockRepository.deleteById(classroomId);
	        return;
	    }

	    // Lock row first: if this fails, the classroom does not reach the store
	    classroomLockRepository.save(new ClassroomLock(classroomId));
	    upsertInStore(classroomId, classroom);
	}

	private void removeFromStore(int classroomId) {
	    if (classroomsStore.removeIf(c -> c.idClassroom() == classroomId)) {
	        logger.info("Removing classroom {} from classroomsStore due to Tombstone", classroomId);
	    }
	}

	private void upsertInStore(int classroomId, ClassroomEvent classroom) {
	    for (int i = 0; i < classroomsStore.size(); i++) {
	        if (classroomsStore.get(i).idClassroom() == classroomId) {
	            logger.info("Updating classroom {} ({}) in classroomsStore", classroomId, classroom.name());
	            classroomsStore.set(i, classroom);
	            return;
	        }
	    }
	    logger.info("Adding classroom {} ({}) to classroomsStore", classroomId, classroom.name());
	    classroomsStore.add(classroom);
	}
}