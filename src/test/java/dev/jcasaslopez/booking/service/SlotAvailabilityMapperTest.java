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

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.mapper.TimeSlotMapper;

public class SlotAvailabilityMapperTest {

	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	private static final int SLOT_DURATION_IN_MINUTES_30 = SLOT_DURATION_30.minutes();

	private static final SlotDuration SLOT_DURATION_60 = new SlotDuration(60);
	private static final int SLOT_DURATION_IN_MINUTES_60 = SLOT_DURATION_60.minutes();

	private final WeeklySchedule weeklySchedule = 
			new WeeklySchedule(List.of("9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "CLOSED", "CLOSED"), 
					SLOT_DURATION_30);
	private final TimeSlotMapper timeSlotMapper = new TimeSlotMapper();

	private SlotAvailabilityMapper mapperWith(SlotDuration slotDuration) {
		return new SlotAvailabilityMapper(slotDuration, weeklySchedule, timeSlotMapper);
	}

	@Test
	void buildAvailabilityGrid_returns_correct_availability_for_each_slot() {
		// Arrange
		SlotAvailabilityMapper mapper = mapperWith(SLOT_DURATION_30);
		Booking booking = new Booking(1L, 1, 1,
				LocalDateTime.of(2026, 5, 7, 9, 30),
				LocalDateTime.of(2026, 5, 7, 11, 0),
				LocalDateTime.now(),
				BookingStatus.ACTIVE);

		// Act
		List<SlotStatusDto> grid = mapper.buildAvailabilityGrid(
				List.of(booking),
				LocalDateTime.of(2026, 5, 7, 9, 0),
				LocalDateTime.of(2026, 5, 7, 11, 30));

		// Assert
		assertAll(
				() -> assertEquals(5, grid.size()),
				() -> assertTrue(grid.get(0).available()),                              // 9:00-9:30
				() -> assertNull(grid.get(0).idBooking()),
				() -> assertFalse(grid.get(1).available()),                             // 9:30-10:00 (same start)
				() -> assertEquals(booking.getIdBooking(), grid.get(1).idBooking()),
				() -> assertFalse(grid.get(2).available()),                             // 10:00-10:30 (inside)
				() -> assertEquals(booking.getIdBooking(), grid.get(2).idBooking()),
				() -> assertFalse(grid.get(3).available()),                             // 10:30-11:00 (same finish)
				() -> assertEquals(booking.getIdBooking(), grid.get(3).idBooking()),
				() -> assertTrue(grid.get(4).available()),                              // 11:00-11:30
				() -> assertNull(grid.get(4).idBooking())
				);
	}

	@ParameterizedTest
	@MethodSource("timePeriodsAndExpectedResults30MinSlots")
	void generateTimeSlotsForPeriod_generates_the_right_time_slots_30(
			int slotDuration, int expectedSlots, LocalDateTime start, LocalDateTime finish) {
		// Arrange
		SlotAvailabilityMapper mapper = mapperWith(SLOT_DURATION_30);

		// Act
		List<TimeSlot> grid = mapper.generateTimeSlotsForPeriod(start, finish);

		// Assert
		assertEquals(expectedSlots, grid.size());
	}

	private static Stream<Arguments> timePeriodsAndExpectedResults30MinSlots() {
		return Stream.of(
				// 30 min slots: Friday 20:30 - Monday 9:30 → 2 slots with the weekend in-between (when it is closed)
				Arguments.of(SLOT_DURATION_IN_MINUTES_30, 2, LocalDateTime.of(2026, 5, 8, 20, 30), LocalDateTime.of(2026, 5, 11, 9, 30)),

				// 30 min slots: single day, full open day → 24 slots (9:00-21:00)
				Arguments.of(SLOT_DURATION_IN_MINUTES_30, 24, LocalDateTime.of(2026, 5, 7, 9, 0), LocalDateTime.of(2026, 5, 7, 21, 0)),

				// 30 min slots: period entirely on a closed day → 0 slots
				Arguments.of(SLOT_DURATION_IN_MINUTES_30, 0, LocalDateTime.of(2026, 5, 9, 9, 0), LocalDateTime.of(2026, 5, 9, 21, 0)),

				// 30 min slots: single slot
				Arguments.of(SLOT_DURATION_IN_MINUTES_30, 1, LocalDateTime.of(2026, 5, 7, 9, 0), LocalDateTime.of(2026, 5, 7, 9, 30)),

				// 30 min slots: spanning a few days. From Tuesday at 15 until Thursday at 11 → 40 slots
				Arguments.of(SLOT_DURATION_IN_MINUTES_30, 40, LocalDateTime.of(2026, 5, 5, 15, 0), LocalDateTime.of(2026, 5, 7, 11, 0))
				);
	}

	@ParameterizedTest
	@MethodSource("timePeriodsAndExpectedResults60MinSlots")
	void generateTimeSlotsForPeriod_generates_the_right_time_slots_60(
			int slotDuration, int expectedSlots, LocalDateTime start, LocalDateTime finish) {
		// Arrange
		SlotAvailabilityMapper mapper = mapperWith(SLOT_DURATION_60);

		// Act
		List<TimeSlot> grid = mapper.generateTimeSlotsForPeriod(start, finish);

		// Assert
		assertEquals(expectedSlots, grid.size());
	}

	private static Stream<Arguments> timePeriodsAndExpectedResults60MinSlots() {
		return Stream.of(
				// 60 min slots: single day, full open day → 12 slots (9:00-21:00)
				Arguments.of(SLOT_DURATION_IN_MINUTES_60, 12, LocalDateTime.of(2026, 5, 7, 9, 0), LocalDateTime.of(2026, 5, 7, 21, 0)),

				// 60 min slots: Friday 20:00 - Monday 10:00 → 2 slots with the weekend in-between (when it is closed)
				Arguments.of(SLOT_DURATION_IN_MINUTES_60, 2, LocalDateTime.of(2026, 5, 8, 20, 0), LocalDateTime.of(2026, 5, 11, 10, 0)),

				// 60 min slots: spanning a few days. From Tuesday at 15 until Thursday at 11 → 20 slots
				Arguments.of(SLOT_DURATION_IN_MINUTES_60, 20, LocalDateTime.of(2026, 5, 5, 15, 0), LocalDateTime.of(2026, 5, 7, 11, 0))
				);
	}

}