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

@ControllerAdvice	
public class GlobalExceptionHandler {
			
	@ExceptionHandler({InvalidBookingException.class, 
		SlotNotValidException.class, 
		SlotOutOfOpeningHoursException.class, 
		IllegalStateException.class, 
		IllegalArgumentException.class})
	public ResponseEntity<StandardResponse> handleBadRequest (RuntimeException ex){
		StandardResponse response = new StandardResponse (ex.getMessage(), null, HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<StandardResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
	    String message = "Malformed or unreadable JSON request";
	    StandardResponse response = new StandardResponse(message, null, HttpStatus.BAD_REQUEST);
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler({NoSuchBookingException.class, 
		NoSuchClassroomException.class})
	public ResponseEntity<StandardResponse> handleNotFound (RuntimeException ex){
		StandardResponse response = new StandardResponse (ex.getMessage(), null, HttpStatus.NOT_FOUND);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<StandardResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		List<String> errors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.toList());

		StandardResponse response = new StandardResponse("Validation failed for one or more fields", errors.toString(), HttpStatus.BAD_REQUEST);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler({JsonMappingException.class,
		JsonProcessingException.class})
    public ResponseEntity<StandardResponse> handleJsonExceptions(JsonProcessingException ex) {
    	StandardResponse response = new StandardResponse ("Error serializing or deserializing JSON data", null, HttpStatus.INTERNAL_SERVER_ERROR);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
	
}