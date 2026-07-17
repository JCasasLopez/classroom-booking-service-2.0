package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;
import dev.jcasaslopez.booking.mapper.TimeSlotMapper;

@Component
public class SlotAvailabilityMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(SlotAvailabilityMapper.class);
	
	private final int slotDuration;
	private final WeeklySchedule weeklySchedule;
	private final TimeSlotMapper mapper;
	
	public SlotAvailabilityMapper(@Value("${time-slot.duration}") int slotDuration, WeeklySchedule weeklySchedule,
			TimeSlotMapper mapper) {
		this.slotDuration = slotDuration;
		this.weeklySchedule = weeklySchedule;
		this.mapper = mapper;
	}

	// SlotStatusDto represents a TimeSlotDto with the additional information that the classroom is booked or not, and if it is,
	// contains the corresponding idBooking. The reason for this is that when representing the classroom grid
	// the front-end can use straight this idBooking to cancel a booking or create a watch alert.
	// SlotStatusDto wraps a flat TimeSlotDto along with the availability status of a classroom.
	// The domain 'TimeSlot' is mapped to 'TimeSlotDto' here to expose a clean, agnostics contract.
	public List<SlotStatusDto> buildAvailabilityGrid(List<Booking> bookings, LocalDateTime start, LocalDateTime finish) {
		List<TimeSlot> slots = generateTimeSlotsForPeriod(start, finish);
		
		return slots.stream()
				.map(slot -> findBookingForSlot(slot, bookings)
						.map(booking -> new SlotStatusDto(mapper.toDto(slot), false, booking.getIdBooking()))
						.orElse(new SlotStatusDto(mapper.toDto(slot), true, null)))
				.toList();
	}

	public Optional<Booking> findBookingForSlot(TimeSlot slot, List<Booking> bookings) {
		return bookings.stream()
				.filter(booking -> slot.getStart().equals(booking.getStart()) || slot.getFinish().equals(booking.getFinish())
						|| (slot.getStart().isAfter(booking.getStart()) && slot.getFinish().isBefore(booking.getFinish())))
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