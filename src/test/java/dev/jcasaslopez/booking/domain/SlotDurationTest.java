package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class SlotDurationTest {
		
	@ParameterizedTest
	@ValueSource(ints = {30, 60})
	void happy_path(int minutes) {
		assertDoesNotThrow(() -> new SlotDuration(minutes));
	}
	
	@ParameterizedTest
	@ValueSource(ints = {15, 20, 45})
	void slot_duration_throws_exception_with_invalid_values(int minutes) {
		assertThrows(IllegalArgumentException.class, () -> new SlotDuration(minutes));
	}
	
	@ParameterizedTest
	@MethodSource("slotDurationIsAlignedArgumentAndResults")
	void is_aligned_returns_expected_results(int slotDuration, boolean expectedResult, LocalTime localTime) {
		// Arrange
		SlotDuration duration = new SlotDuration(slotDuration);
		
		// Act
		boolean actualResult = duration.isAligned(localTime);
		
		// Assert
		assertEquals(actualResult, expectedResult);	
	}
	
	private static Stream<Arguments> slotDurationIsAlignedArgumentAndResults() {
		return Stream.of(
				// Slot duration: 30 minutes. Returns true if the time is on the hour (e.g., 11:00)
				Arguments.of(30, true, LocalTime.of(11, 0)),
				// Slot duration: 30 minutes. Returns true if the time is on the half hour (e.g., 11:30)
				Arguments.of(30, true, LocalTime.of(11, 30)),
				// Slot duration: 30 minutes. Returns false if the time is neither of the above (e.g., 11:15)
				Arguments.of(30, false, LocalTime.of(11, 15)),
				// Slot duration: 60 minutes. Returns true if the time is on the hour (e.g., 11:00)
				Arguments.of(60, true, LocalTime.of(11, 0)),
				// Slot duration: 60 minutes. Returns false if the time is on the half hour (e.g., 11:30)
				Arguments.of(60, false, LocalTime.of(11, 30)),
				// Slot duration: 60 minutes. Returns false if the time is neither of the above (e.g., 11:15)
				Arguments.of(60, false, LocalTime.of(11, 15))
				);
	}
}