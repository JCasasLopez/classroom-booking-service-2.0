package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.mapper.TimeSlotMapper;

public class SlotAvailabilityMapperTest {

	private final WeeklySchedule weeklySchedule = 
			new WeeklySchedule(List.of("9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "CLOSED", "CLOSED"));
	private final TimeSlotMapper timeSlotMapper = new TimeSlotMapper();
	private final SlotAvailabilityMapper mapper = new SlotAvailabilityMapper(30, weeklySchedule, timeSlotMapper);
	
	@Test
	void buildAvailabilityGrid_returns_correct_availability_for_each_slot() {
	    // Arrange
	    Booking booking = new Booking(1L, 1, 1, 
	    	    LocalDateTime.of(2026, 5, 7, 9, 30),
	    	    LocalDateTime.of(2026, 5, 7, 10, 30),
	    	    LocalDateTime.now(), 
	    	    BookingStatus.ACTIVE);
	    
	    List<Booking> bookings = List.of(booking);

	    // Act
	    List<SlotStatusDto> grid = mapper.buildAvailabilityGrid(
	        bookings,
	        LocalDateTime.of(2026, 5, 7, 9, 0),
	        LocalDateTime.of(2026, 5, 7, 11, 0)  
	    );

	    // Assert
	    assertAll(
	        () -> assertTrue(grid.get(0).available()),
	        () -> assertNull(grid.get(0).idBooking()),
	        () -> assertFalse(grid.get(1).available()),
	        () -> assertEquals(booking.getIdBooking(), grid.get(1).idBooking()),
	        () -> assertFalse(grid.get(2).available()),
	        () -> assertEquals(booking.getIdBooking(), grid.get(2).idBooking()),
	        () -> assertTrue(grid.get(3).available()),
	        () -> assertNull(grid.get(3).idBooking())
	    );
	}
	
	@ParameterizedTest
	@MethodSource("TimePeriodsAndExpectedResults")
	void generateTimeSlotsForPeriod_generates_the_right_time_slots(int expectedSlots, LocalDateTime start, LocalDateTime finish) {
		// Arrange
		
		// Act
		List<TimeSlot> grid = mapper.generateTimeSlotsForPeriod(start, finish);
		
		// Assert
		assertEquals(expectedSlots, grid.size());
	}
	
	private static Stream<Arguments> TimePeriodsAndExpectedResults() {
		return Stream.of(
				// Friday 20:30 - Monday 9:30 → 2 slots with the weekend in-between (when it is closed)
				Arguments.of(2, LocalDateTime.of(2026, 5, 8, 20, 30), LocalDateTime.of(2026, 5, 11, 9, 30)),
				
				// Single day, full open day → 24 slots (9:00-21:00)
				Arguments.of(24, LocalDateTime.of(2026, 5, 7, 9, 0), LocalDateTime.of(2026, 5, 7, 21, 0)),

				// Period entirely on a closed day → 0 slots
				Arguments.of(0, LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 21, 0)),

				// Single slot
				Arguments.of(1, LocalDateTime.of(2026, 5, 7, 9, 0), LocalDateTime.of(2026, 5, 7, 9, 30)),
				
				// Spanning a few days . From Tuesday at 15 until Thursday at 11 → 40 slots
				Arguments.of(40, LocalDateTime.of(2026, 5, 5, 15, 0), LocalDateTime.of(2026, 5, 7, 11, 0))
				);
	}

}
