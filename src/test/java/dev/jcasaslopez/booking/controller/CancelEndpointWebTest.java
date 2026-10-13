package dev.jcasaslopez.booking.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.exception.BookingNotFoundException;
import dev.jcasaslopez.booking.exception.GlobalExceptionHandler;
import dev.jcasaslopez.booking.exception.InvalidBookingStatusException;
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
public class CancelEndpointWebTest {
	
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper mapper;
	@MockitoBean BookingService service;
	
	private static final int ID_USER = 1;
	private static final String EMAIL = "test@gmail";
	private static final int ID_BOOKING = 1;
	
	@BeforeAll
	static void setUpUserContext() {
		UserContext.setContext(EMAIL, ID_USER);
	}
	
	@Test
	void cancel_endpoint_returns_400_if_idBooking_is_not_valid() throws Exception {
	    mockMvc.perform(patch(BookingEndpoints.CANCEL, -1))
	    .andExpect(status().isBadRequest())
	    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
	    .andExpect(jsonPath("$.message", containsString("must be greater than 0")));
	}
	
	@Test
	void cancel_endpoint_returns_404_if_booking_is_not_found() throws Exception {
		String exceptionMessage = "Booking was not found in the database"; 

		doThrow(new BookingNotFoundException(exceptionMessage)).when(service).cancel(anyLong(), anyInt(), anyString());
		
		mockMvc.perform(patch(BookingEndpoints.CANCEL, ID_BOOKING))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isNotFound())
		.andExpect(jsonPath("$.status").value("NOT_FOUND"));
	}
	
	@Test
	void cancel_endpoint_returns_409_when_booking_is_not_active() throws Exception {
		String exceptionMessage = "Only ACTIVE bookings can be cancelled"; 

		doThrow(new InvalidBookingStatusException(exceptionMessage)).when(service).cancel(anyLong(), anyInt(), anyString());
		
		mockMvc.perform(patch(BookingEndpoints.CANCEL, ID_BOOKING))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isConflict())
		.andExpect(jsonPath("$.status").value("CONFLICT"));
	}

}
