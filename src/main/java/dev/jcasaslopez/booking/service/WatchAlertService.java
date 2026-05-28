package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;

public interface WatchAlertService {
	
	WatchAlertResponseDto addWatchAlert(Long idBooking);
	List<WatchAlertResponseDto> watchAlertsListByUserAndTimePeriod(LocalDateTime start, LocalDateTime finish);	
}