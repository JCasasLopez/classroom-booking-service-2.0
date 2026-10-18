package dev.jcasaslopez.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.jcasaslopez.booking.entity.NotificationOutbox;

public interface OutboxRepository extends JpaRepository<NotificationOutbox, Long> {

}
