package dev.jcasaslopez.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.jcasaslopez.booking.exception.ClassroomNotFoundException;
import dev.jcasaslopez.booking.exception.GlobalExceptionHandler;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;
import dev.jcasaslopez.booking.filter.BookingAuthenticationFilter;
import dev.jcasaslopez.booking.service.SearchService;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.context.UserContext;

@WebMvcTest(
	    controllers = {SearchController.class, GlobalExceptionHandler.class},
	    
	    // Exclude the authentication filter from this slice, as configuration complexity skyrockets if included,
	    // and it is unrelated to DTO validation anyway; it is covered in a separate filter test.
	    excludeFilters = @ComponentScan.Filter(
	        type = FilterType.ASSIGNABLE_TYPE,
	        classes = BookingAuthenticationFilter.class
	    )
	)
public class ClassroomAvailabilityEndpointWebTest {
	
	@Autowired MockMvc mockMvc;
	@MockitoBean SearchService service;
	
	private static final int ID_USER = 1;
	private static final String EMAIL = "test@gmail";
	private static final int ID_CLASSROOM = 1;
	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 13, 11, 0);
	private static final LocalDateTime FINISH = LocalDateTime.of(2026, 10, 13, 12, 0);

	
	@BeforeAll
	static void setUpUserContext() {
		UserContext.setContext(EMAIL, ID_USER);
	}
	
	@ParameterizedTest
	@MethodSource("classroomAvailabilityRequest")
	void classroom_availability_endpoint_returns_400_when_request_is_bad(Integer idClassroom, LocalDateTime start, LocalDateTime finish) throws Exception {
	    
	    var requestBuilder = get(BookingEndpoints.CLASSROOM_AVAILABILITY, idClassroom);
	    
	    if (start != null) {
	        requestBuilder.param("start", start.toString());
	    }
	    if (finish != null) {
	        requestBuilder.param("finish", finish.toString());
	    }

	    mockMvc.perform(requestBuilder)
	    .andExpect(status().isBadRequest())
	    .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
	    
	    verifyNoInteractions(service);
	}

	private static Stream<Arguments> classroomAvailabilityRequest(){
	    return Stream.of(
	            // idClassroom non-positive
	            Arguments.of(-1, START, FINISH),	      
	            // start is null
	            Arguments.of(ID_CLASSROOM, null, FINISH),
	            // finish is null
	            Arguments.of(ID_CLASSROOM, START, null)
	          
	    );
	}
	
	@Test
	void classroom_availability_endpoint_returns_400_when_slot_is_out_of_opening_hours() throws Exception {		
		String exceptionMessage = "Slot out of opening hours";

		doThrow(new SlotOutOfOpeningHoursException(exceptionMessage)).when(service).availabilityCalendarByClassroom(anyInt(), any(), any());

		mockMvc.perform(get(BookingEndpoints.CLASSROOM_AVAILABILITY, ID_CLASSROOM)
				.param("start", START.toString())
				.param("finish", FINISH.toString()))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isBadRequest())
	    .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
	}

	@Test
	void classroom_availability_endpoint_returns_404_when_classroom_is_not_found() throws Exception {				
		String exceptionMessage = "Classroom not found";

		doThrow(new ClassroomNotFoundException(exceptionMessage)).when(service).availabilityCalendarByClassroom(anyInt(), any(), any());
	
		mockMvc.perform(get(BookingEndpoints.CLASSROOM_AVAILABILITY, ID_CLASSROOM)
				.param("start", START.toString())
				.param("finish", FINISH.toString()))
		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isNotFound())
	    .andExpect(jsonPath("$.status").value("NOT_FOUND"));
	}

}