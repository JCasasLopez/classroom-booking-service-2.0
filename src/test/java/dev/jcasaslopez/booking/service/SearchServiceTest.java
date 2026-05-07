package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
	
	@Mock BookingRepository bookingRepository;
	@InjectMocks SearchServiceImpl searchService;
	
	private static List<ClassroomEvent> allClassrooms = List.of(
		    new ClassroomEvent(1, "Main Auditorium", 150, true, true),
		    new ClassroomEvent(2, "Standard Seminar Room", 30, true, false),
		    new ClassroomEvent(3, "Advanced Tech Lab", 25, true, true),
		    new ClassroomEvent(4, "Small Study Group", 10, false, false),
		    new ClassroomEvent(5, "Conference Hall A", 80, true, true),
		    new ClassroomEvent(6, "Creative Workshop Space", 40, false, true),
		    new ClassroomEvent(7, "Lecture Theatre B", 120, true, true),
		    new ClassroomEvent(8, "Quiet Reading Room", 15, false, false)
		);
	
	@BeforeEach
	void setUp() {
	    ReflectionTestUtils.setField(searchService, "classroomsStore", allClassrooms);
	}
	
	@Test
	void classroomsAvailableByPeriod_returns_the_expected_list() {
		// Arrange
		List<Integer> occupiedClassrooms = List.of(1, 2, 4, 6);
		when(bookingRepository.findOccupiedClassroomsbyPeriod(LocalDateTime.of(2026, 5, 7, 11, 0), LocalDateTime.of(2026, 5, 7, 21, 0)))
										.thenReturn(occupiedClassrooms);
		
		// Act
		List<ClassroomEvent> availableClassrooms = searchService.classroomsAvailableByPeriod(LocalDateTime.of(2026, 5, 7, 11, 0), LocalDateTime.of(2026, 5, 7, 21, 0));
		
		// Assert
		assertAll(
			    () -> assertEquals(4, availableClassrooms.size()),
			    () -> assertTrue(availableClassrooms.stream()
			            .map(ClassroomEvent::getIdClassroom)
			            .toList()
			            .containsAll(List.of(3, 5, 7, 8)))
			);
	}
	
	@ParameterizedTest
	@MethodSource("featuresFilterProvider")
	void classroomsAvailableByPeriodAndFeatures_returns_the_expected_list(
	        int seats, boolean projector, boolean speakers, List<Integer> expectedIds) {
		// Arrange
		when(bookingRepository.findOccupiedClassroomsbyPeriod(LocalDateTime.of(2026, 5, 7, 11, 0), LocalDateTime.of(2026, 5, 7, 21, 0)))
										.thenReturn(List.of());
		
		// Act
		List<ClassroomEvent> availableClassrooms = searchService.classroomsAvailableByPeriodAndFeatures
				(LocalDateTime.of(2026, 5, 7, 11, 0), LocalDateTime.of(2026, 5, 7, 21, 0), seats, projector, speakers);
		
		// Assert
		assertAll(
			    () -> assertEquals(expectedIds.size(), availableClassrooms.size()),
			    () -> assertTrue(availableClassrooms.stream()
			            .map(ClassroomEvent::getIdClassroom)
			            .toList()
			            .containsAll(expectedIds))
			);
	}
	
	static Stream<Arguments> featuresFilterProvider() {
	    return Stream.of(
	    		// Conditions -> Seats, projector, speakers, idClassrooms of classrooms available.
	            Arguments.of(100, true,  false, List.of(1, 7)),
	            Arguments.of(30, true, false,  List.of(1, 2, 5, 7)),
	            Arguments.of(40, true,  true,  List.of(1, 5, 7)),
	            Arguments.of(20, false, true, List.of(1, 3, 5, 6, 7))
	    );
	}
}