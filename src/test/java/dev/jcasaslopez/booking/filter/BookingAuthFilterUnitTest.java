package dev.jcasaslopez.booking.filter;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class BookingAuthFilterUnitTest {
	
	@Mock JwtService jwtService;
    @Mock FilterChain filterChain;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    private final static String secretKey = "MTIzNDU2Nzg5MEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaMDEyMzQ1Njc4OTA=";
   
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
    
}
