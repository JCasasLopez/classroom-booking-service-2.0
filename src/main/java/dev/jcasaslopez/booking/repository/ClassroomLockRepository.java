package dev.jcasaslopez.booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.jcasaslopez.booking.entity.ClassroomLock;
import jakarta.persistence.LockModeType;

public interface ClassroomLockRepository extends JpaRepository<ClassroomLock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassroomLock c WHERE c.idClassroom = :idClassroom")
    Optional<ClassroomLock> lockByIdClassroom(@Param("idClassroom") int idClassroom);
}