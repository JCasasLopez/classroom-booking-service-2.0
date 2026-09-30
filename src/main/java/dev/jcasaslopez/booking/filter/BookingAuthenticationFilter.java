package dev.jcasaslopez.booking.filter;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.dto.AuthResponse;
import dev.jcasaslopez.classroom.shared.enums.TokenPurpose;
import dev.jcasaslopez.classroom.shared.filter.AuthenticationFilterBase;
import dev.jcasaslopez.classroom.shared.handler.StandardResponseHandler;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.PublicSwaggerPaths;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class BookingAuthenticationFilter extends AuthenticationFilterBase {
	
	private final StandardResponseHandler standardResponseHandler;

	private static final Set<String> EXCLUDED_PATHS = Set.of(
	        BookingEndpoints.AVAILABILITY_CALENDAR, BookingEndpoints.CLASSROOMS_AVAILABILITY,
	        BookingEndpoints.BOOKING_BY_SLOT, BookingEndpoints.GENERATE_TOKEN,
	        PublicSwaggerPaths.SWAGGER_UI, PublicSwaggerPaths.API_DOCS
	    );

	    public BookingAuthenticationFilter(JwtService jwtService, @Value("${jwt.secretKey}") String secretKey, 
	    		StandardResponseHandler standardResponseHandler) {
	        super(jwtService, secretKey, standardResponseHandler);
	    }

	    @Override
	    protected boolean shouldNotFilter(HttpServletRequest request) {
	        String path = request.getRequestURI();
	        return EXCLUDED_PATHS.stream().anyMatch(path::contains);
	    }

	    @Override
	    protected AuthResponse validateToken(String authHeader) {
	        return jwtService.validateJwt(authHeader, base64SecretKey, TokenPurpose.ACCESS);
	    }

}
