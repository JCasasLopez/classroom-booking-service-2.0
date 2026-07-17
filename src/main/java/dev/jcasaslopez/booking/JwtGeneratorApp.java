package dev.jcasaslopez.booking;

import org.springframework.boot.SpringApplication;
import dev.jcasaslopez.booking.util.AuthTestHelper;

public class JwtGeneratorApp {

	// Prints a valid JWT upon startup to facilitate authentication for API exploration via Postman.
	public static void main(String[] args) {
		SpringApplication.run(ClassroomBookingService2Application.class, args);

		System.out.println("\n-------------------------------------------");
		System.out.println("JWT FOR DEMO PURPOSES:");
		System.out.println(AuthTestHelper.generateTestJwt());
		System.out.println("-------------------------------------------\n");

	}

}
