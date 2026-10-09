package dev.jcasaslopez.booking.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.exception.DataIntegrityException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.validator.ClassroomValidator;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Service
public class SearchServiceImpl implements SearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
	
	private final BookingRepository bookingRepository;
	private final ClassroomValidator classroomValidator;
	private final SlotAvailabilityMapper slotAvailabilityMapper;
	private final List<ClassroomEvent> classroomsStore;
	private final WeeklySchedule weeklySchedule;
	private final SlotDuration slotDuration;
	
	public SearchServiceImpl(BookingRepository bookingRepository, ClassroomValidator classroomValidator,
			SlotAvailabilityMapper slotAvailabilityMapper, List<ClassroomEvent> classroomsStore,
			WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
		this.bookingRepository = bookingRepository;
		this.classroomValidator = classroomValidator;
		this.slotAvailabilityMapper = slotAvailabilityMapper;
		this.classroomsStore = classroomsStore;
		this.weeklySchedule = weeklySchedule;
		this.slotDuration =  slotDuration;
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
	            .filter(classroom -> !occupiedClassrooms.contains(classroom.idClassroom()))
	            .toList();
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriodAndFeatures(LocalDateTime start, LocalDateTime finish,
			int seats, boolean projector, boolean speakers) {
		 logger.info("Fetching available classrooms from {} to {} with features: seats={}, projector={}, speakers={}", 
		            start, finish, seats, projector, speakers);
		 
		return classroomsAvailableByPeriod(start, finish).stream()
				.filter(c -> c.seats() >= seats)
				.filter(c -> projector ? c.projector() : true)
				.filter(c -> speakers ? c.speakers() : true)
				.toList();
	}
	
	@Override
	public Long findBookingByClassroomAndTimePeriod(int idClassroom, LocalDateTime start, LocalDateTime finish) {
	    validateStartAndFinish(start, finish);
	    validateIsSingleTimeSlot(start, finish);
	    
		List<Booking> bookings = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish);
		
		if(bookings.isEmpty()) {
	        logger.error("No active booking found for classroom {} between {} and {}", idClassroom, start, finish);
			throw new BookingNotFoundExceptions("No active booking found for classroom " + idClassroom + " between " + start + " and " + finish);
		} else if(bookings.size() > 1) {
		    logger.error("Data integrity violation: more than 1 booking for the time period: {}", bookings.toString());
			throw new DataIntegrityException("An internal data consistency error has occurred");
		}
		
		return bookings.get(0).getIdBooking();
	}
	
	// we need a specific validation method here, as TimeSlot validates slot alignment, which is too strict here 
	// — a search period like 11:15–14:00 is valid even if it doesn't align with slot boundaries.
	// Also, start and finish must be on the same day to avoid closed days and out of opening hours slots in between.	
	private void validateStartAndFinish(LocalDateTime start, LocalDateTime finish) {
		if (!finish.isAfter(start)) {
		    throw new IllegalArgumentException("Finish must be after start");
		}
		
		LocalDateTime now = LocalDateTime.now();
		if(start.isBefore(now) || finish.isBefore(now)) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}
			
		if (!start.toLocalDate().equals(finish.toLocalDate())) {
		    throw new IllegalArgumentException("Start and finish have to be in the same day");
		}

		DayOfWeek searchDayOfWeek = start.getDayOfWeek();
		DaySchedule daySchedule = weeklySchedule.scheduleFor(searchDayOfWeek);
		
		if (!(daySchedule instanceof DaySchedule.Open open)) {
		    throw new SlotOutOfOpeningHoursException("The center is closed that day");
		}

		if (start.toLocalTime().isBefore(open.openingTime()) ||
		    finish.toLocalTime().isAfter(open.closingTime())) {
		    throw new SlotOutOfOpeningHoursException("Start or finish out of opening hours");
		}		
		
	}
	
	private void validateIsSingleTimeSlot(LocalDateTime start, LocalDateTime finish) {
		Duration slotDurationInMinutes = Duration.ofMinutes(slotDuration.minutes());
	    Duration requested = Duration.between(start, finish);
	    
	    if (!requested.equals(slotDurationInMinutes)) {
	    	 logger.error("Invalid search period: expected duration {} but got {} (start={}, finish={})",
	    			 slotDurationInMinutes, requested, start, finish);
	        throw new IllegalArgumentException("The search period must match exactly the minimum time slot duration");
	    }
	}
}