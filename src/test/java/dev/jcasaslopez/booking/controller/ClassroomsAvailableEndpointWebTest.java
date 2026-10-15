package dev.jcasaslopez.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
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
public class ClassroomsAvailableEndpointWebTest {
	
	@Autowired MockMvc mockMvc;
	@MockitoBean SearchService service;
	
	private static final int ID_USER = 1;
	private static final String EMAIL = "test@gmail";
	
	private static final LocalDateTime START = LocalDateTime.of(2026, 10, 13, 11, 0);
	private static final LocalDateTime FINISH = LocalDateTime.of(2026, 10, 13, 12, 0);
	private static final int SEATS = 20;
	private static final boolean PROJECTOR = true;
	private static final boolean SPEAKERS = true;
	
	@BeforeAll
	static void setUpUserContext() {
		UserContext.setContext(EMAIL, ID_USER);
	}
	
	@ParameterizedTest
	@MethodSource("testData")
	void classrooms_available_endpoint_returns_400_when_parameters_are_invalid(LocalDateTime start, LocalDateTime finish,
			Integer seats, Boolean projector, Boolean speakers) throws Exception {
		
		var requestBuilder = get(BookingEndpoints.CLASSROOMS_AVAILABLE);

		if (start != null) requestBuilder.param("start", start.toString());
		if (finish != null) requestBuilder.param("finish", finish.toString());
		if (seats != null) requestBuilder.param("seats", String.valueOf(seats));
		if (projector != null) requestBuilder.param("projector", String.valueOf(projector));
		if (speakers != null) requestBuilder.param("speakers", String.valueOf(speakers));

		mockMvc.perform(requestBuilder)
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));

	}
	
	private static Stream<Arguments> testData(){
		return Stream.of(
				Arguments.of(null, LocalDateTime.of(2026, 10, 15, 11, 0), 10, true, true),
				Arguments.of(LocalDateTime.of(2026, 10, 15, 11, 0), null, 10, true, true),
				Arguments.of(LocalDateTime.of(2026, 10, 15, 11, 0), LocalDateTime.of(2026, 10, 15, 12, 0), -5, true, true),
				Arguments.of(LocalDateTime.of(2026, 10, 15, 11, 0), LocalDateTime.of(2026, 10, 15, 12, 0), 10, null, true),
				Arguments.of(LocalDateTime.of(2026, 10, 15, 11, 0), LocalDateTime.of(2026, 10, 15, 12, 0), 10, true, null)
				);
				
	}
	
	@Test
	void classrooms_available_endpoint_returns_400_when_slot_is_out_of_opening_hours() throws Exception {		
		String exceptionMessage = "Slot out of opening hours";

		doThrow(new SlotOutOfOpeningHoursException(exceptionMessage)).when(service)
				.classroomsAvailableByPeriodAndFeatures(any(), any(), anyInt(), anyBoolean(), anyBoolean());

		mockMvc.perform(get(BookingEndpoints.CLASSROOMS_AVAILABLE)
				.param("start", START.toString())
				.param("finish", FINISH.toString())
				.param("seats", String.valueOf(SEATS))
				.param("projector", String.valueOf(PROJECTOR))
				.param("speakers", String.valueOf(SPEAKERS)))

		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));
	}
	
	@Test
	void classrooms_available_endpoint_returns_400_when_parameters_are_invalid() throws Exception {		
		String exceptionMessage = "Illegal Argument Exception";

		doThrow(new IllegalArgumentException(exceptionMessage)).when(service)
				.classroomsAvailableByPeriodAndFeatures(any(), any(), anyInt(), anyBoolean(), anyBoolean());

		mockMvc.perform(get(BookingEndpoints.CLASSROOMS_AVAILABLE)
				.param("start", START.toString())
				.param("finish", FINISH.toString())
				.param("seats", String.valueOf(SEATS))
				.param("projector", String.valueOf(PROJECTOR))
				.param("speakers", String.valueOf(SPEAKERS)))

		.andExpect(jsonPath("$.message").value(exceptionMessage))
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("$.status").value("BAD_REQUEST"));
	}

}
