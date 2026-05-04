package dev.jcasaslopez.booking.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.event.EventPublisher;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@Service
public class BookingServiceImpl implements BookingService {
	
	private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
	private BookingRepository bookingRepository;
	private WeeklySchedule weeklySchedule;
	private ClassroomValidator classroomValidator;
	private EventPublisher eventPublisher;
	@Value("${time-slot.duration}") private int slotDuration; 
	@Value("${booking.maximum-duration}") private int bookingMaxDuration; 
	@Value("${booking.maximum-number-per-week}") private int maxNumberBookings; 
	
	public BookingServiceImpl(BookingRepository bookingRepository, WeeklySchedule weeklySchedule,
			ClassroomValidator classroomValidator, EventPublisher eventPublisher) {
		this.bookingRepository = bookingRepository;
		this.weeklySchedule = weeklySchedule;
		this.classroomValidator = classroomValidator;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Booking book(BookingRequestDto booking) {
		logger.debug("Booking request received for user {} in classroom {}", booking.idUser(), booking.idClassroom());
		classroomValidator.validateClassroomExists(booking.idClassroom());

		// It returns a list with the booking start and finish
		List<LocalDateTime> bookingTimes = checkBookingValidity(booking);
		
		Booking savedBooking = bookingRepository.save(new Booking(
				0, 
				booking.idUser(), 
				booking.idClassroom(), 
				bookingTimes.get(0), // start
				bookingTimes.get(1), // finish
				LocalDateTime.now(),
				BookingStatus.ACTIVE)
				);
		
		eventPublisher.bookEventPublisher(UserContext.getEmail());
		return savedBooking;
	}

	@Override
	public void cancel(Long idBooking, BookingStatus bookingStatus) {
		logger.debug("Cancel request received for booking {}", idBooking);
		bookingRepository.findById(idBooking)
					.orElseThrow(() -> new NoSuchBookingException("Booking {} was not found in the database: " + idBooking));
		bookingRepository.modifyBookingStatus(idBooking, BookingStatus.CANCELLED);
		eventPublisher.cancelBookingEventPublisher(UserContext.getEmail());
	}

	@Override
	public List<Booking> bookingsByUser(int idUser) {
		// No user existence check needed: this end-point requires JWT authentication, which is only issued to existing users. 
		logger.debug("Searching booking history for user {}", idUser);
		return bookingRepository.findBookingsByUser(idUser);
	}

	@Transactional
	@Override
	// Past bookings are set to COMPLETE automatically every hour.
	@Scheduled(fixedRate = 360_000)
	public void markBookingsAsCompleted() {
		LocalDateTime now = LocalDateTime.now();
	    logger.debug("Marking all past bookings as COMPLETE from: {}", now);
	    bookingRepository.markCompletedBookings(now); 
	}
	
	// *******************************************************************************************************
	// ****************************************** Auxiliary methods ******************************************
	// *******************************************************************************************************

	private List<LocalDateTime> checkBookingValidity(BookingRequestDto booking) {
		// Sorted copy of the booking's time slots
		List<LocalDateTime> listStartTimeSlots = booking.startTimeSlotList();
		Collections.sort(listStartTimeSlots);
		
		LocalDateTime bookingStart = listStartTimeSlots.get(0);
		LocalDateTime bookingFinish = listStartTimeSlots.get(listStartTimeSlots.size()-1).plusMinutes(slotDuration);
		
		checkTimeSlotsAreValid(listStartTimeSlots);
		checkSlotsAreConsecutive(listStartTimeSlots);
		checkBookDoesNotExceedMaxAllowedTime(listStartTimeSlots);
		checkclassroomIsAvailable(booking.idClassroom(), bookingStart, bookingFinish);
		checkUserHasBookingsLeft(booking.idUser(), bookingStart);
		
		return List.of(bookingStart, bookingFinish);
	}
	
	// There is no actual need to convert into TimeSlots, but by doing so, we validate them (see TimeSlot class)
	// That is reason the method does not return anything
	private void checkTimeSlotsAreValid (List<LocalDateTime> listSlots) { 
		listSlots.stream()
		.map(slot -> new TimeSlot(slot, weeklySchedule, slotDuration))
		// we do not return the list, since we do not need it (see comment above), but we do need a terminal operation for the stream
		.collect(Collectors.toList());
	}

	private void checkSlotsAreConsecutive(List<LocalDateTime> listSlots) {
		for(int i=0; i < listSlots.size() - 1; i++) {
			if(!listSlots.get(i).plusMinutes(slotDuration).equals(listSlots.get(i+1))) {
				logger.warn("Booking slots:{} are not consecutive", listSlots.toString());
				throw new InvalidBookingException("Booking slots are not consecutive");
			}
		}
	}
	
	private void checkBookDoesNotExceedMaxAllowedTime(List<LocalDateTime> listSlots) {
		int intendedBookingDuration = listSlots.size() * slotDuration;
		if(intendedBookingDuration >  bookingMaxDuration) {
			logger.warn("Maximum duration allowed is {} min. Trying to book {} min", bookingMaxDuration, intendedBookingDuration);
			throw new InvalidBookingException("Booking exceeds maximum duration allowed");
		}
	}
	
	private void checkclassroomIsAvailable (int idClassroom, LocalDateTime start, LocalDateTime finish) {
		if(bookingRepository.findActiveBookingsForClassroomByPeriod(idClassroom, start, finish).size() != 0) {
	    	logger.warn("Classroom {} is not available between {} and {}", idClassroom, start, finish);
			throw new InvalidBookingException("Classroom is not available for this time period");
		}
	}
	
	private void checkUserHasBookingsLeft(int idUser, LocalDateTime start) {		
	    LocalDateTime weekStart = start
	        .with(DayOfWeek.MONDAY)
	        .withHour(8).withMinute(0).withSecond(0).withNano(0);

	    LocalDateTime weekEnd = start
	        .with(DayOfWeek.SUNDAY)
	        .withHour(23).withMinute(59).withSecond(0).withNano(0);
	    
	    long bookingsForUserThatWeek = bookingRepository.findBookingsByUser(idUser).stream()
	    					.filter(booking ->  booking.getStatus() == BookingStatus.ACTIVE && 
	    										booking.getStart().isAfter(weekStart) 
	    										&& booking.getFinish().isBefore(weekEnd))
	    					.count();
	    
	    if(bookingsForUserThatWeek >= maxNumberBookings) {
	    	logger.warn("User has reached {}, the maximum number of weekly bookings", maxNumberBookings);
			throw new InvalidBookingException("User has reached the maximum number of weekly bookings");
	    }
	}
}