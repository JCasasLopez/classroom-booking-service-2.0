package dev.jcasaslopez.booking.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.domain.UserInfo;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class BookingAuthFilterUnitTest {
	
	@Mock JwtService jwtService;
    @Mock FilterChain filterChain;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    private final static String secretKey = "MTIzNDU2Nzg5MEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaMDEyMzQ1Njc4OTA=";
    private static final String EMAIL = "user@example.com";
	private static final Integer USER_ID = 1;
    private BookingAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new BookingAuthenticationFilter(jwtService, secretKey);
    }
    
    @Test
    void auth_filter_does_not_require_authentication_for_searches() {
    	// Arrange
    	when(request.getRequestURI()).thenReturn(BookingEndpoints.AVAILABILITY_CALENDAR);
    	
    	// Act
    	boolean shouldNotFilterResult = filter.shouldNotFilter(request);

        // Assert
        assertTrue(shouldNotFilterResult);
    }
    
    @Test
    void auth_filter_returns_error_401_if_jwt_is_invalid() throws ServletException, IOException {
    	// Arrange
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
    	when(request.getHeader("Authorization")).thenReturn("valid_jwt");
        when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.of(new UserInfo(EMAIL, USER_ID)));

    	// Act
    	filter.doFilterInternal(request, response, filterChain);

    	// Assert
    	verify(jwtService).validateJwt(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }
    
    @Test
    void auth_filter_sets_user_info_correctly_in_ThreadLocal() throws IOException, ServletException {
    	// Arrange
    	when(request.getHeader("Authorization")).thenReturn("valid_jwt");
    	when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.of(new UserInfo(EMAIL, USER_ID)));

    	// UserContext gets cleared in the "finally" block right after filterChain.doFilter() runs, so by the time 
    	// doFilterInternal() returns, the values are already gone.
    	//
    	// By default, mocking a void method makes Mockito do nothing when it's called. doAnswer() replaces that 
    	// "do nothing" with our own code, which runs exactly when the production code invokes 
    	// filterChain.doFilter(request, response). "invocation" represents that actual call (its args, target method...) 
    	// unused here, but available if needed. The lambda body IS what gets executed in place of the real doFilter() call.
        doAnswer(invocation -> {
            assertAll("UserContext values",
                () -> assertEquals(EMAIL, UserContext.getEmail(), "User email should match"),
                () -> assertEquals(USER_ID, UserContext.getIdUser(), "User ID should match")
            );
            return null;
        }).when(filterChain).doFilter(any(), any());

    	// Act
    	filter.doFilterInternal(request, response, filterChain);
    }
    
    @Test
    void auth_filter_clears_UserContext_whatever_happens() throws ServletException, IOException {
		// Arrange
    	when(request.getHeader("Authorization")).thenReturn("jwt");
    	when(jwtService.validateJwt(any(), any(), any())).thenReturn(Optional.of(new UserInfo(EMAIL, USER_ID)));
		
		// Act
    	filter.doFilterInternal(request, response, filterChain);
    	
    	// Assert
    	assertAll("UserContext cleanup",
    			() -> assertThrows(IllegalStateException.class, () -> UserContext.getEmail()),
    			() -> assertThrows(IllegalStateException.class, () -> UserContext.getIdUser())
    			);
    }		
}
