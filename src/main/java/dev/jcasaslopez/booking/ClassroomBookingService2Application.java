package dev.jcasaslopez.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import dev.jcasaslopez.booking.util.AuthTestHelper;

@EnableScheduling
@SpringBootApplication
public class ClassroomBookingService2Application {

	public static void main(String[] args) {
		SpringApplication.run(ClassroomBookingService2Application.class, args);
		
		// Prints a valid JWT upon startup to facilitate authentication for API exploration via Postman.
		System.out.println("\n*************************************************************");
		System.out.println("JWT FOR DEMO PURPOSES:");
		System.out.println(AuthTestHelper.generateTestJwt());
		System.out.println("*************************************************************\n");
	}

}
