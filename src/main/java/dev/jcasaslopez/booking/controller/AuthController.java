package dev.jcasaslopez.booking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

@RestController
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@GetMapping(value = Endpoints.GENERATE_TOKEN)
	public ResponseEntity<StandardResponse<String>> generateToken(@RequestParam(defaultValue = "1") int idUser) {
		logger.debug("GET /generate-token?idUser={}", idUser);

		String message = String.format("JWT created successfully for user ID %s", idUser);
		StandardResponse<String> response = new StandardResponse<>(message, AuthTestHelper.generateTestJwt(idUser), HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
