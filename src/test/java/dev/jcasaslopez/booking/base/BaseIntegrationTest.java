package dev.jcasaslopez.booking.base;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.testHelper.TestHelper;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @Autowired protected TestRestTemplate testRestTemplate;
    @Autowired protected List<ClassroomEvent> classroomStore;
    @Autowired protected ObjectMapper objectMapper;

    @ServiceConnection
    @Container
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3");

    @Container
    static final KafkaContainer kafkaContainer = 
        new KafkaContainer(DockerImageName.parse("apache/kafka"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @BeforeAll
    static void setup() throws Exception {
        TestHelper.produceClassroomEvents(kafkaContainer.getBootstrapServers());
    }
    
    @BeforeEach
    void waitForStore() {
        TestHelper.waitForClassroomStore(classroomStore);
    }
}