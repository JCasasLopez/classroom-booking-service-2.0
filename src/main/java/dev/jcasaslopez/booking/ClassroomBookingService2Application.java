package dev.jcasaslopez.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ClassroomBookingService2Application {

	public static void main(String[] args) {
		SpringApplication.run(ClassroomBookingService2Application.class, args);
	}

}
