package dev.jcasaslopez.booking.filter;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.classroom.shared.domain.UserInfo;
import dev.jcasaslopez.classroom.shared.enums.TokenType;
import dev.jcasaslopez.classroom.shared.filter.AuthenticationFilterBase;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.PublicSwaggerPaths;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class BookingAuthenticationFilter extends AuthenticationFilterBase {

	private static final Set<String> EXCLUDED_PATHS = Set.of(
	        BookingEndpoints.AVAILABILITY_CALENDAR, BookingEndpoints.CLASSROOMS_AVAILABILITY,
	        BookingEndpoints.BOOKING_BY_SLOT, BookingEndpoints.GENERATE_TOKEN,
	        PublicSwaggerPaths.SWAGGER_UI, PublicSwaggerPaths.API_DOCS
	    );

	    public BookingAuthenticationFilter(JwtService jwtService, @Value("${jwt.secretKey}") String secretKey) {
	        super(jwtService, secretKey);
	    }

	    @Override
	    protected boolean shouldNotFilter(HttpServletRequest request) {
	        String path = request.getRequestURI();
	        return EXCLUDED_PATHS.stream().anyMatch(path::contains);
	    }

	    @Override
	    protected Optional<UserInfo> validateToken(String authHeader) {
	        return jwtService.validateJwt(authHeader, base64SecretKey, TokenType.ACCESS);
	    }

}
