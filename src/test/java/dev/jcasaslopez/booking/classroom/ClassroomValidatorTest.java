package dev.jcasaslopez.booking.classroom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class ClassroomValidatorTest {

	private List<ClassroomEvent> classrooms = List.of(
			new ClassroomEvent(101, "Main Hall", 150, true, true),
			new ClassroomEvent(205, "Physics Lab", 30, true, false),
			new ClassroomEvent(310, "Seminar Room", 20, false, false),
			new ClassroomEvent(402, "Multimedia Auditorium", 80, true, true)
			);

	private ClassroomValidator classroomValidator = new ClassroomValidator(classrooms);

	@Test
	void does_not_throw_exception_if_the_classroom_exists() {
		// Arrange

		// Act & Assert
		assertDoesNotThrow(() -> classroomValidator.validateClassroomExists(101));
	}

	@Test
	void throws_exception_if_the_classroom_does_not_exist() {
		// Arrange

		// Act & Assert
		assertThrows(NoSuchClassroomException.class, () -> classroomValidator.validateClassroomExists(103));
	}
}