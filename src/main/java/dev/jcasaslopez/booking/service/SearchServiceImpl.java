package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class SearchServiceImpl implements SearchService {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
	
	private BookingRepository bookingRepository;
	private ClassroomValidator classroomValidator;
	private SlotAvailabilityMapper slotAvailabilityMapper;
	private List<ClassroomEvent> classroomsStore;
	
	@Override
	public List<SlotStatusDto> availabilityCalendarByClassroom(int idClassroom, LocalDateTime start, LocalDateTime finish) {
	    logger.info("Fetching availability calendar for classroom {} from {} to {}", idClassroom, start, finish);
		classroomValidator.validateClassroomExists(idClassroom);
		List<Booking> bookingsForPeriod = bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish);
		return slotAvailabilityMapper.buildAvailabilityGrid(bookingsForPeriod, start, finish);
	}

	// ClassroomEvent = Classroom 
	@Override
	public List<ClassroomEvent> classroomsAvailableByPeriod(LocalDateTime start, LocalDateTime finish) {
	    logger.info("Fetching available classrooms from {} to {}", start, finish);
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
}