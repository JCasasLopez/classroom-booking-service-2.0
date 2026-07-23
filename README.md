# Booking Microservice

Booking microservice (v2.0) manages classroom reservations and watch alerts, as part of the Classrooms application — a distributed, event-driven system built with a microservices architecture (see full [technical documentation](#) for details).

The entire Classrooms application is deployed and available at [www.book-your-classroom.com](https://www.book-your-classroom.com).

## Table of Contents
- [Purpose](#purpose)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Local Execution Prerequisites](#local-execution-prerequisites)
- [Deployment](#deployment)
- [Tests](#tests)
- [Limitations of deploying Booking on its own](#limitations-of-deploying-booking-on-its-own)
- [Exploring the Booking Microservice I: Authentication and Pre-loaded Data](#exploring-the-booking-microservice-i-authentication-and-pre-loaded-data)
- [Exploring the Booking Microservice II: Endpoints](#exploring-the-booking-microservice-ii-endpoints)
- [Swagger](#swagger)

## Purpose

The purpose of this repository is to allow exploring the microservice individually, as opposed to as a part of the entire application. The accompanying database and Kafka Docker containers come with some pre-loaded data - bookings, watch alerts and classrooms- for convenience (see the 'Authentication & Pre-loaded data' section).


## Features

- Makes classroom reservations *
- Cancels classroom reservations *
- Retrieves booking history by user * 
- Retrieves availability calendar by classroom
- Searches available classrooms by time period / features
- Creates watch alerts *
- Retrieves watch alert history by user and time period * 
- Generates a valid JWT: obviously in a production environment would compromise the system security, but as this is only a demo has been added to allow microservice exploration.

\* Endpoints marked with an asterisk require authentication (a valid JWT).


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

Note: this project depends on an internal shared library, which JitPack fetches automatically from the library's GitHub repository. MySQL and Kafka are also spun up via Docker (see `docker-compose.yml`).

## Deployment

```bash
git clone https://github.com/JCasasLopez/classroom-booking-service-2.0
```
```bash
cd classroom-booking-service-2.0
```
```bash
docker compose up --build
```

## Tests

Since the Booking microservice has a fairly complex business logic, the microservice functionalities are tested comprehensively, with both unit and integration tests. Integration tests use **Testcontainers**, so they spin up their own isolated MySQL and Kafka instances — no manual setup required, and they don't interfere with the `docker-compose.yml` environment used for manual exploration.

Tests already run automatically as part of the deployment described above (no `-DskipTests` flag is used). To run them independently:

```bash
mvn test
```

## Limitations of deploying Booking on its own

As already mentioned at the very beginning of this README, the deployment described above only allows working with the Booking microservice on its own — the rest of the Classrooms application is left out. Apart from the obvious inability to work with data from other microservices (Users or Classrooms), the main shortcoming of this approach is that the user will never receive the notifications sent by the system. This is due to two reasons: the email associated with the JWT is a made-up address, and the Notification microservice — which reads the Kafka topic and actually sends the notifications — is not spun up.

To explore the notifications, you can check the `notifications` Kafka topic (obviously, the topic is empty before you perform any action that trigger a notification, which are creating a booking, cancelling a booking, and creating a watch alert):

```bash
docker exec -it kafka-broker bash
```

```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic notifications --from-beginning
```

## Exploring the Booking Microservice I: Authentication and Pre-loaded Data
As mentioned in the "Features" section, all endpoints except the search-related ones are protected, and an endpoint to generate a valid JWT is provided.

By default, the JWT is built with `idUser=1`, which corresponds to the user associated with the pre-loaded bookings in the database container. Using this default token gives you access to that pre-loaded data out of the box.

If you'd rather start from a clean slate, generate a token with a different `idUser` (see "Endpoints Exploration > Generate a valid JWT"). Bookings and watch alerts created with that token won't have any pre-loaded data associated with them.

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

Or to build the token with a different user ID:
```bash
curl --location 'http://localhost:9000/classroom-booking/generate-token?idUser=<OTHER_ID_USER>'
```

This token must be used in the `Authorization: Bearer <VALID_JWT>` header for all other endpoints below. 

</details>

<details>  
<summary><b>Create a booking</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/bookings' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <VALID_JWT>' \
--data '{
  "idUser": 1,
  "idClassroom": 1,
  "startTimeSlotList": [
    "<FUTURE_DATE e.g. 2027-07-27T10:00:00>",
    "<FUTURE_DATE e.g. 2027-07-27T10:30:00>"
  ]
}'
```

Note that each entry in `startTimeSlotList` marks the start of a 30-minute slot (or whatever time slot duration is set to. Default value is 30 minutes). See full [technical documentation](#) for details. In this example, two consecutive slots are booked, resulting in a 10:00–11:00 reservation.

</details>

<details>
<summary><b>Cancel a booking</b></summary>

```bash
curl --location --request PATCH 'http://localhost:9000/classroom-booking/bookings/cancel?idBooking=1' \
--header 'Authorization: Bearer <VALID_JWT>' \
--data ''
```

Only a booking with ACTIVE status can be cancelled.

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
<summary><b>Retrieve availability calendar by classroom</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/availability-calendar?start=<FUTURE_DATE e.g. 2027-07-27T10:00:00>&finish=<FUTURE_DATE e.g. 2027-07-27T18:00:00>&idClassroom=1'
```

Replace `start` and `finish` with actual future dates in ISO-8601 format (`yyyy-MM-ddTHH:mm:ss`) before running the request. This endpoint is public and doesn't require a token.

Keep in mind that `start` and `finish` must be on the same day, on a day the classroom is open (default: Monday to Friday, 9:00–22:00; closed on weekends), and must be valid time slots (default slot duration: 30 minutes, so 10:00, 10:30, etc. are valid, but not 10:15). See the technical documentation for full details.

</details>

<details>
<summary><b>Search available classrooms by time period / features</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/searches/classrooms-available?start=<FUTURE_DATE e.g. 2027-07-27T10:00:00>&finish=<FUTURE_DATE e.g. 2027-07-27T11:00:00>&seats=10&projector=true&speakers=false'
```

Replace `start` and `finish` with actual future dates in ISO-8601 format (`yyyy-MM-ddTHH:mm:ss`) before running the request. This endpoint is public and doesn't require a token.

`seats`, `projector` and `speakers` must all be present in the request (they can't be omitted), but that doesn't mean you have to filter by them: set `seats=0`, `projector=false` and `speakers=false` to ignore that criterion and include all classrooms regardless of capacity or features.

Same criteria about `start` and `finish` as in the previous endpoint applies.
</details>

<details>
<summary><b>Find booking by classroom/time slot</b> <i>(supporting endpoint)</i></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/searches/booking-by-slot?start=<FUTURE_DATE e.g. 2027-07-27T10:00:00>&finish=<FUTURE_DATE e.g. 2027-07-27T11:00:00>&idClassroom=1'
```

This endpoint is not meant to be called directly — it's used internally by the front-end when a user clicks an already-booked slot to request the ID of the booking that falls in that time slot. Since creating a watch alert requires an `idBooking` (not a time slot), this endpoint resolves the clicked classroom + time period into the corresponding `idBooking`. Included here for completeness.

</details>

<details>
<summary><b>Create a watch alert</b></summary>

```bash
curl --location --request POST 'http://localhost:9000/classroom-booking/watch-alerts?idBooking=1' \
--header 'Authorization: Bearer <VALID_JWT>'
```

`idBooking` must reference an existing ACTIVE booking.
</details>

<details>
<summary><b>Get watch alerts by user and time period</b></summary>

```bash
curl --location 'http://localhost:9000/classroom-booking/watch-alerts?startSearch=2026-01-01T00:00:00&finishSearch=2030-01-01T00:00:00 \
--header 'Authorization: Bearer <VALID_JWT>''
```

Returns the watch alerts belonging to the authenticated user (resolved from the JWT, no need to pass any user information as a parameter) that fall within the given `startSearch`–`finishSearch` period.
</details>

## Swagger

Interactive API documentation is available via Swagger UI:

`http://localhost:9000/swagger-ui/index.html`

This allows you to explore and try out all endpoints directly from the browser, without needing `curl`.
