package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.context.UserContext;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;
import dev.jcasaslopez.classroom.shared.security.GenerateJwt;

public class WatchAlertEndpointsIntegrationTest extends BaseIntegrationTest {
	
	@Autowired WatchAlertRepository watchAlertRepository;

	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	
	@Test
	void add_watch_alert_endpoint_returns_the_expected_response_and_persists_watch_alert() {
		// Arrange	
		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = TestHelper.createBooking
													(testRestTemplate, USER_ID, CLASSROOM_ID, slotDuration.minutes());
		BookingResponseDto bookingResult = httpResponse.getBody().details();

		// Act
		ResponseEntity<StandardResponse<WatchAlertResponseDto>> httpAddWatchAlertResponse = addWatchAlert(bookingResult);

		// Assert
		StandardResponse<WatchAlertResponseDto> response = httpAddWatchAlertResponse.getBody();
		List<WatchAlert> watchAlerts = watchAlertRepository.findAll();
		String userEmail = UserContext.getEmail();
		
		assertAll(
				() -> assertEquals(HttpStatus.CREATED, response.status()),
				() -> assertNotNull(response.details()),
				() -> assertEquals("Watch alert created successfully", response.message()),
				() -> assertEquals(1, watchAlerts.size()),
				() -> assertTrue(watchAlerts.stream().allMatch(w -> w.getIdBooking() == bookingResult.idBooking()
						&& w.getUserEmail().equals(userEmail)))
			);
	}
    
    private ResponseEntity<StandardResponse<WatchAlertResponseDto>> addWatchAlert(BookingResponseDto bookingResult){
    	Long idBooking = bookingResult.idBooking(); 
    	
		String addWatchAlertUrl = UriComponentsBuilder.fromPath(BookingEndpoints.WATCH_ALERTS)
				.queryParam("idBooking", idBooking)
				.toUriString();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(new GenerateJwt(secretKey).withIdUser(USER_ID).build());
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		
		return testRestTemplate.exchange(
				addWatchAlertUrl, 
		        HttpMethod.POST, 
		        httpRequest, 
		        new ParameterizedTypeReference<StandardResponse<WatchAlertResponseDto>>() {} 
		);
    }
}