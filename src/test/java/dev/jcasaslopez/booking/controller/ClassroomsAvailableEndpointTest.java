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
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ClassroomsAvailableEndpointTest extends BaseIntegrationTest {
	
	@Test
	void classrooms_available_endpoint_returns_the_expected_response() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		int seats = 20;
		boolean projector = false;
		boolean speakers = true;

		String classroomsAvailableUrl = UriComponentsBuilder.fromPath(Endpoints.CLASSROOMS_AVAILABILITY)
				.queryParam("start", TestHelper.generateStartSearch())
				.queryParam("finish", TestHelper.generateFinishSearch(300))
				.queryParam("seats", seats)
				.queryParam("projector", projector)
				.queryParam("speakers", speakers)
				.toUriString();
		
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		
		// Act
		ResponseEntity<StandardResponse<List<ClassroomEvent>>> httpResponse = testRestTemplate.exchange(
				classroomsAvailableUrl, 
				HttpMethod.GET,
				httpRequest, 
				new ParameterizedTypeReference<StandardResponse<List<ClassroomEvent>>>() {}
			);
		
		// Assert
		StandardResponse<List<ClassroomEvent>> response = httpResponse.getBody();
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.status()),
				() -> assertNotNull(response.details()),
				() -> assertTrue(response.message()
		.contains(String.format("(seats: %s - projector: %s - speakers: %s) retrieved successfully", seats, projector, speakers)))
				);	
	}
}