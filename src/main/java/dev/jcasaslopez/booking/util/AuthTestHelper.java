package dev.jcasaslopez.booking.util;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class AuthTestHelper {
	
	public static String generateTestJwt() {
		String base64SecretKey = "MTIzNDU2Nzg5MEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaMDEyMzQ1Njc4OTA=";
	    byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey);
	    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

	    return Jwts.builder()
	            .header().type("JWT").and()
	            .subject("Username")
	            .id(UUID.randomUUID().toString())
	            .claim("roles", List.of("ROLE_USER"))
	            .claim("idUser", 1)
	            .claim("purpose", "access")
	            .claim("email", "user@example.com")
	            .issuedAt(new Date(System.currentTimeMillis()))
	            .expiration(new Date(System.currentTimeMillis() + 3600_000)) 
	            .signWith(key, Jwts.SIG.HS256)
	            .compact();	    
	}

}
