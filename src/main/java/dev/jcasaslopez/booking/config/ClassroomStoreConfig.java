package dev.jcasaslopez.booking.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

// This bean represents the list of classrooms that obtains the classrooms 
// by reading the corresponding Kafka topic (see 'listener' folder).
@Configuration
public class ClassroomStoreConfig {

	// ClassroomEvent doubles as a DTO here: it carries exactly the classroom
	// data this service needs, with no divergence expected.
	@Bean
	List<ClassroomEvent> classroomsStore() {
		return new ArrayList<ClassroomEvent>();
	}

}