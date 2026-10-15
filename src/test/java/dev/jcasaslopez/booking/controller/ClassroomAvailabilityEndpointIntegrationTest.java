package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.SlotStatusDto;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;

public class ClassroomAvailabilityEndpointIntegrationTest extends BaseIntegrationTest {
	
	@Test
	void classroom_availability_endpoint_returns_the_expected_response() {
		// Arrange
		int idClassroom = 1;
		HttpHeaders headers = new HttpHeaders();
		
		String availabilityCalendarUrl = UriComponentsBuilder.fromPath(BookingEndpoints.CLASSROOM_AVAILABILITY)
                .queryParam("start", TestHelper.generateStartSearch())
                // 300 minutes -> 5 hours
                .queryParam("finish", TestHelper.generateFinishSearch(300))
                .buildAndExpand(idClassroom)
                .toUriString();
		
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		
		// Act
		 ResponseEntity<StandardResponse<List<SlotStatusDto>>> httpResponse = testRestTemplate.exchange(
	                availabilityCalendarUrl,
	                HttpMethod.GET,
	                httpRequest,
	                new ParameterizedTypeReference<StandardResponse<List<SlotStatusDto>>>() {});
				
		List<SlotStatusDto> slotStatus = httpResponse.getBody().details();
		
		// Assert	
		assertAll(
				() -> assertEquals(HttpStatus.OK, httpResponse.getBody().status()),
				() -> assertEquals(10, slotStatus.size()),
				() -> assertTrue(slotStatus.stream().allMatch(slot -> slot.available() == true)),
				() -> assertTrue(httpResponse.getBody().message()
						.equals(String.format("Availability calendar for classroom %s retrieved successfully", idClassroom)))
				);
			
	}

}