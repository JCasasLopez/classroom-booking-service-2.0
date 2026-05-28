package dev.jcasaslopez.booking.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.OpeningHours;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Service
public class SearchServiceImpl implements SearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
	
	private final BookingRepository bookingRepository;
	private final ClassroomValidator classroomValidator;
	private final SlotAvailabilityMapper slotAvailabilityMapper;
	private final List<ClassroomEvent> classroomsStore;
	private final WeeklySchedule weeklySchedule;
	
	public SearchServiceImpl(BookingRepository bookingRepository, ClassroomValidator classroomValidator,
			SlotAvailabilityMapper slotAvailabilityMapper, List<ClassroomEvent> classroomsStore,
			WeeklySchedule weeklySchedule) {
		this.bookingRepository = bookingRepository;
		this.classroomValidator = classroomValidator;
		this.slotAvailabilityMapper = slotAvailabilityMapper;
		this.classroomsStore = classroomsStore;
		this.weeklySchedule = weeklySchedule;
	}

	@Override
	public List<SlotStatusDto> availabilityCalendarByClassroom(int idClassroom, LocalDateTime start, LocalDateTime finish) {
	    logger.info("Fetching availability calendar for classroom {} from {} to {}", idClassroom, start, finish);
	    validateStartAndFinish(start, finish);
		classroomValidator.validateClassroomExists(idClassroom);
		List<Booking> bookingsForPeriod = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish);
		return slotAvailabilityMapper.buildAvailabilityGrid(bookingsForPeriod, start, finish);
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriod(LocalDateTime start, LocalDateTime finish) {
	    logger.info("Fetching available classrooms from {} to {}", start, finish);
	    validateStartAndFinish(start, finish);
	    List<Integer> occupiedClassrooms = bookingRepository.findOccupiedClassroomsbyPeriod(start, finish);
		return classroomsStore.stream()
	            .filter(classroom -> !occupiedClassrooms.contains(classroom.getIdClassroom()))
	            .toList();
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriodAndFeatures(LocalDateTime start, LocalDateTime finish,
			int seats, boolean projector, boolean speakers) {
		 logger.info("Fetching available classrooms from {} to {} with features: seats={}, projector={}, speakers={}", 
		            start, finish, seats, projector, speakers);
		return classroomsAvailableByPeriod(start, finish).stream()
				.filter(c -> c.getSeats() >= seats)
				.filter(c -> projector ? c.getProjector() : true)
				.filter(c -> speakers ? c.getSpeakers() : true)
				.toList();
	}
	
	// we need a specific validation method here, as TimeSlot validates slot alignment, which is too strict here 
	// — a search period like 11:15–14:00 is valid even if it doesn't align with slot boundaries.
	// Also, start and finish must be on the same day to avoid closed days and out of opening hours slots in between.	
	private void validateStartAndFinish(LocalDateTime start, LocalDateTime finish) {
		if(start.isBefore(LocalDateTime.now()) || finish.isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}
		
		DayOfWeek searchStartDayOfWeek = start.getDayOfWeek();
		DayOfWeek searchFinishDayOfWeek = finish.getDayOfWeek();
		
		if(searchStartDayOfWeek != searchFinishDayOfWeek) {
			throw new IllegalArgumentException("Start and finish have to be in the same day");
		}

		OpeningHours startOpeningHours = weeklySchedule.getWeeklySchedule().get(searchStartDayOfWeek);
		OpeningHours finishOpeningHours = weeklySchedule.getWeeklySchedule().get(searchFinishDayOfWeek);
		
		if (startOpeningHours.openingTime() == null) {			
			throw new SlotOutOfOpeningHoursException("The center is closed that day");
		}
		
		if(start.toLocalTime().isBefore(startOpeningHours.openingTime()) || 
				finish.toLocalTime().isAfter(finishOpeningHours.closingTime())) {
			throw new SlotOutOfOpeningHoursException("Start or finish out of opening hours");
		}
		
	}
}