package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

// Controller parameter validation and the following exceptions are already covered
// by existing endpoint tests and are not repeated here:
//- IllegalStateException          	→ 400
//- NoSuchClassroomException        → 404
//- NoSuchBookingException        	→ 404

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WatchAlertEndpointsTest extends BaseIntegrationTest {

	private static final int SLOT_DURATION = 30;

	@Test
	@Order(1)
	void add_watch_alert_endpoint_returns_the_expected_response() {
		// Arrange	
		BookingResponseDto bookingResult = putInBooking();

		// Act
		ResponseEntity<StandardResponse<WatchAlertResponseDto>> httpAddWatchAlertResponse = addWatchAlert(bookingResult);

		// Assert
		StandardResponse<WatchAlertResponseDto> response = httpAddWatchAlertResponse.getBody();
		assertAll(
				() -> assertEquals(HttpStatus.CREATED, response.status()),
				() -> assertNotNull(response.details()),
				() -> assertEquals("Watch alert created successfully", response.message())				
			);
	}
    
    @Test
    @Order(2)
	void get_watch_alerts_endpoint_returns_the_expected_response() throws JsonMappingException, JsonProcessingException {
    	// Arrange	

    	// Act
    	HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
    	String getWatchAlertsUrl = UriComponentsBuilder.fromPath(Endpoints.USER_WATCH_ALERTS)
    			.queryParam("startSearch", TestHelper.generateStartSearch())
    			.queryParam("finishSearch", TestHelper.generateFinishSearch(300))
    			.toUriString();
    	HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
    	
    	ResponseEntity<StandardResponse<List<WatchAlertResponseDto>>> httpResponse = testRestTemplate.exchange(
    			getWatchAlertsUrl, 
    			HttpMethod.GET, 
    			httpRequest, 
    			new ParameterizedTypeReference<StandardResponse<List<WatchAlertResponseDto>>>() {}
    		);
    	
    	// Assert
    	assertAll(
    			() -> assertEquals(HttpStatus.OK, httpResponse.getBody().status()),
    			() -> assertNotNull(httpResponse.getBody().details()),
    			() -> assertEquals("Watch alerts retrieved successfully", httpResponse.getBody().message())
    			);
    		
    }
  
    // *********************************** AUXILIARY METHODS ***********************************
    
    private BookingResponseDto putInBooking() {
    	int classroomId = 1;
		BookingRequestDto bookingDto = new BookingRequestDto(1, classroomId, TestHelper.generateBookingSlots(SLOT_DURATION));

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<BookingRequestDto> httpBookingRequest = new HttpEntity<>(bookingDto, headers);

		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = testRestTemplate.exchange(
		        Endpoints.BOOK, 
		        HttpMethod.POST, 
		        httpBookingRequest, 
		        new ParameterizedTypeReference<StandardResponse<BookingResponseDto>>() {} 
		);
		return httpResponse.getBody().details();
    }
    
    private ResponseEntity<StandardResponse<WatchAlertResponseDto>> addWatchAlert(BookingResponseDto bookingResult){
    	Long idBooking = bookingResult.idBooking(); 
    	
		String addWatchAlertUrl = UriComponentsBuilder.fromPath(Endpoints.ADD_WATCH_ALERT)
				.queryParam("idBooking", idBooking)
				.toUriString();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		
		return testRestTemplate.exchange(
				addWatchAlertUrl, 
		        HttpMethod.POST, 
		        httpRequest, 
		        new ParameterizedTypeReference<StandardResponse<WatchAlertResponseDto>>() {} 
		);
    }
}