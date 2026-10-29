package dev.jcasaslopez.booking.config;

import java.time.DayOfWeek;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opening-times")
public record WeeklyScheduleProperties(Map<DayOfWeek, String> hours) {}