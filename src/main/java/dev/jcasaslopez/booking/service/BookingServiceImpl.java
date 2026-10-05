package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jcasaslopez.booking.domain.BookingPeriod;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.exception.InvalidBookingStatusException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.mapper.BookingMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.booking.validator.BookingValidator;
import dev.jcasaslopez.classroom.shared.context.UserContext;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Service
public class BookingServiceImpl implements BookingService {
	
	private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
	
	private final BookingRepository bookingRepository;
	private final WatchAlertRepository watchAlertRepository;
	private final EventPublisher eventPublisher;
	private final BookingMapper mapper;
	private final List<ClassroomEvent> classroomsStore;
	private final BookingValidator bookingValidator;
	
	public BookingServiceImpl(BookingRepository bookingRepository, WatchAlertRepository watchAlertRepository,
			EventPublisher eventPublisher, BookingMapper mapper,
			List<ClassroomEvent> classroomsStore, BookingValidator bookingValidator) {
		this.bookingRepository = bookingRepository;
		this.watchAlertRepository = watchAlertRepository;
		this.eventPublisher = eventPublisher;
		this.mapper = mapper;
		this.classroomsStore = classroomsStore;
		this.bookingValidator = bookingValidator;
	}

	@Override
	public BookingResponseDto book(BookingRequestDto booking, int idUser) {
		// It returns a list with the booking start and finish
		BookingPeriod bookingPeriod = bookingValidator.validateBooking(booking, idUser);
		
		Booking savedBooking = bookingRepository.save(new Booking(
				0, 
				idUser, 
				booking.idClassroom(), 
				bookingPeriod.start(),
				bookingPeriod.finish(),
				LocalDateTime.now(),
				BookingStatus.ACTIVE)
				);
		
		eventPublisher.publishBookingRelatedEvent(NotificationType.BOOKING_CONFIRMED, savedBooking, UserContext.getEmail());
		logger.info("Booking created: ID= {}, User ID= {}, Classroom ID= {}, Start= {}, Finish= {}", 
		        savedBooking.getIdBooking(), savedBooking.getIdUser(), savedBooking.getIdClassroom(), 
		        savedBooking.getStart(), savedBooking.getFinish());
		return mapper.toResponseDto(savedBooking, classroomsStore);
	}

	@Override
	@Transactional
	public void cancel(Long idBooking, int idUser) {				
	    // Both "booking not found" and "booking belongs to another user" are deliberately
	    // mapped to the same exception. Distinguishing between them would let an attacker
	    // enumerate valid booking IDs simply by observing which error is returned.
		Booking booking = bookingRepository.findById(idBooking)
	            .filter(b -> b.getIdUser() == idUser)
	            .orElseThrow(() -> new BookingNotFoundExceptions("Booking was not found in the database"));
		
		if(booking.getStatus() != BookingStatus.ACTIVE) {
			throw new InvalidBookingStatusException("Only ACTIVE bookings can be cancelled");
		}
		
		bookingRepository.modifyBookingStatus(idBooking, BookingStatus.CANCELLED);
		eventPublisher.publishBookingRelatedEvent(NotificationType.BOOKING_CANCELLED, booking, UserContext.getEmail());
		triggerWatchAlerts(booking);
		
		logger.info("Booking cancelled: ID= {}, User ID= {}, Classroom ID= {}", 
	            booking.getIdBooking(), idUser, booking.getIdClassroom());
	}

	@Override
	public List<BookingResponseDto> bookingsByUser(int idUser) {
		logger.debug("Searching booking history for user {}", idUser);
				
		return bookingRepository.findBookingsByUser(idUser).stream()
				.map(booking -> mapper.toResponseDto(booking, classroomsStore))
				.toList();
			
	}

	@Transactional
	@Override
	// Past bookings are set to COMPLETE automatically every hour.
	@Scheduled(fixedRate = 3_600_000)
	public void markBookingsAsCompleted() {
		LocalDateTime now = LocalDateTime.now();
	    bookingRepository.markCompletedBookings(now); 
	    logger.info("Past bookings marked as COMPLETE up to: {}", now);
	}
	
	// This is the core enforcement of our pragmatic denormalization strategy (see WatchAlert entity comment over email field).
	// Since this thread runs under the context  of the user performing the cancellation, we cannot resolve 
	// the targets' email via ThreadLocal. By reading the email addresses directly from the local DB, 
    // we publish to Kafka with zero latency and microservices are completely independent from each other.
	private void triggerWatchAlerts(Booking cancelledBooking) {
		List<WatchAlert> watchAlertsForBooking = watchAlertRepository.findWatchAlertsByBooking(cancelledBooking.getIdBooking());
		watchAlertsForBooking.forEach(watchAlert -> eventPublisher.publishBookingRelatedEvent 
						(NotificationType.WATCH_ALERT_TRIGGERED, cancelledBooking, watchAlert.getUserEmail()));
	}

}