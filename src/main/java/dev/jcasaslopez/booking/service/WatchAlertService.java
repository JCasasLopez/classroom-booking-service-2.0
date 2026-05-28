package dev.jcasaslopez.booking.service;

import java.time.LocalDateTime;
import java.util.List;

import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;

public interface WatchAlertService {
	
	WatchAlertResponseDto addWatchAlert(WatchAlertRequestDto watchAlertDto);
	List<WatchAlertResponseDto> watchAlertsListByUserAndTimePeriod(LocalDateTime start, LocalDateTime finish);	
}