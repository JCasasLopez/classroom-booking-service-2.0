package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DayScheduleTest {

	@Test
	void daySchedule_with_finish_time_before_opening_time_throws_exception() {
		assertThrows(IllegalArgumentException.class, () -> new DaySchedule.Open(LocalTime.of(10, 0), LocalTime.of(9, 0)));	
	}
	
	@Test
	void daySchedule_with_finish_time_same_as_opening_time_throws_exception() {
		assertThrows(IllegalArgumentException.class, () -> new DaySchedule.Open(LocalTime.of(10, 0), LocalTime.of(10, 0)));	
	}
	
	@Test
	void daySchedule_with_null_as_opening_time_throws_exception() {
		assertThrows(NullPointerException.class, () -> new DaySchedule.Open(null, LocalTime.of(10, 0)));	
	}
	
	@Test
	void daySchedule_with_null_as_closing_time_throws_exception() {
		assertThrows(NullPointerException.class, () -> new DaySchedule.Open(LocalTime.of(10, 0), null));	
	}
	
}