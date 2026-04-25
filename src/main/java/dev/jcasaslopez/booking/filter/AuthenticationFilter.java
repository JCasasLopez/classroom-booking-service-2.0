package dev.jcasaslopez.booking.filter;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.classroom.shared.enums.TokenType;
import dev.jcasaslopez.classroom.shared.security.JwtService;
import dev.jcasaslopez.classroom.shared.utility.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	private final JwtService jwtService;
	private final String base64SecretKey;

	public AuthenticationFilter(JwtService jwtService, 	@Value("${jwt.secretKey}") String base64SecretKey) {
		this.jwtService = jwtService;
		this.base64SecretKey = base64SecretKey;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		logger.debug("Entering AuthenticationFilter...");
		
		String requestURI = request.getRequestURI();

	    // If it is a search, allow the request to go through without verifying the token.
	    if (requestURI.contains("/searches/")) {
	        filterChain.doFilter(request, response);
	        return;
	    }
		
		String authHeader = request.getHeader("Authorization");

		// Exceptions thrown in a Filter are NOT caught by @RestControllerAdvice because Filters sit outside the Spring
		// DispatcherServlet context, so we use response.sendError() to manually trigger a 401 Unauthorized response.
		Optional<String> validationResult = jwtService.validateJwt(authHeader, base64SecretKey, TokenType.ACCESS);
		try {
		    if (validationResult.isEmpty()) {
		        response.sendError(401, "Authentication failed");
		        return; 
		    }
		    // We need access to user's email address to send notification.
		    UserContext.setEmail(validationResult.get());
		    filterChain.doFilter(request, response);
		} finally {
		    UserContext.clear(); 
		}
	}

}
