package dev.jcasaslopez.booking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.validator.ClassroomValidator;
import dev.jcasaslopez.booking.validator.RangeValidator;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Service
public class SearchServiceImpl implements SearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
	
	private final BookingRepository bookingRepository;
	private final SlotAvailabilityMapper slotAvailabilityMapper;
	private final List<ClassroomEvent> classroomsStore;
	private final ClassroomValidator classroomValidator;
	private final SlotDuration slotDuration;
	private final WeeklySchedule weeklySchedule;
	
	public SearchServiceImpl(BookingRepository bookingRepository, SlotAvailabilityMapper slotAvailabilityMapper,
			List<ClassroomEvent> classroomsStore, ClassroomValidator classroomValidator, 
			SlotDuration slotDuration, WeeklySchedule weeklySchedule) {
		this.bookingRepository = bookingRepository;
		this.slotAvailabilityMapper = slotAvailabilityMapper;
		this.classroomsStore = classroomsStore;
		this.classroomValidator = classroomValidator;
		this.slotDuration = slotDuration;
		this.weeklySchedule = weeklySchedule;
	}

	@Override
	public List<SlotStatusDto> availabilityCalendarByClassroom(int idClassroom, LocalDate startDate, LocalDate endDate) {
		logger.info("Fetching availability calendar for classroom {} from {} to {}", idClassroom, startDate, endDate);
		
		RangeValidator.validateDateRange(startDate, endDate);

		LocalDateTime start = startDate.atStartOfDay();
		LocalDateTime finish = endDate.plusDays(1).atStartOfDay();
		
		classroomValidator.validateClassroomExists(idClassroom);
		
		List<Booking> bookingsForPeriod = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish);
		
		return slotAvailabilityMapper.buildAvailabilityGrid(bookingsForPeriod, start, finish);
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriod(LocalDateTime start, LocalDateTime finish) {
		logger.info("Fetching available classrooms from {} to {}", start, finish);
	   
		RangeValidator.validateTimeRange(start, finish, weeklySchedule, slotDuration);
	    
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

}