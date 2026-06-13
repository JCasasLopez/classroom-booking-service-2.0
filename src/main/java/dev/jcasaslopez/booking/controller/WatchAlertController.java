package dev.jcasaslopez.booking.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.service.WatchAlertService;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
public class WatchAlertController {

private static final Logger logger = LoggerFactory.getLogger(WatchAlertController.class);
	
	private final WatchAlertService service;
	
	public WatchAlertController(WatchAlertService service) {
		this.service = service;
	}

	@PostMapping(value=Endpoints.ADD_WATCH_ALERT)
	public ResponseEntity<StandardResponse<WatchAlertResponseDto>> addWatchAlert(@RequestParam @NotNull @Positive Long idBooking) {
		logger.debug("POST /watch-alerts - idBooking={}", idBooking);
		WatchAlertResponseDto watchAlert = service.addWatchAlert(idBooking);
		
		StandardResponse<WatchAlertResponseDto> response = new StandardResponse<>("Watch alert created successfully", watchAlert, HttpStatus.CREATED);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	// No need to pass any user information as a parameter, as the end-point needs the user to be authenticated, 
	// and the user's email is held in UserContext.
	@GetMapping(value=Endpoints.USER_WATCH_ALERTS)
	public ResponseEntity<StandardResponse<List<WatchAlertResponseDto>>> getWatchAlertsByUser(@RequestParam @NotNull LocalDateTime startSearch, 
			@RequestParam @NotNull LocalDateTime finishSearch) {
		logger.debug("GET /watch-alerts - start={} - finish={}", startSearch, finishSearch);
		
		List<WatchAlertResponseDto> watchAlerts = service.watchAlertsListByUserAndTimePeriod(startSearch, finishSearch);
		
		StandardResponse<List<WatchAlertResponseDto>> response = new StandardResponse<>("Watch alerts retrieved successfully", watchAlerts, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
		
}
