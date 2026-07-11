# Booking Microservice

Booking microservice (v2.0) manages classroom reservations and watch alerts, as part of the Classrooms application — a distributed, event-driven system built with a microservices architecture (see full [technical documentation](#) for details).

## Features

- Makes classroom reservations *
- Cancels classroom reservations *
- Retrieves booking history by user
- Retrieves availability calendar by classroom
- Searches available classrooms by time period / features
- Creates watch alerts *
- Retrieves watch alert history by user and time period

\* Endpoints marked with an asterisk also trigger a notification to the user's email.

## Tech Stack

- Java 17
- Spring Boot 3.4.5
- MySQL
- Kafka
- Docker
- JJWT
- Maven

**Testing**
- JUnit
- Mockito
- Testcontainers

**Internal dependency**
- `classroom-shared-library` — shared DTOs, JWT validation, Kafka event payloads and global exception handling across the ecosystem.

## Local Execution Prerequisites

- **Java 17**
- **Docker Desktop** — used to run MySQL and Kafka locally.
- **Maven** 
- **`classroom-shared-library`** — this project depends on an internal shared library that is not published to Maven Central. It must be built and installed locally before compiling Booking:

```bash
  git clone <shared-library-repo-url>
  cd classroom-shared-library
  mvn clean install
```

- **MySQL and Kafka running** — spin up both via Docker (see `docker-compose.yml`).
