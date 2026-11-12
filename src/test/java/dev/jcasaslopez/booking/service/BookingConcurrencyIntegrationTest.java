package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.TransactionTimedOutException;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

public class BookingConcurrencyIntegrationTest extends BaseIntegrationTest {
	
	private static final int THREADS = 10;
	private ClassroomEvent classroom1;
	private ClassroomEvent classroom2;

    @Autowired private BookingService bookingService;
    
    @BeforeEach
    void pickClassrooms() {
        classroom1 = classroomsStore.get(0);
        classroom2 = classroomsStore.get(1);
    }

    private record Outcome(BookingResponseDto booking, Throwable error) {}
    
    @RepeatedTest(20)
    void only_one_of_many_concurrent_bookings_for_the_same_slot_succeeds_test() throws Exception {
        LocalDateTime start = nextMondayAt10();
        LocalDateTime finish = start.plusMinutes(slotDuration.minutes());   

        List<Callable<BookingResponseDto>> tasks = new ArrayList<>();
        for (int i = 1; i <= THREADS; i++) {
            int idUser = i;   // different users, so the weekly limit does not interfere
            tasks.add(() -> bookingService.book(
                    new BookingRequestDto(classroom1.idClassroom(), start, finish), idUser, "user" + idUser + "@example.com"));
        }

        List<Outcome> outcomes = runSimultaneously(tasks);

        long successes = outcomes.stream().filter(o -> o.error() == null).count();
        assertEquals(1, successes, "exactly one booking must succeed");
        assertEquals(1, repository.count(), "exactly one booking must be persisted");

        // The rest must fail because the slot is taken, not because of lock timeouts
        outcomes.stream().filter(o -> o.error() != null).forEach(o -> {
            assertFalse(o.error() instanceof PessimisticLockingFailureException, "lock wait timed out");
            assertFalse(o.error() instanceof TransactionTimedOutException, "transaction timed out");
        });
    }
    
    @Test
    void concurrent_bookings_for_different_classrooms_all_succeed_test() throws Exception {
        LocalDateTime start = nextMondayAt10();
        LocalDateTime finish = start.plusMinutes(slotDuration.minutes());   

        List<Callable<BookingResponseDto>> tasks = List.of(
                () -> bookingService.book(new BookingRequestDto(classroom1.idClassroom(), start, finish), 1, "user1@example.com"),
                () -> bookingService.book(new BookingRequestDto(classroom2.idClassroom(), start, finish), 2, "user2@example.com"));

        List<Outcome> outcomes = runSimultaneously(tasks);

        assertTrue(outcomes.stream().allMatch(o -> o.error() == null), "both bookings must succeed");
        assertEquals(2, repository.count());
    }

    
    // Starts all tasks at the same instant and collects every outcome
    private List<Outcome> runSimultaneously(List<Callable<BookingResponseDto>> tasks) throws Exception {
    	ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    	CountDownLatch ready = new CountDownLatch(tasks.size());
    	CountDownLatch go = new CountDownLatch(1);
    	try {
    		List<Future<Outcome>> futures = new ArrayList<>();
    		for (Callable<BookingResponseDto> task : tasks) {
    			futures.add(pool.submit(() -> {
    				ready.countDown();
    				go.await();                       // wait at the starting line
    				try {
    					return new Outcome(task.call(), null);
    				} catch (Throwable t) {
    					return new Outcome(null, t);
    				}
    			}));
    		}
    		assertTrue(ready.await(10, TimeUnit.SECONDS), "threads were not ready in time");
    		go.countDown();                           // starting gun

    		List<Outcome> outcomes = new ArrayList<>();
    		for (Future<Outcome> future : futures) {
    			outcomes.add(future.get(30, TimeUnit.SECONDS));
    		}
    		return outcomes;
    	} finally {
    		pool.shutdownNow();
    	}
    }

    private LocalDateTime nextMondayAt10() {
    	return LocalDate.now()
    			.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    			.atTime(10, 0);
    }
}