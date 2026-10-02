package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.mapper.BookingMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.context.UserContext;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;

public class FindBookingByEndpointTest extends BaseIntegrationTest {
	
	@Autowired BookingRepository repository;
	@Autowired BookingMapper mapper;
	@Autowired WeeklySchedule weeklySchedule;
	
	private static final int SLOT_DURATION = 30;
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	private static final String EMAIL = "user@example.com";
	
	@Test
	void find_booking_endpoint_returns_the_expected_response() {
		// Arrange
		TestHelper.createBooking(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);
		
		// Act
		ResponseEntity<StandardResponse<Long>> httpResponse = getHttpResponse();
		
		// Assert
		StandardResponse<Long> response = httpResponse.getBody();
		assertAll(
				() -> assertEquals(HttpStatus.OK, response.status()),
				() -> assertInstanceOf(Long.class, response.details()),
				() -> assertTrue(response.message().startsWith("Active booking for classroom 1 between") 
		                  && response.message().endsWith("retrieved successfully"))
				);	
	}
	
	@Test
	void find_booking_endpoint_returns_500_if_finds_more_than_one_booking() {
		// Arrange
		TestHelper.createBooking(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);
		
		// As the API prevents creating overlapping bookings, we bypass the endpoint and persist a conflicting 
		// record directly via the repository to force  a data consistency error (HTTP 500) during retrieval.
		createConflictingBooking(CLASSROOM_ID, USER_ID, EMAIL);
		
		// Act
		ResponseEntity<StandardResponse<Long>> httpResponse = getHttpResponse();

		// Assert
		StandardResponse<Long> response = httpResponse.getBody();
		assertAll(
				() -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status()),
				() -> assertTrue(response.message().equals("An internal data consistency error has occurred"))
				);	
	}
	
// *********************************** AUXILIARY METHODS ***********************************
    
	private ResponseEntity<StandardResponse<Long>> getHttpResponse(){
		HttpHeaders headers = new HttpHeaders();
		String bookingBySlotUrl = UriComponentsBuilder.fromPath(BookingEndpoints.TARGET_BOOKING)
				.queryParam("start", TestHelper.generateStartSearch())
				.queryParam("finish", TestHelper.generateFinishSearch(30))
				.queryParam("idClassroom", CLASSROOM_ID)
				.toUriString();
		HttpEntity<Void> httpRequest =  new HttpEntity<>(headers); 

		return testRestTemplate.exchange(
				bookingBySlotUrl, 
				HttpMethod.GET,
				httpRequest, 
				new ParameterizedTypeReference<StandardResponse<Long>>() {}
				);
	}
	
    private void createConflictingBooking(int classroomId, int idUser, String email) {
    	BookingRequestDto conflictingBookingDto = new BookingRequestDto(classroomId, TestHelper.generateBookingSlots(SLOT_DURATION));
    	UserContext.setContext(email, idUser);
    	Booking conflictingbooking = mapper.toEntity(conflictingBookingDto, weeklySchedule);
    	repository.saveAndFlush(conflictingbooking);
    }

}