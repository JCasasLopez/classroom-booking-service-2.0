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
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.mapper.WatchAlertMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@Service
public class WatchAlertServiceImpl implements WatchAlertService {
	
	private static final Logger logger = LoggerFactory.getLogger(WatchAlertServiceImpl.class);
	
	private final WatchAlertMapper mapper; 
	private final WatchAlertRepository watchAlertRepository;
	private final BookingRepository bookingRepository;
	private final ClassroomValidator classroomValidator;
	private final EventPublisher eventPublisher;
	private final List<ClassroomEvent> classroomsStore;
	
	public WatchAlertServiceImpl(WatchAlertMapper mapper, WatchAlertRepository watchAlertRepository,
			BookingRepository bookingRepository, ClassroomValidator classroomValidator, EventPublisher eventPublisher,
			List<ClassroomEvent> classroomsStore) {
		this.mapper = mapper;
		this.watchAlertRepository = watchAlertRepository;
		this.bookingRepository = bookingRepository;
		this.classroomValidator = classroomValidator;
		this.eventPublisher = eventPublisher;
		this.classroomsStore = classroomsStore;
	}

	@Override
	public WatchAlertResponseDto addWatchAlert(Long idBooking) {
		WatchAlert watchAlert = mapper.toEntity(new WatchAlertRequestDto(idBooking));
		
		Booking booking = bookingRepository.findById(idBooking)
							.orElseThrow(() -> new NoSuchBookingException(String.format("Booking %s was not found in the database", idBooking)));
		
		if(booking.getStatus() != BookingStatus.ACTIVE) {
			throw new IllegalStateException(String.format("Booking %s is not an active booking", idBooking));
		}
				
		classroomValidator.validateClassroomExists(booking.getIdClassroom());
				
		WatchAlert savedWatchAlert = watchAlertRepository.save(watchAlert);
		eventPublisher.publishBookingRelatedEvent(NotificationType.WATCH_ALERT_CONFIRMED, savedWatchAlert, UserContext.getEmail());
	
		logger.info("Watch alert created: Classroom ID= {}, User ID= {}, Start= {}, Finish= {}", 
				booking.getIdClassroom(), booking.getIdUser(), booking.getStart(), booking.getFinish());
		
		return mapper.toResponseDto(savedWatchAlert, classroomsStore, bookingRepository);
	}

	@Override
	public List<WatchAlertResponseDto> watchAlertsListByUserAndTimePeriod(LocalDateTime startSearch, LocalDateTime finishSearch) {
		if(!startSearch.isBefore(finishSearch)) throw new IllegalArgumentException("Start time has to preced finish time");
		
		return watchAlertRepository.findWatchAlertsByUserAndTimePeriod(UserContext.getEmail(), startSearch, finishSearch)
						.stream()
						.map(watchAlert -> mapper.toResponseDto(watchAlert, classroomsStore, bookingRepository))
						.toList();
	}
}