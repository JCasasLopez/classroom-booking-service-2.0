package dev.jcasaslopez.booking.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class ClassroomUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(ClassroomUtils.class);
	
	public static String findClassroomName (Booking booking, List<ClassroomEvent> classroomsStore) {
		int targetIdClassroom = booking.getIdClassroom();
		ClassroomEvent targetClassroom = classroomsStore.stream()
		        .filter(c -> c.getIdClassroom() == targetIdClassroom)
		        .findFirst()
		        .orElseGet(() -> {
		        	logger.warn("Classroom not found: idClassroom={}", targetIdClassroom);
		        	throw new NoSuchClassroomException(String.format("No classrooms with id:%s were found", targetIdClassroom));
        });
		return targetClassroom.getName();
	}

}
