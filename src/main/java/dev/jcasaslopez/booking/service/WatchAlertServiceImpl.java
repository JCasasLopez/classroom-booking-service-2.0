package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.event.EventPublisher;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.mapper.WatchAlertMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@Service
public class WatchAlertServiceImpl implements WatchAlertService {
	
	private static final Logger logger = LoggerFactory.getLogger(WatchAlertServiceImpl.class);
	
	private WatchAlertMapper watchAlertMapper; 
	private WatchAlertRepository watchAlertRepository;
	private BookingRepository bookingRepository;
	private ClassroomValidator classroomValidator;
	private EventPublisher eventPublisher;
	private List<ClassroomEvent> classroomStore;
	
	public WatchAlertServiceImpl(WatchAlertMapper watchAlertMapper, WatchAlertRepository watchAlertRepository,
			BookingRepository bookingRepository, ClassroomValidator classroomValidator, EventPublisher eventPublisher) {
		this.watchAlertMapper = watchAlertMapper;
		this.watchAlertRepository = watchAlertRepository;
		this.bookingRepository = bookingRepository;
		this.classroomValidator = classroomValidator;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public WatchAlert addWatchAlert(WatchAlertRequestDto watchAlertDto) {
		WatchAlert watchAlert = watchAlertMapper.toEntity(watchAlertDto);
		
		long idBooking = watchAlert.getIdBooking();
		Booking booking = bookingRepository.findById(idBooking)
									.orElseThrow(() -> new NoSuchBookingException("Booking {} was not found in the database: " + idBooking));
		classroomValidator.validateClassroomExists(booking.getIdClassroom());
				
		WatchAlert savedWatchAlert = watchAlertRepository.save(watchAlert);
		eventPublisher.watchAlertEventPublisher(savedWatchAlert, UserContext.getEmail());
	
		logger.info("Watch alert created: Classroom ID= {}, User ID= {}, Start= {}, Finish= {}", 
				booking.getIdClassroom(), booking.getIdUser(), booking.getStart(), booking.getFinish());
		
		return savedWatchAlert;
	}

	@Override
	public List<WatchAlertResponseDto> watchAlertsListByUserAndTimePeriod(LocalDateTime startSearch, LocalDateTime finishSearch) {
		return watchAlertRepository.findWatchAlertsByUserAndTimePeriod(UserContext.getEmail(), startSearch, finishSearch)
						.stream()
						.map(watchAlert -> watchAlertMapper.toResponseDto(watchAlert, classroomStore, bookingRepository))
						.toList();
	}
}