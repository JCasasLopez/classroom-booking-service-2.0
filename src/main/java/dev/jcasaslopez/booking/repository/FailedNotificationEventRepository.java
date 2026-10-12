package dev.jcasaslopez.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.booking.entity.FailedNotificationEvent;

public interface FailedNotificationEventRepository extends JpaRepository<FailedNotificationEvent, Long> {

}
