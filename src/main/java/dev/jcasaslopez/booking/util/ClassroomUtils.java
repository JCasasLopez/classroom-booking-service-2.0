package dev.jcasaslopez.booking.util;

import java.util.List;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.exception.ClassroomNotFoundException;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class ClassroomUtils {
		
	public static String findClassroomName (Booking booking, List<ClassroomEvent> classroomsStore) {
		int targetIdClassroom = booking.getIdClassroom();
		ClassroomEvent targetClassroom = classroomsStore.stream()
		        .filter(c -> c.idClassroom() == targetIdClassroom)
		        .findFirst()
		        .orElseGet(() -> {
		        	throw new ClassroomNotFoundException(String.format("No classrooms with id:%s were found", targetIdClassroom));
        });
		return targetClassroom.name();
	}

}
