package dev.jcasaslopez.booking.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.enums.TokenPurpose;
import dev.jcasaslopez.classroom.shared.handler.StandardResponseHandler;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.PublicSwaggerPaths;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class AuthFilterUnitTest {
	
	@Mock JwtService jwtService;
	@Mock StandardResponseHandler standardResponseHandler;
	@Mock HttpServletRequest request;

	private static final String secretKey = "...";
	private BookingAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		filter = new BookingAuthenticationFilter(jwtService, secretKey, standardResponseHandler);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			BookingEndpoints.CLASSROOMS_AVAILABLE,
			BookingEndpoints.CLASSROOM_AVAILABILITY,
			BookingEndpoints.TARGET_BOOKING,
			PublicSwaggerPaths.SWAGGER_UI,
			PublicSwaggerPaths.API_DOCS
	})
	void shouldNotFilter_returns_true_for_excluded_paths(String path) {
		// Arrange
		when(request.getRequestURI()).thenReturn(path);
		
		// Act & Assert
		assertTrue(filter.shouldNotFilter(request));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			BookingEndpoints.BOOKINGS,
			BookingEndpoints.CANCEL,
			BookingEndpoints.WATCH_ALERTS
	})
	void shouldNotFilter_returns_false_for_protected_paths(String path) {
		// Arrange
		when(request.getRequestURI()).thenReturn(path);
		
		// Act & Assert
		assertFalse(filter.shouldNotFilter(request));
	}

	@Test
	void validateToken_uses_Access_TokenType_but_not_AdminRole() {
	    // Arrange
	    String authHeader = "Bearer valid-token";

	    // Act
	    filter.validateToken(authHeader);

	    // Assert
	    verify(jwtService).validateJwt(authHeader, secretKey, TokenPurpose.ACCESS);
	}
}