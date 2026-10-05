package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;

public interface WatchAlertService {
	
	WatchAlertResponseDto addWatchAlert(Long idBooking, String email);
	List<WatchAlertResponseDto> watchAlertsListByUserAndTimePeriod(LocalDateTime start, LocalDateTime finish, String email);	
}