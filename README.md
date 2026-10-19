# Booking Microservice

Booking microservice (v3.0) manages classroom reservations and watch alerts, as part of the Classrooms application — a distributed, event-driven system built with a microservices architecture (see full [technical documentation](#) for details).

The entire Classrooms application is deployed and available at [www.book-your-classroom.com](https://www.book-your-classroom.com).

## Table of Contents
- [Purpose](#purpose)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Docker Deployment](#docker-deployment)
- [Configuration and environment variables](#configuration-and-environment-variables)
- [Tests](#tests)
- [Limitations of deploying Booking on its own](#limitations-of-deploying-booking-on-its-own)
- [Exploring the Booking Microservice I: Authentication and Pre-loaded Data](#exploring-the-booking-microservice-i-authentication-and-pre-loaded-data)
- [Exploring the Booking Microservice II: Endpoints](#exploring-the-booking-microservice-ii-endpoints)
- [Swagger](#swagger)
- [Contribution and License](#contribution-and-license)
- [Contact](#contact)

## Purpose

The purpose of this repository is to allow exploring the microservice individually, as opposed to as a part of the entire application found at [www.book-your-classroom.com](https://www.book-your-classroom.com). 

The accompanying database and Kafka Docker containers come with some pre-loaded data - bookings, watch alerts and classrooms- for convenience (see the 'Authentication & Pre-loaded data' section).


## Features

- Makes classroom reservations *
- Cancels classroom reservations *
- Retrieves booking history by user * 
- Retrieves a classroom availability calendar
- Searches available classrooms by time period / features
- Creates watch alerts *
- Retrieves watch alert history by user and time period * 
- Generates a valid JWT: obviously in a production environment this would compromise the system security, but as this is only a demo has been added to allow microservice exploration (all JWTs are created for a user with ID=1 and a fake email address).

\* Endpoints marked with an asterisk require authentication (a valid JWT).


## Tech Stack

- **Java 17**
- **Spring Boot 3.4.5**
- **Persistence & DB**: MySQL
- **Messaging**: Apache Kafka
- **API Documentation**: OpenAPI / Swagger (Springdoc 2.7.0)
- **Containerization**: Docker
- **Build Tool**: Maven

**Testing**
- JUnit 5 & Mockito
- Testcontainers (MySQL & Kafka)
- Spring Kafka Test

**Internal dependency**
- `classroom-shared-library` — shared DTOs, JWT creation and validation, Kafka event payloads and an authentication base filter that is extended by each microservice.


## Docker Deployment

Prerequisites: Docker Desktop only.

This builds the application (Maven and Java run inside the build container — nothing needs to be installed locally) and starts MySQL, Kafka and the microservice together.

1. Clone the repository:

```bash
   git clone https://github.com/JCasasLopez/classroom-booking-service-2.0
```

2. Move into the project directory:

```bash
   cd classroom-booking-service-2.0
```

3. Build and start the containers:

```bash
   docker compose up --build
```

## Configuration and environment variables

This project externalizes configuration (Kafka topic names, JWT secret, database credentials, business rules such as opening times and booking limits) into environment variables, resolved via a `.env` file consumed by Docker Compose.

For convenience, the `.env` file is committed to this repository with demo values, so the project can be started with a single `docker-compose up` command, with no manual setup required.

This is a deliberate decision for a demo/portfolio project, not an oversight. In a real production environment:

- The `.env` file would **NOT** be committed to version control.
- Secrets (JWT key, database credentials) would be stored in a secure, access-controlled system designed for that purpose, instead of a plain text file, and injected into each service only at deploy time.

Since no real infrastructure or user data is involved here, the values in `.env` are safe to expose publicly, and doing so keeps the project easy to run for anyone evaluating it.

## Tests

Since the Booking microservice has a fairly complex business logic, the microservice functionalities are tested comprehensively, with both unit, web and integration tests. Integration tests use **Testcontainers**, so they spin up their own isolated MySQL and Kafka instances — no manual setup required, and they don't interfere with the `docker-compose.yml` environment used for manual exploration.

Tests are skipped during the Docker build (`-DskipTests`) to keep `docker-compose up` fast for manual exploration of the running application. To run the full test suite:

```bash
mvn clean test
```

## Limitations of deploying Booking on its own

As already mentioned at the beginning of this README, the deployment described above only allows working with the Booking microservice on its own — the rest of the Classrooms application is left out. 

Note that the technical documentation describes a production deployment with 3 Kafka brokers for fault tolerance (replication factor of 3, with `min.insync.replicas=2`), which tolerates the loss of one broker without data loss. The `docker-compose.yml` in this repository runs a single broker for local development, so no replication is in effect.

Apart from the obvious inability to work with data from other microservices (Users or Classrooms), the main shortcoming of this approach is that the user will never receive the notifications sent by the system. This is due to two reasons: the email associated with the JWT is a fake email address, and the Notification microservice — which reads the Kafka topic and actually sends the notifications — is not spun up.

To explore the notifications, you can check the `notifications` Kafka topic (obviously, the topic is empty before you perform any action that triggers a notification, which are creating a booking, cancelling a booking, and creating a watch alert):

1. Open a shell inside the Kafka container:

```bash
   docker exec -it kafka-broker bash
```

2. Inside the container, start a consumer to read the messages from the `notifications` topic:

```bash
   kafka-console-consumer --bootstrap-server localhost:9092 --topic notifications --from-beginning
```

## Exploring the Booking Microservice I: Authentication and Pre-loaded Data
As mentioned in the "Features" section, all endpoints except the search-related ones are protected, and an endpoint to generate a valid JWT is provided.

The JWT built by this endpoint generates a token with the claim `idUser=1`, which corresponds to the user associated with the pre-loaded bookings in the database container. Using this default token gives you access to that pre-loaded data out of the box.


**All pre-loaded bookings belong to `idUser=1`, `idClassroom=1`**:

| idBooking | start                | finish               | status    |
|-----------|-----------------------|------------------------|-----------|
| 1         | Next Monday 10:00:00   | Next Monday 11:00:00   | ACTIVE    |
| 2         | 2026-01-01 10:00:00    | 2026-01-01 12:00:00    | COMPLETED |
| 3         | 2026-02-01 10:00:00    | 2026-02-01 12:00:00    | CANCELLED |


**All pre-loaded watch alerts belong to `userEmail = user@example.com` (the email associated with `idUser=1`)**.

Note: Watch alerts store `userEmail` instead of `idUser` to avoid synchronous calls to the User microservice to resolve subscriber emails when sending a notification. This is a safe trade-off since emails are immutable once an account is created, and since both `userEmail` and `idUser` are already available in the JWT (for a thorough discussion on this topic, check the technical documentation).

| idWatchAlert | idBooking | 
|--------------|-----------|
| 1            | 1         | 
| 2            | 2         |
| 3            | 3         | 


Finally, the pre-loaded classrooms are: 

| idClassroom | name | seats | projector | speakers |
|-------------|------|-------|-----------|----------|
| 1           | 101  | 30    | true      | false    |
| 2           | 102  | 50    | false     | true     |


## Exploring the Booking Microservice II: Endpoints

<details>
<summary><b>Generate a valid JWT</b></summary>


To generate a token with the default idUser=1 
```bash
curl --location 'http://localhost:9000/classroom-booking/generate-token'
```

This token must be used in the `Authorization: Bearer <VALID_JWT>` header in all the protected endpoints (only search-related endpoints are public). 

</details>

<details>  
<summary><b>Create a booking</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/bookings' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <VALID_JWT>' \
--data '{
  "idClassroom": 1,
  "startTimeSlotList": [
    "2028-07-27T10:00:00",
    "2028-07-27T10:30:00"
  ]
}'
```

Note that each entry in `startTimeSlotList` marks the start of a 30-minute slot (or whatever time slot duration is set to. Default value is 30 minutes). See full [technical documentation](#) for details. 

In this example, two consecutive slots are booked, resulting in a 10:00–11:00 reservation for Tuesday the 27th of July 2028. The date have to be in the future, and the time within the opening hours (by default Monday to Friday from 9h to 22h. Weekends closed). 

</details>

<details>
<summary><b>Cancel a booking</b></summary>

```bash
curl --location --request PATCH 'http://localhost:9000/classroom-booking/bookings/1/cancel' \
--header 'Authorization: Bearer <VALID_JWT>' 
```
Replace '.../bookings/**1**...' with any other valid idBooking.

Only a booking with ACTIVE status can be cancelled (when a booking is cancelled, it is not deleted from the database, but its status is updated from ACTIVE to CANCELLED).

</details>

<details>
<summary><b>Retrieve booking history by user</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/bookings' \
--header 'Authorization: Bearer <VALID_JWT>'
```

The system extracts the user ID from the JWT and searches the booking history for that user. Since the user ID in the JWT is fixed, only bookings for `idUser = 1` can be retrieved.

</details>

<details>
<summary><b>Retrieve classroom availability calendar</b></summary>

```basht
curl --location 'http://localhost:9000/classroom-booking/classrooms/1/availability?start=2029-01-01T10:00:00&finish=2029-01-01T18:00:00&idClassroom=1'
```

Replace '.../classrooms/**1**...' with any other valid idClassroom, and `start` and `finish` with any future date in ISO-8601 format (`yyyy-MM-ddTHH:mm:ss`) before running the request. This endpoint is public and does not require a token.

`start` and `finish` must be on the same day, on a day the classroom is open (default: Monday to Friday, 9:00–22:00; closed on weekends), and must be valid time slots (default slot duration: 30 minutes, so 10:00, 10:30, etc. are valid, but not 10:15). See the technical documentation for full details.

</details>

<details>
<summary><b>Search available classrooms by time period / features</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/classrooms?start=2029-01-01T10:00:00&finish=2029-01-01T18:00:00&seats=10&projector=true&speakers=false'
```

Replace `start` and `finish` with actual future dates in ISO-8601 format (`yyyy-MM-ddTHH:mm:ss`) before running the request. This endpoint is public and doesn't require a token.

`seats`, `projector` and `speakers` must all be present in the request (they cannot be omitted), but that does not mean you have to filter by them: set `seats=0`, `projector=false` and `speakers=false` to ignore that criterion and include all classrooms regardless of capacity or features.

Same criteria about `start` and `finish` as in the previous endpoint applies.
</details>


<details>
<summary><b>Create a watch alert</b></summary>

```bash
curl --location --request POST 'http://localhost:9000/classroom-booking/watch-alerts/1' \
--header 'Authorization: Bearer <VALID_JWT>'
```

`idBooking` (with the value of 1 in this example: ...watch-alerts/**1**...) must reference an existing ACTIVE booking.
</details>

<details>
<summary><b>Get watch alerts by user and time period</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/watch-alerts?startSearch=2026-01-01T00:00:00&finishSearch=2030-01-01T00:00:00 \
--header 'Authorization: Bearer <VALID_JWT>''
```

Returns the watch alerts belonging to the authenticated user (resolved from the JWT, no need to pass any user information as a parameter) that fall within the given `startSearch`–`finishSearch` period.
</details>

<details>
<summary><b>Find booking by classroom/time slot</b> <i>(supporting endpoint)</i></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/watch-alerts/1/target-booking?start=2029-01-01T10:00:00&finish=2029-01-01T10:30:00'
```

Replace '.../watch-alerts/**1**/...' with any valid idClassroom.

This endpoint is not meant to be called directly (so it is included here just for completeness) — it is used internally by the front-end when a user clicks an already-booked slot to request the ID of the booking that falls in that time slot. This ID will be used in the next step to create a watch alert on that booking. 

The reason why there is an independent endpoint for this is that the whole process is resolved in two stages: 1) First, the front-end obtains the ID for the booking, and asks the user for confirmation. 2) If the user confirms that he wants to create a watch alert on that booking, it uses the ID obtained in the first step to do so.

'start' and 'finish' have to coincide with a valid slot, that is, for a slot duration of 30 minutes, e.g. 10:30 to 11:00, but not 10:15 to 10:45, or 10:00 to 11:00.

</details>

## Swagger

Interactive API documentation is available via Swagger UI:

[http://localhost:9000/classroom-booking/swagger-ui/index.html](http://localhost:9000/classroom-booking/swagger-ui/index.html)

This allows you to explore and try out all endpoints directly from the browser, without needing `curl`.

## Contribution and License
### Contributing
As this project is intended as a personal demo, external contributions are not being accepted at this time.

### License
This project is licensed under the MIT License.  
See the [LICENSE](./LICENSE) file for details.

## Contact
Created by Jorge Casas López.  
Email: [j.casas.lopez.26@gmail.com](mailto:j.casas.lopez.26@gmail.com) 

