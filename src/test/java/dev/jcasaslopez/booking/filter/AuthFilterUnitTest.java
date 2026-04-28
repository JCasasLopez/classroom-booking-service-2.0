package dev.jcasaslopez.booking.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class AuthFilterUnitTest {
	
	@Mock JwtService jwtService;
    @Mock FilterChain filterChain;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    private final static String secretKey = "MTIzNDU2Nzg5MEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaMDEyMzQ1Njc4OTA=";
    private AuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(jwtService, secretKey);
    }

    @Test
    void auth_filter_does_not_require_authentication_for_searches() throws ServletException, IOException {
    	// Arrange
    	when(request.getRequestURI()).thenReturn("/searches/something");

    	// Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);	
    }
     
    @Test
    void auth_filter_returns_error_401_if_jwt_is_invalid() throws ServletException, IOException {
    	// Arrange
    	when(request.getRequestURI()).thenReturn("/booking/something");
    	when(request.getHeader("Authorization")).thenReturn("invalid_token");
        when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.empty());

    	// Act
    	filter.doFilterInternal(request, response, filterChain);

    	// Assert
    	verify(jwtService).validateJwt(any(), any(), any());
    	verify(response).sendError(401, "Authentication failed");
    	verify(filterChain, never()).doFilter(any(), any());
    }
       
    @Test
    void auth_filter_continues_with_the_filter_chain_when_jwt_is_valid() throws ServletException, IOException {
    	// Arrange
    	when(request.getRequestURI()).thenReturn("/booking/something");
    	when(request.getHeader("Authorization")).thenReturn("valid_jwt");
        when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.of("user_email"));

    	// Act
    	filter.doFilterInternal(request, response, filterChain);

    	// Assert
    	verify(jwtService).validateJwt(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void auth_filter_sets_user_email_correctly_in_ThreadLocal() throws IOException, ServletException {
    	// Arrange
    	when(request.getRequestURI()).thenReturn("/booking/something");
    	when(request.getHeader("Authorization")).thenReturn("valid_jwt");
    	when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.of("user_email"));

    	// Capture UserContext at the moment filterChain.doFilter() is called, before finally clears it
    	doAnswer(invocation -> {
    		assertEquals("user_email", UserContext.getEmail());
    		return null;
    	}).when(filterChain).doFilter(any(), any());

    	// Act
    	filter.doFilterInternal(request, response, filterChain);
    
    }
    
	// Tests the 3 authentication scenarios tested above. UserContext has to be cleared in all of them for the test to pass.
    @ParameterizedTest
    @MethodSource("provideAuthScenarios")
    void auth_filter_clears_UserContext_whatever_happens(String route, Optional<String> validationResult) throws ServletException, IOException {
    	// Arrange
    	when(request.getRequestURI()).thenReturn(route);
    	if(validationResult != null) {
    		when(request.getHeader("Authorization")).thenReturn("anything");
            when(jwtService.validateJwt(any(), any(), any())).thenReturn(validationResult);
    		}
    	
    	// Act
    	filter.doFilterInternal(request, response, filterChain);

    	// Assert
    	assertNull(UserContext.getEmail());
    }
    
    private static Stream<Arguments> provideAuthScenarios() {
    	// This method provides the same 3 authentication scenarios tested so far:
    	// 1) For a search, there is no authentication.
    	// 2) For non-searching request, with an invalid JWT
    	// 3) For non-searching request, with a valid JWT
    	return Stream.of(
    			Arguments.of("/searches/something", null),
    			Arguments.of("/bookings/something", Optional.empty()),
    			Arguments.of("/bookings/something", Optional.of("email"))
    			);
    }
       
}