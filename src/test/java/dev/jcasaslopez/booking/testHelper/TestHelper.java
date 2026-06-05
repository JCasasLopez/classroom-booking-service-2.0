package dev.jcasaslopez.booking.testHelper;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class TestHelper {
		
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
	            .claim("email", "jorgecasas78@hotmail.com")
	            .issuedAt(new Date(System.currentTimeMillis()))
	            .expiration(new Date(System.currentTimeMillis() + 3600_000)) 
	            .signWith(key, Jwts.SIG.HS256)
	            .compact();	    
	}
	
	// Wait until the classrooms Kafka topic is available before running the test.
	public static void waitForClassroomStore(List<?> classroomsStore) {
		 Awaitility.await()
	        .atMost(15, TimeUnit.SECONDS)
	        .pollInterval(500, TimeUnit.MILLISECONDS)
	        .until(() -> !classroomsStore.isEmpty());
	}
	
	public static BookingResponseDto extractBookingResponse(StandardResponse body, ObjectMapper mapper) {
	    return mapper.convertValue(body.details(), BookingResponseDto.class);
	}
	
	public static void produceClassroomEvents(String bootstrapServers) throws Exception {
	    try (KafkaProducer<String, ClassroomEvent> producer = new KafkaProducer<>(buildProducerProps(bootstrapServers))) {
	        for (ClassroomEvent classroom : buildClassroomEvents()) {
	            producer.send(new ProducerRecord<>("classrooms", classroom)).get();
	        }
	    }
	}

	private static Properties buildProducerProps(String bootstrapServers) {
	    Properties props = new Properties();
	    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
	    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
	    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
	    props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
	    return props;
	}

	private static List<ClassroomEvent> buildClassroomEvents() {
	    return List.of(
		    new ClassroomEvent(1, "Salón de Actos", 150, true, true),
	        new ClassroomEvent(2, "Aula 102", 25, false, false),
	        new ClassroomEvent(3, "Aula Marie Curie", 45, false, true)
	    );
	}
}