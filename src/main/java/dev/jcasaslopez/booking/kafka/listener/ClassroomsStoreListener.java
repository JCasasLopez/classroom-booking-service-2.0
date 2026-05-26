package dev.jcasaslopez.booking.kafka.listener;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
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
		// Only re-read from the beginning if classroomsStore is empty, meaning the micro-service
		// has just started after a crash.
		if (classroomsStore.isEmpty()) {
			callback.seekToBeginning(assignments.keySet());
		}
	}

	@KafkaHandler
	public void classroomListenerHandler(ClassroomEvent classroom) {
		int classroomId = classroom.getIdClassroom();
	    for (int i = 0; i < classroomsStore.size(); i++) {
	        if (classroomsStore.get(i).getIdClassroom() == classroomId) {
	            logger.info("Updating classroom {} in classroomsStore", classroomId);
	            classroomsStore.set(i, classroom);
	            return;
	        }
	    }
	    logger.info("Adding classroom {} ({}) to classroomsStore", classroomId, classroom.getName());
	    classroomsStore.add(classroom);
	}
}