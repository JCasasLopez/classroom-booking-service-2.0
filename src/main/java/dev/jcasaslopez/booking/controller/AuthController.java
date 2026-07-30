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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Auth (Testing utility)", description = "Development/testing helper for generating JWTs to explore the API with tools like Postman")
public class AuthController {

	private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	@Operation(
			summary = "[Testing only] Generates a valid JWT for a given user id",
			description = """
					Convenience endpoint for exploring the API with tools like Postman, without going through the full login flow.
					Generates a valid access token for the given `idUser`, defaulting to user id 1 if not specified.

					This endpoint bypasses real authentication and should never be exposed in a production environment.
					"""
			)
	@ApiResponse(responseCode = "200", description = "JWT generated successfully",
	content = @Content(schema = @Schema(implementation = StandardResponse.class)))
	@GetMapping(value = Endpoints.GENERATE_TOKEN)
	public ResponseEntity<StandardResponse<String>> generateToken(@RequestParam(defaultValue = "1") int idUser) {
		logger.debug("GET /generate-token?idUser={}", idUser);

		String message = String.format("JWT created successfully for user ID %s", idUser);
		StandardResponse<String> response = new StandardResponse<>(message, AuthTestHelper.generateTestJwt(idUser), HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
