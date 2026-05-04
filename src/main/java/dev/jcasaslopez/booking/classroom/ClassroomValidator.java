package dev.jcasaslopez.booking.classroom;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Component
public class ClassroomValidator {

	    private final List<ClassroomEvent> classroomsStore;

	    public ClassroomValidator(List<ClassroomEvent> classroomsStore) {
	        this.classroomsStore = classroomsStore;
	    }

	    public void validateClassroomExists(int idClassroom) {
	        classroomsStore.stream()
	            .filter(c -> c.getIdClassroom() == idClassroom)
	            .findFirst()
	            .orElseThrow(() -> new NoSuchClassroomException("Classroom not found: " + idClassroom));
	    }
}