package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
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
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class WatchAlertEndpointsTest extends BaseIntegrationTest {
	
    @Value("${time-slot.duration}") private int slotDuration;
	
    @Test
    @Order(1)
	void add_watch_alert_endpoint_returns_the_expected_response() {
		// Arrange	
		BookingResponseDto bookingResult = putInBooking();
		
		// Act
		ResponseEntity<StandardResponse> httpAddWatchAlertResponse = addWatchAlert(bookingResult);

		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.CREATED, httpAddWatchAlertResponse.getBody().status()),
				() -> assertNotNull(httpAddWatchAlertResponse.getBody().details()),
				() -> assertEquals("Watch alert created successfully", httpAddWatchAlertResponse.getBody().message())				);
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
    	
    	ResponseEntity<StandardResponse> httpResponse = testRestTemplate.exchange(
    			getWatchAlertsUrl, HttpMethod.GET, httpRequest, StandardResponse.class);
    	
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
		BookingRequestDto bookingDto = new BookingRequestDto(1, classroomId, TestHelper.generateBookingSlots(slotDuration));

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<BookingRequestDto> httpBookingRequest = new HttpEntity<>(bookingDto, headers);

		ResponseEntity<StandardResponse> httpBookingResponse = testRestTemplate.postForEntity
				(Endpoints.BOOK, httpBookingRequest, StandardResponse.class);
		
		return TestHelper.extractBookingResponse(httpBookingResponse.getBody(), objectMapper);
    }
    
    private ResponseEntity<StandardResponse> addWatchAlert(BookingResponseDto bookingResult){
    	Long idBooking = bookingResult.idBooking(); 
		String addWatchAlertUrl = UriComponentsBuilder.fromPath(Endpoints.ADD_WATCH_ALERT)
				.queryParam("idBooking", idBooking)
				.toUriString();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		return testRestTemplate.postForEntity(
		        addWatchAlertUrl,
		        httpRequest,
		        StandardResponse.class
		);
    }
}