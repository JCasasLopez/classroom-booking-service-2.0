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

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Component
@KafkaListener(topics = "${kafka.topic.name.classrooms}")
public class ClassroomsStoreListener implements ConsumerSeekAware {
	
	private static final Logger logger = LoggerFactory.getLogger(ClassroomsStoreListener.class);

	// This bean represents the list of classrooms.
	private List<ClassroomEvent> classroomsStore;

	public ClassroomsStoreListener(List<ClassroomEvent> classroomsStore) {
		this.classroomsStore = classroomsStore;
	}

	@Override
	public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
		// Kafka's 'earliest' config is ignored if a valid offset already exists. Since a crash wipes our RAM bean 
		// but keeps Kafka's offset, we must programmatically force a seek-to-beginning to rebuild the local store.
		if (classroomsStore.isEmpty()) {
			callback.seekToBeginning(assignments.keySet());
		}
	}

	@KafkaHandler
	public void classroomListenerHandler(
	        @Header(KafkaHeaders.RECEIVED_KEY) Integer classroomId, 
	        @Payload(required = false) ClassroomEvent classroom) {  
	    
		// Handle Kafka tombstone (classroom deletion)
	    if (classroom == null) {
	        for (int i = 0; i < classroomsStore.size(); i++) {
	            if (classroomsStore.get(i).getIdClassroom() == classroomId) {
	                logger.info("Removing classroom {} from classroomsStore due to Tombstone", classroomId);
	                classroomsStore.remove(i);
	                return;
	            }
	        }
	        return; 
	    }

	    for (int i = 0; i < classroomsStore.size(); i++) {
	        if (classroomsStore.get(i).getIdClassroom() == classroomId) {
	            logger.info("Updating classroom {} ({}) in classroomsStore", classroomId, classroom.getName());
	            classroomsStore.set(i, classroom);
	            return;
	        }
	    }
	    logger.info("Adding classroom {} ({}) to classroomsStore", classroomId, classroom.getName());
	    classroomsStore.add(classroom);
	}
}