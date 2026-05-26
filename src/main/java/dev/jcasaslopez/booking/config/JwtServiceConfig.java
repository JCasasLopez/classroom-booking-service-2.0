package dev.jcasaslopez.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.classroom.shared.security.JwtService;

@Configuration
public class JwtServiceConfig {
	
	@Bean
    JwtService jwtService() {
        return new JwtService();
    }

}
