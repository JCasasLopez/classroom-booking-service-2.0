package dev.jcasaslopez.booking.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class BookingMapperTest {
	
	// We instantiate BookingMapper by hand instead so we do not need to use Spring and the whole context
	private final static BookingMapper mapper = new BookingMapper();
	
	private final WeeklySchedule weeklySchedule = 
			new WeeklySchedule(List.of("9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "CLOSED", "CLOSED"));

	private final List<ClassroomEvent> classroomsStore = List.of(
		    new ClassroomEvent(1, "Blue Whale Auditorium", 30, true, true),
		    new ClassroomEvent(2, "201", 200, true, true),
		    new ClassroomEvent(5, "The Think Tank Room", 8, false, false)
		);
	
	@BeforeAll
	static void setup() {
	    ReflectionTestUtils.setField(mapper, "slotDuration", 30);
	}
	
	@Test
	void when_slots_are_valid_mapper_returns_correct_entity () {
		// Arrange
		BookingRequestDto bookingRequestDto = new BookingRequestDto(1, 1, 
				// The list has to be mutable, since it will have to be sorted in convertStartToTimeSlots()
				new ArrayList<>(List.of(
					    LocalDateTime.of(2026, 4, 22, 10, 0),
					    LocalDateTime.of(2026, 4, 22, 10, 30),
					    LocalDateTime.of(2026, 4, 22, 11, 0)
					)));
	
		// Act
		Booking mappedBooking = mapper.toEntity(bookingRequestDto, weeklySchedule);
		
		// Assert
		assertAll(() -> assertEquals(1, mappedBooking.getIdUser()),
				() -> assertEquals(1, mappedBooking.getIdClassroom()),
				() -> assertEquals(LocalDateTime.of(2026, 4, 22, 10, 0), mappedBooking.getStart()),
				() -> assertEquals(LocalDateTime.of(2026, 4, 22, 11, 30), mappedBooking.getFinish()),
				() -> assertEquals(BookingStatus.ACTIVE, mappedBooking.getStatus())
				);
		
	}
	
	@Test
	void when_classroom_is_present_returns_correct_ResponseDto () {
		// Arrange
		Booking booking = new Booking(0, 1, 1, LocalDateTime.of(2026, 4, 22, 10, 0), LocalDateTime.of(2026, 4, 22, 11, 30), 
									LocalDateTime.now(), BookingStatus.ACTIVE);
		
		// Act
		BookingResponseDto mappedBooking = mapper.toResponseDto(booking, classroomsStore);
		
		// Assert
		assertAll(() -> assertEquals("Blue Whale Auditorium", mappedBooking.name()),
				() -> assertEquals(LocalDateTime.of(2026, 4, 22, 10, 0), mappedBooking.start()),
				() -> assertEquals(LocalDateTime.of(2026, 4, 22, 11, 30), mappedBooking.finish())
				);
	}
	
	@Test
	void when_classroom_is_not_present_throws_NoSuchClassroomException () {
		// Arrange
		Booking booking = new Booking(0, 1, 8, LocalDateTime.of(2026, 4, 22, 10, 0), LocalDateTime.of(2026, 4, 22, 11, 30), 
									LocalDateTime.now(), BookingStatus.ACTIVE);
				
		// Act & Assert
		assertThrows(NoSuchClassroomException.class, () -> mapper.toResponseDto(booking, classroomsStore));
	}
}
