package dev.jcasaslopez.booking.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice	
public class GlobalExceptionHandler {
				
	@ExceptionHandler({InvalidBookingException.class, 
		SlotNotValidException.class, 
		SlotOutOfOpeningHoursException.class, 
		IllegalStateException.class, 
		IllegalArgumentException.class})
	public ResponseEntity<StandardResponse<Void>> handleBadRequest (RuntimeException ex){
		StandardResponse<Void> response = new StandardResponse<>(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<StandardResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
	    String message = "Malformed or unreadable JSON request";
	    StandardResponse<Void> response = new StandardResponse<>(message, null, HttpStatus.BAD_REQUEST);
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(InvalidBookingStatusException.class)
	public ResponseEntity<StandardResponse<Void>> handleBookingNotActive(InvalidBookingStatusException ex) {
	    StandardResponse<Void> response = new StandardResponse<>(ex.getMessage(), null, HttpStatus.CONFLICT);
	    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}
	
	@ExceptionHandler({NoSuchBookingException.class, 
		NoSuchClassroomException.class})
	public ResponseEntity<StandardResponse<Void>> handleNotFound (RuntimeException ex){
		StandardResponse<Void> response = new StandardResponse<>(ex.getMessage(), null, HttpStatus.NOT_FOUND);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<StandardResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		List<String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.toList());

		StandardResponse<String> response = new StandardResponse<>("Validation failed for one or more fields", errors.toString(), HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<StandardResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
	    String message = ex.getConstraintViolations().stream()
	            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
	            .collect(Collectors.joining(", "));
	    StandardResponse<Void> response = new StandardResponse<>(message, null, HttpStatus.BAD_REQUEST);
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler({JsonMappingException.class,
		JsonProcessingException.class})
    public ResponseEntity<StandardResponse<Void>> handleJsonExceptions(JsonProcessingException ex) {
    	StandardResponse<Void> response = new StandardResponse<>("Error serializing or deserializing JSON data", null, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
	
	@ExceptionHandler({DataIntegrityException.class})
    public ResponseEntity<StandardResponse<Void>> handleDataIntegrityException(DataIntegrityException ex) {
    	StandardResponse<Void> response = new StandardResponse<>(ex.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
	
}