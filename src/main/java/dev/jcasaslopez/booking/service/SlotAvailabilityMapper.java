package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

@Component
public class SlotAvailabilityMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(SlotAvailabilityMapper.class);
	
	private int slotDuration;
	private WeeklySchedule weeklySchedule;
	
	public SlotAvailabilityMapper(@Value("${time-slot.duration}") int slotDuration, WeeklySchedule weeklySchedule) {
		this.slotDuration = slotDuration;
		this.weeklySchedule = weeklySchedule;
	}

	// SlotStatusDto represents a TimeSlot with the additional information that the classroom is booked or not, and if it is,
	// contains the corresponding idBooking. The reason for this is that when representing the classroom grid
	// the front-end can use straight this idBooking to cancel a booking or create a watch alert.
	public List<SlotStatusDto> buildAvailabilityGrid(List<Booking> bookings, LocalDateTime start, LocalDateTime finish) {
		List<TimeSlot> slots = generateTimeSlotsForPeriod(start, finish);
		return slots.stream()
				.map(slot -> findBookingForSlot(slot, bookings)
						.map(booking -> new SlotStatusDto(slot, false, booking.getIdBooking()))
						.orElse(new SlotStatusDto(slot, true, null)))
				.toList();
	}

	public Optional<Booking> findBookingForSlot(TimeSlot slot, List<Booking> bookings) {
		return bookings.stream()
				.filter(booking -> slot.getStart().equals(booking.getStart()))
				.findAny();
	}

	// Returns the s list of TimeSlot objects that represent time slots where the classroom is open, but offer
	// no information as to whether the classroom is available or not.
	public List<TimeSlot> generateTimeSlotsForPeriod(LocalDateTime periodStart, LocalDateTime periodEnd){
		List<TimeSlot> grid = new ArrayList<>();
		LocalDateTime cursor = periodStart;

		while (cursor.isBefore(periodEnd)) {
			try {
				grid.add(new TimeSlot(cursor, weeklySchedule, slotDuration));
			} catch (SlotOutOfOpeningHoursException e) {
				// Out-of-opening-hours slot is skipped
			    logger.debug("Skipping slot at {}: out of opening hours", cursor);
			}
			cursor = cursor.plusMinutes(slotDuration);
		}
		return grid;
	}
}