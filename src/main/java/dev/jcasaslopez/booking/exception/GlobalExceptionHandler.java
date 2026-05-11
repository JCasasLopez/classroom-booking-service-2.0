package dev.jcasaslopez.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

@ControllerAdvice	
public class GlobalExceptionHandler {
		
	@ExceptionHandler({InvalidBookingException.class, SlotNotValidException.class, SlotOutOfOpeningHoursException.class})
	public ResponseEntity<StandardResponse> handleBadRequest (RuntimeException ex){
		StandardResponse response = new StandardResponse (ex.getMessage(), null, HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler({NoSuchBookingException.class, NoSuchClassroomException.class})
	public ResponseEntity<StandardResponse> handleNotFound (RuntimeException ex){
		StandardResponse response = new StandardResponse (ex.getMessage(), null, HttpStatus.NOT_FOUND);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
}