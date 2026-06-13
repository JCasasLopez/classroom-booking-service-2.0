package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

// Controller parameter validation and the following exceptions are already covered
// by existing endpoint tests and are not repeated here:
// - IllegalArgumentException          → 400
// - SlotOutOfOpeningHoursException    → 400
// - NoSuchClassroomException          → 404

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AvailabilityCalendarEndpointTest extends BaseIntegrationTest {
	
	@Test
	void availability_calendar_endpoint_returns_the_expected_response() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		
		String availabilityCalendarUrl = UriComponentsBuilder.fromPath(Endpoints.AVAILABILITY_CALENDAR)
				.queryParam("start", TestHelper.generateStartSearch())
				.queryParam("finish", TestHelper.generateFinishSearch(300))
				.queryParam("idClassroom", 1)
				.toUriString();
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		
		// Act
		ResponseEntity<StandardResponse<List<SlotStatusDto>>> httpResponse = testRestTemplate.exchange(availabilityCalendarUrl, HttpMethod.GET,
								httpRequest, new ParameterizedTypeReference<StandardResponse<List<SlotStatusDto>>>() {});
		
		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.OK, httpResponse.getBody().status()),
				() -> assertNotNull(httpResponse.getBody().details()),
				() -> assertTrue(httpResponse.getBody().message()
						.equals(String.format("Availability calendar for classroom %s retrieved successfully", 1)))
				);
	}

}