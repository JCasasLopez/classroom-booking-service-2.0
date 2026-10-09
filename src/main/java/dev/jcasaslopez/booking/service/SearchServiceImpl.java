package dev.jcasaslopez.booking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.exception.DataIntegrityException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.validator.ClassroomValidator;
import dev.jcasaslopez.booking.validator.SearchValidator;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Service
public class SearchServiceImpl implements SearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
	
	private final BookingRepository bookingRepository;
	private final SlotAvailabilityMapper slotAvailabilityMapper;
	private final List<ClassroomEvent> classroomsStore;
	private final SlotDuration slotDuration;
	private final ClassroomValidator classroomValidator;
	private final SearchValidator searchValidator;
	
	public SearchServiceImpl(BookingRepository bookingRepository, SlotAvailabilityMapper slotAvailabilityMapper,
			List<ClassroomEvent> classroomsStore, SlotDuration slotDuration, ClassroomValidator classroomValidator,
			SearchValidator searchValidator) {
		this.bookingRepository = bookingRepository;
		this.slotAvailabilityMapper = slotAvailabilityMapper;
		this.classroomsStore = classroomsStore;
		this.slotDuration = slotDuration;
		this.classroomValidator = classroomValidator;
		this.searchValidator = searchValidator;
	}

	@Override
	public List<SlotStatusDto> availabilityCalendarByClassroom(int idClassroom, LocalDateTime start, LocalDateTime finish) {
		logger.info("Fetching availability calendar for classroom {} from {} to {}", idClassroom, start, finish);
		
		searchValidator.validateSearch(start, finish);
		classroomValidator.validateClassroomExists(idClassroom);
		
		List<Booking> bookingsForPeriod = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish);
		
		return slotAvailabilityMapper.buildAvailabilityGrid(bookingsForPeriod, start, finish);
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriod(LocalDateTime start, LocalDateTime finish) {
		logger.info("Fetching available classrooms from {} to {}", start, finish);
	   
		searchValidator.validateSearch(start, finish);
	    
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
		searchValidator.validateSearch(start, finish);
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