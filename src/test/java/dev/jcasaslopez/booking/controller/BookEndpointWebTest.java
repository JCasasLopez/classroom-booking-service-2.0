package dev.jcasaslopez.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionTimedOutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.exception.ClassroomNotFoundException;
import dev.jcasaslopez.booking.exception.GlobalExceptionHandler;
import dev.jcasaslopez.booking.exception.InvalidBookingException;
import dev.jcasaslopez.booking.filter.BookingAuthenticationFilter;
import dev.jcasaslopez.booking.service.BookingService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.context.UserContext;

@WebMvcTest(
	    controllers = {BookingController.class, GlobalExceptionHandler.class},
	    
	    // Exclude the authentication filter from this slice, as configuration complexity skyrockets if included,
	    // and it is unrelated to DTO validation anyway; it is covered in a separate filter test.
	    excludeFilters = @ComponentScan.Filter(
	        type = FilterType.ASSIGNABLE_TYPE,
	        classes = BookingAuthenticationFilter.class
	    )
	)
public class BookEndpointWebTest {
	
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper mapper;
	@MockitoBean BookingService service;
	
	private static final int ID_CLASSROOM = 1;
	private static final int ID_USER = 1;
	private static final String EMAIL = "test@gmail";
	private static final LocalDateTime START_BOOKING = LocalDateTime.of(2026, 10, 13, 10, 00);
	private static final LocalDateTime FINISH_BOOKING = LocalDateTime.of(2026, 10, 13, 11, 00);

	
	@BeforeAll
	static void setUpUserContext() {
		UserContext.setContext(EMAIL, ID_USER);
	}
	
	@ParameterizedTest
	@MethodSource("testdata")
	void booking_endpoint_returns_400_when_booking_request_validations_fail(BookingRequestDto bookingRequest) throws JsonProcessingException, Exception {
		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(bookingRequest)))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));

		verifyNoInteractions(service);
	}
	
	private static Stream<Arguments> testdata(){
		return Stream.of(
				Arguments.of((Object) null),
				Arguments.of(new BookingRequestDto(null, START_BOOKING, FINISH_BOOKING)),
				Arguments.of(new BookingRequestDto(ID_CLASSROOM, null, FINISH_BOOKING)),
				Arguments.of(new BookingRequestDto(ID_CLASSROOM, START_BOOKING, null))
						);
	}
	
	@Test
	void booking_endpoint_returns_400_when_booking_request_is_malformed() throws JsonProcessingException, Exception {
		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content("Malformed booking request"))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));

		verifyNoInteractions(service);
	}
	
	@Test
	void booking_endpoint_returns_404_when_classroom_is_not_found() throws JsonProcessingException, Exception {
		// Arrange	
		BookingRequestDto bookingRequest = new BookingRequestDto(ID_CLASSROOM, START_BOOKING, FINISH_BOOKING);
		String exceptionMessage = "Classroom not found"; 

		doThrow(new ClassroomNotFoundException(exceptionMessage)).when(service).book(bookingRequest, ID_CLASSROOM, EMAIL);

		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(bookingRequest)))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isNotFound())
		.andExpect(jsonPath("$.status").value("NOT_FOUND"));
	}
	
	@Test
	void booking_endpoint_returns_400_when_booking_is_not_valid() throws JsonProcessingException, Exception {
		// Arrange
		BookingRequestDto bookingRequest = new BookingRequestDto(ID_CLASSROOM, START_BOOKING, FINISH_BOOKING);
		String exceptionMessage = "Invalid booking"; 

		doThrow(new InvalidBookingException(exceptionMessage)).when(service).book(any(BookingRequestDto.class), anyInt(), anyString());

		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(bookingRequest)))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));
	}
	
	@Test
	void booking_endpoint_returns_503_when_transaction_times_out() throws Exception {
		// Arrange	
		BookingRequestDto bookingRequest = new BookingRequestDto(ID_CLASSROOM, START_BOOKING, FINISH_BOOKING);
		String internalDetail = "Transaction timed out: deadline was Thu Nov 12 15:46:00"; 

		doThrow(new TransactionTimedOutException(internalDetail)).when(service).book(bookingRequest, ID_USER, EMAIL);

		// Act + Assert
		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(bookingRequest)))
		.andExpect(status().isServiceUnavailable())
		.andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"))
		.andExpect(jsonPath("$.message").value("The booking could not be completed right now. Please try again in a moment."));
	}

	@Test
	void booking_endpoint_returns_503_when_classroom_lock_cannot_be_acquired() throws Exception {
		// Arrange	
		BookingRequestDto bookingRequest = new BookingRequestDto(ID_CLASSROOM, START_BOOKING, FINISH_BOOKING);
		String internalDetail = "Pessimistic lock failed on table classroom_locks for id " + ID_CLASSROOM; 

		doThrow(new PessimisticLockingFailureException(internalDetail)).when(service).book(bookingRequest, ID_USER, EMAIL);

		// Act + Assert
		mockMvc.perform(post(BookingEndpoints.BOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(bookingRequest)))
		.andExpect(status().isServiceUnavailable())
		.andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"))
		.andExpect(jsonPath("$.message").value("The booking could not be completed right now. Please try again in a moment."));
	}
}
