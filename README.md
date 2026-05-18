# SkyBookerApp — Airline Ticket Booking System (Backend)

## About

SkyBooker is a full-stack Airline Ticket Booking System inspired by MakeMyTrip and GoIbibo. It connects passengers with airlines, enabling them to search for one-way and round-trip flights, compare fare classes, select seats on an interactive seat map, enter passenger details, and complete the booking with secure online payment via **Razorpay** — all from a unified platform.

The backend is architected as **10 independently deployable Spring Boot microservices** backed by MySQL, integrated through a Spring Cloud API Gateway with JWT-based stateless security, and event-driven via Apache Kafka.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│  Angular Frontend (localhost:4200)                                    │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   API Gateway   │ :8080
                    │  (Spring Cloud) │
                    │  CORS + Routing │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │         Eureka              │ :8761
              │    Service Registry         │
              └──────────────┬──────────────┘
                             │
    ┌────────────────────────┼────────────────────────────┐
    │          │          │          │         │          │
  Auth    Airline    Flight    Booking   Passenger   Seat
 :8081    :8082     :8083     :8084     :8085       :8086
    │                   │          │
    │                   │          │
  Payment          Notification   │
  :8087            :8088          │
    │                │            │
    └────────────────┘            │
           Kafka ──── payment-success / flight-status-changed
```

---

## Microservices Summary

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| service-registry | 8761 | — | Eureka Service Discovery |
| api-gateway | 8080 | — | Spring Cloud Gateway — CORS, JWT validation, routing |
| auth-service | 8081 | skybooker_auth_db | User registration, login, JWT, profile, password reset |
| airline-service | 8082 | skybooker_airline_db | Airlines & airports master data |
| flight-service | 8083 | skybooker_flight_db | Flight schedules, search, status + Kafka producer |
| booking-service | 8084 | skybooker_booking_db | Booking lifecycle, PNR, ancillary add-ons, check-in reminder support |
| passenger-service | 8085 | skybooker_passenger_db | Passenger details, seat assignment, ticket generation |
| seat-service | 8086 | skybooker_seat_db | Seat map, hold/release/confirm (optimistic locking) |
| payment-service | 8087 | skybooker_payment_db | Razorpay integration, refunds, Kafka publisher |
| notification-service | 8088 | skybooker_notification_db | Email, in-app alerts, Kafka consumer, schedulers |

---

## Prerequisites

- **Java 17+**
- **MySQL 8.x** — running locally on port `3306`
- **Redis 7.x** — required for token blacklist, OTP storage, caching, and rate limiting
- **Apache Kafka** — required for event-driven notifications (optional for basic booking flow)
- **Docker** — recommended for running Kafka/Zookeeper/Redis (see below)

---

## Quick Start

### 1. Start MySQL

Ensure MySQL is running on `localhost:3306`. All databases are auto-created via `createDatabaseIfNotExist=true` in each service's `application.yml`.

### 2. Start Kafka (Docker)

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** (port `2181`) and **Kafka** (port `9092`) — event streaming
- **Redis** (port `6379`) — in-memory cache, token store, and rate limiting

Kafka is required for:
- `payment-success` topic — triggers booking confirmation emails
- `flight-status-changed` topic — triggers flight delay/cancellation alerts

> **Note:** Services are configured to gracefully handle Kafka being unavailable. The payment flow still works — Kafka events are logged as warnings and skipped.

### 3. Start Services (in order)

1. `service-registry` (Eureka — must start first)
2. `api-gateway`
3. All other microservices (any order)

Each service can be started via:
```bash
cd <service-directory>
./mvnw spring-boot:run
```

---

## Razorpay Integration (Test Mode)

The payment service uses **Razorpay** for processing payments. The following **Test Mode** credentials are preconfigured:

| Key | Value |
|-----|-------|
| **Key ID** | `rzp_test_Semh94IyjyUsxQ` |
| **Key Secret** | `3qqAI6Uzei7OfMH3P9Q2Am8H` |
| **Currency** | `INR` |

### Razorpay Test Card for Payments

| Field | Value |
|-------|-------|
| Card Number | `4111 1111 1111 1111` |
| Expiry | Any future date (e.g. `12/30`) |
| CVV | Any 3 digits (e.g. `123`) |
| OTP | `1234` (Razorpay test OTP) |

### Payment Flow

1. Frontend initiates payment → backend creates a Razorpay order
2. Razorpay checkout modal opens in the browser
3. User completes payment with test card
4. Frontend sends `razorpayOrderId`, `razorpayPaymentId`, and `razorpaySignature` to the `/verify` endpoint
5. Backend verifies HMAC-SHA256 signature, marks payment as `PAID`, confirms the booking, and publishes a Kafka event

> These credentials are configured in `payment-service/src/main/resources/application.yml` under the `razorpay:` section.

---

## Redis Integration

Redis (`localhost:6379`) is used across multiple microservices as an in-memory data store. It is started automatically via `docker-compose up -d`.

### Redis Usage by Service

| Service | Redis Purpose | Key Pattern | TTL |
|---------|--------------|-------------|-----|
| **auth-service** | JWT token blacklist | `blacklist:{token}` | Matches JWT remaining expiry |
| **auth-service** | OTP storage for password reset | `otp:{email}` | 30 min (configurable) |
| **auth-service** | Reset token storage | `reset:{uuid}` | 30 min (configurable) |
| **flight-service** | Flight search results cache | `flightSearch::{key}` | 5 min |
| **seat-service** | Seat hold TTL tracking | `seat-hold:{seatId}` | 15 min |
| **notification-service** | Unread notification counter | `unread:{userId}` | No expiry (managed by app) |
| **api-gateway** | Rate limiting (auth endpoints) | `request_rate_limiter.{...}` | Sliding window |

### Demonstrating Redis (Redis CLI)

Connect to the Redis container:
```bash
docker exec -it skybooker-redis redis-cli
```

Useful commands for presentation:
```bash
# View all stored keys
KEYS *

# After logout — see blacklisted JWT token
KEYS blacklist:*
TTL blacklist:<token>

# After forgot-password — see OTP
GET otp:user@test.com
TTL otp:user@test.com

# After holding a seat — see TTL countdown
KEYS seat-hold:*
TTL seat-hold:<seatId>

# Notification unread count
GET unread:<userId>

# Flight search cache
KEYS flightSearch*

# Rate limiter counters
KEYS request_rate_limiter*

# Live stream of all Redis commands in real-time
MONITOR
```

---

## Auth Service

Manages user identity, authentication, and authorization across the platform. Issues JWT tokens on login, maintains a token blacklist for logout, supports password reset via email.

### Features

- **User Registration** — Register with email, password, phone, and role selection (`PASSENGER` or `AIRLINE_STAFF`). Admin self-registration is blocked.
- **User Login** — Secure login returning `accessToken` and `refreshToken`
- **JWT Authentication** — Stateless auth with `userId`, `email`, and `role` claims
- **Token Refresh** — Issue new access token via valid refresh token
- **Logout** — Invalidates JWT via in-memory blacklist
- **Profile Management** — `GET /me`, `PUT /me` (full update), `PATCH /me` (partial update)
- **Password Management** — Change password (authenticated), forgot password (email reset token), reset password
- **Account Deactivation** — Self-deactivation via `DELETE /me`
- **Role-Based Access Control** — Roles: `PASSENGER`, `AIRLINE_STAFF`, `ADMIN`
- **Admin User Management** — List all users, filter by role
- **Security** — BCrypt password hashing, CORS configured for `localhost:4200`
- **Database** — MySQL (`skybooker_auth_db`), tables: `users`, `password_reset_tokens`, `token_blacklist`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | PUBLIC | Register (PASSENGER or AIRLINE_STAFF only) |
| POST | `/api/v1/auth/login` | PUBLIC | Login and receive JWT tokens |
| POST | `/api/v1/auth/logout` | Authenticated | Invalidate current JWT |
| POST | `/api/v1/auth/refresh` | PUBLIC | Get new access token via refresh token |
| GET | `/api/v1/auth/validate` | PUBLIC | Validate a JWT token |
| GET | `/api/v1/auth/me` | Authenticated | Get current user profile |
| PUT | `/api/v1/auth/me` | Authenticated | Full profile update |
| PATCH | `/api/v1/auth/me` | Authenticated | Partial profile update |
| PUT | `/api/v1/auth/me/change-password` | Authenticated | Change password |
| DELETE | `/api/v1/auth/me` | Authenticated | Deactivate account |
| POST | `/api/v1/auth/forgot-password` | PUBLIC | Request OTP for password reset |
| POST | `/api/v1/auth/verify-otp` | PUBLIC | Verify 6-digit OTP — returns reset token |
| POST | `/api/v1/auth/reset-password` | PUBLIC | Reset password via token from OTP verification |
| GET | `/api/v1/auth/admin/users` | ADMIN | List all users |
| GET | `/api/v1/auth/admin/users/role/{role}` | ADMIN | List users by role |

---

## Airline Service

Manages airline and airport master data. Stores IATA/ICAO codes, airline profiles, and airport details including GPS coordinates and timezone. Powers the frontend airport autocomplete search.

### Features

- **Airline Management** — Create, update, activate, and deactivate airline profiles
- **Airport Management** — Create and manage airport profiles with IATA/ICAO, city, country, GPS, timezone
- **Airport Autocomplete Search** — Keyword search across name, city, and IATA code
- **Airline Retrieval** — Fetch by ID or IATA code; list all or active-only airlines
- **Airport Retrieval** — Fetch by ID, IATA, city, or country
- **Role-Based Access Control** — `ADMIN` has full CRUD; `AIRLINE_STAFF` and guests have read-only access
- **Database** — MySQL (`skybooker_airline_db`), tables: `airlines`, `airports`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/airlines` | ADMIN | Create a new airline |
| GET | `/api/v1/airlines` | PUBLIC | List all airlines |
| GET | `/api/v1/airlines/active` | PUBLIC | List active airlines only |
| GET | `/api/v1/airlines/{airlineId}` | PUBLIC | Get airline by ID |
| GET | `/api/v1/airlines/iata/{iataCode}` | PUBLIC | Get airline by IATA code |
| PUT | `/api/v1/airlines/{airlineId}` | ADMIN | Update airline details |
| PATCH | `/api/v1/airlines/{airlineId}/activate` | ADMIN | Activate an airline |
| PATCH | `/api/v1/airlines/{airlineId}/deactivate` | ADMIN | Deactivate an airline |
| POST | `/api/v1/airports` | ADMIN | Create a new airport |
| GET | `/api/v1/airports` | PUBLIC | List all airports |
| GET | `/api/v1/airports/{airportId}` | PUBLIC | Get airport by ID |
| GET | `/api/v1/airports/iata/{iataCode}` | PUBLIC | Get airport by IATA code |
| GET | `/api/v1/airports/city/{city}` | PUBLIC | Filter airports by city |
| GET | `/api/v1/airports/country/{country}` | PUBLIC | Filter airports by country |
| GET | `/api/v1/airports/search?keyword=` | PUBLIC | Autocomplete airport search |
| PUT | `/api/v1/airports/{airportId}` | ADMIN | Update airport details |

---

## Flight Service

Manages flight schedules and inventory. Supports one-way and round-trip search, real-time status updates, and publishes a Kafka `flight-status-changed` event whenever a flight's status changes.

### Features

- **Flight Schedule Management** — Create, update, and delete flights
- **One-Way Flight Search** — Search by origin, destination, and departure date
- **Round-Trip Flight Search** — Single call returning both outbound and return legs
- **Real-Time Flight Status** — `ON_TIME`, `DELAYED`, `CANCELLED`, `DEPARTED`, `ARRIVED`
- **Kafka Producer** — Publishes `FlightStatusChangedEvent` to `flight-status-changed` topic
- **Seat Inventory Tracking** — Maintains `totalSeats` and `availableSeats` per flight
- **Airline-wise Retrieval** — Fetch all flights for a specific airline
- **Database** — MySQL (`skybooker_flight_db`), tables: `flights`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/flights` | AIRLINE_STAFF / ADMIN | Create a new flight |
| GET | `/api/v1/flights/{id}` | PUBLIC | Get flight by ID |
| GET | `/api/v1/flights/airline/{airlineId}` | PUBLIC | List flights by airline |
| GET | `/api/v1/flights/search?origin=&destination=&date=` | PUBLIC | One-way flight search |
| GET | `/api/v1/flights/search/round-trip?origin=&destination=&departureDate=&returnDate=` | PUBLIC | Round-trip flight search |
| PUT | `/api/v1/flights/{id}` | AIRLINE_STAFF / ADMIN | Update flight details |
| PUT | `/api/v1/flights/{id}/status?status=` | AIRLINE_STAFF / ADMIN | Update flight status (triggers Kafka event) |
| DELETE | `/api/v1/flights/{id}` | AIRLINE_STAFF / ADMIN | Delete a flight |

---

## Booking Service

Central orchestration service managing the complete booking lifecycle. Generates unique PNR codes, stores fare breakdowns, and manages ancillary add-ons.

### Features

- **Booking Creation** — Linked to `userId` and `flightId`, auto-generates 6-character PNR code
- **Booking Lifecycle** — `PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED`
- **PNR-Based Lookup** — Public endpoint for retrieving bookings by PNR
- **My Bookings Dashboard** — All bookings and upcoming bookings for a user
- **Ancillary Add-Ons** — Meal preference (`VEG`, `NON_VEG`, `JAIN`, `VEGAN`) and extra luggage
- **Fare Storage** — `baseFare`, `taxes`, and `totalFare` with `ONE_WAY` and `ROUND_TRIP` support
- **Internal Scheduler Endpoint** — Used by notification-service for check-in reminders
- **Database** — MySQL (`skybooker_booking_db`), tables: `bookings`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/bookings` | PASSENGER | Create a new booking |
| GET | `/api/v1/bookings/{id}` | PASSENGER / ADMIN | Get booking by ID |
| GET | `/api/v1/bookings/pnr/{pnr}` | PUBLIC | Retrieve booking by PNR code |
| GET | `/api/v1/bookings/user/{userId}` | PASSENGER / ADMIN | All bookings for a user |
| GET | `/api/v1/bookings/user/{userId}/upcoming` | PASSENGER / ADMIN | Upcoming bookings |
| GET | `/api/v1/bookings/flight/{flightId}` | AIRLINE_STAFF / ADMIN | All bookings for a flight |
| PUT | `/api/v1/bookings/{id}/cancel` | PASSENGER / ADMIN | Cancel a booking |
| PUT | `/api/v1/bookings/{id}/confirm` | Authenticated (internal) | Confirm after payment |
| POST | `/api/v1/bookings/{id}/addon` | PASSENGER / ADMIN | Add meal/baggage add-on |
| GET | `/api/v1/bookings/internal/departing?from=&to=` | PUBLIC (internal) | Departing bookings for scheduler |

---

## Passenger Service

Manages traveller information for each booking. Stores passport details, generates ticket numbers, handles seat assignment, and supports multi-passenger bookings.

### Features

- **Passenger Management** — Add passengers with title, name, DOB, gender, passport, nationality
- **Ticket Number Generation** — Auto-generates unique ticket number per passenger
- **Passenger Types** — `ADULT`, `CHILD`, `INFANT`
- **Seat Assignment** — Link passenger to a specific seat after booking
- **CRUD Operations** — Create, read, update, delete passengers
- **Passenger Count** — Count passengers per booking
- **Database** — MySQL (`skybooker_passenger_db`), tables: `passenger_info`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/passengers` | PASSENGER | Add a passenger to a booking |
| GET | `/api/v1/passengers/{id}` | PASSENGER / AIRLINE_STAFF / ADMIN | Get passenger by ID |
| GET | `/api/v1/passengers/booking/{bookingId}` | PASSENGER / AIRLINE_STAFF / ADMIN | All passengers for a booking |
| PUT | `/api/v1/passengers/{id}` | PASSENGER / ADMIN | Update passenger details |
| PUT | `/api/v1/passengers/{id}/assign-seat` | PASSENGER / ADMIN | Assign seat to passenger |
| DELETE | `/api/v1/passengers/{id}` | PASSENGER / ADMIN | Delete passenger record |
| GET | `/api/v1/passengers/count/{bookingId}` | AIRLINE_STAFF / ADMIN | Count passengers for a booking |

---

## Seat Service

Manages seat inventory and interactive seat map per flight. Enforces a 15-minute seat hold lifecycle using optimistic locking and runs a scheduler to auto-release expired holds.

### Features

- **Seat Map Configuration** — Add seats in bulk with class, row, column, window/aisle flags, extra legroom, and price multiplier
- **Interactive Seat Map** — Full seat map showing `AVAILABLE`, `HELD`, `CONFIRMED`, `BLOCKED` status
- **Seat Hold Lifecycle** — `AVAILABLE` → `HELD` (15-min lock) → `CONFIRMED` or auto-released
- **Seat Hold Expiry Scheduler** — Runs every 2 minutes, releases expired holds
- **Class-Based Filtering** — `ECONOMY`, `BUSINESS`, `FIRST`
- **Seat Class Pricing** — `priceMultiplier` per seat (e.g., 1.0 Economy, 1.5 Business, 2.0 First)
- **Database** — MySQL (`skybooker_seat_db`), tables: `seats`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/seats` | AIRLINE_STAFF / ADMIN | Add seats in bulk |
| GET | `/api/v1/seats/{seatId}` | Authenticated | Get seat by ID |
| GET | `/api/v1/seats/flight/{flightId}/available` | Authenticated | Available seats for a flight |
| GET | `/api/v1/seats/flight/{flightId}/class/{seatClass}` | Authenticated | Available seats by class |
| GET | `/api/v1/seats/flight/{flightId}/map` | Authenticated | Full seat map |
| GET | `/api/v1/seats/flight/{flightId}/count?seatClass=` | Authenticated | Count available by class |
| PUT | `/api/v1/seats/hold` | PASSENGER / AIRLINE_STAFF / ADMIN | Hold a seat (15-min lock) |
| PUT | `/api/v1/seats/{seatId}/release` | PASSENGER / AIRLINE_STAFF / ADMIN | Release a held seat |
| PUT | `/api/v1/seats/{seatId}/confirm` | PASSENGER / AIRLINE_STAFF / ADMIN | Confirm a held seat |
| PUT | `/api/v1/seats/{seatId}` | AIRLINE_STAFF / ADMIN | Update seat details |
| DELETE | `/api/v1/seats/flight/{flightId}` | AIRLINE_STAFF / ADMIN | Delete all seats for a flight |
| DELETE | `/api/v1/seats/flight/{flightId}/class/{seatClass}` | AIRLINE_STAFF / ADMIN | Delete seats by class for a flight |

---

## Payment Service

Handles all financial transactions via **Razorpay** payment gateway. Creates orders, verifies webhook callbacks with HMAC-SHA256, processes refunds, and publishes payment events to Kafka.

### Features

- **Payment Initiation** — Creates a Razorpay order, returns `razorpayOrderId` + `razorpayKeyId` to frontend
- **Payment Verification** — Validates signature, transitions to `PAID`, confirms booking, publishes Kafka event
- **Refund Processing** — Full and partial refunds via Razorpay API
- **Payment Retrieval** — By payment ID, booking ID, or user ID
- **Revenue Aggregation** — Total revenue across all `PAID` transactions (Admin)
- **Payment Lifecycle** — `PENDING` → `PAID` → (optionally) `REFUNDED` / `PARTIALLY_REFUNDED`
- **Kafka Producer** — Publishes `PaymentSuccessEvent` to `payment-success` topic
- **Database** — MySQL (`skybooker_payment_db`), tables: `payments`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/payments/initiate` | PASSENGER | Create Razorpay order for a booking |
| POST | `/api/v1/payments/verify` | PASSENGER | Verify payment signature and confirm booking |
| GET | `/api/v1/payments/{paymentId}` | PASSENGER / ADMIN | Get payment by ID |
| GET | `/api/v1/payments/booking/{bookingId}` | PASSENGER / ADMIN | Get payment by booking |
| GET | `/api/v1/payments/user/{userId}` | PASSENGER / ADMIN | Get all payments for a user |
| POST | `/api/v1/payments/refund` | PASSENGER / ADMIN | Initiate full or partial refund |
| GET | `/api/v1/payments/admin/revenue` | ADMIN | Get total platform revenue |
| GET | `/api/v1/payments/admin/revenue/monthly?year=` | ADMIN | Get monthly revenue breakdown |

---

## Notification Service

Multi-channel alert hub. Listens to Kafka events, dispatches transactional emails via **Gmail SMTP (JavaMailSender)**, stores in-app notifications with read/unread state, and runs a check-in reminder scheduler.

### Features

- **Kafka Event-Driven** — Listens to `payment-success` and `flight-status-changed` topics
- **Booking Confirmation** — Rich HTML email with PNR, flight number, route, amount, and transaction ID
- **Flight Status Alerts** — In-app alerts for delayed/cancelled flights
- **Check-In Reminder Scheduler** — Runs hourly, sends reminders for flights departing in 23–25 hours
- **In-App Notification Centre** — Persistent storage, read/unread toggle, unread count badge
- **Admin Broadcast** — Send platform-wide notifications with optional role-based targeting
- **Notification Types** — `BOOKING_CONFIRMED`, `PAYMENT_SUCCESS`, `FLIGHT_DELAY`, `FLIGHT_CANCELLATION`, `GATE_CHANGE`, `CHECKIN_REMINDER`, `BOARDING`, `GENERAL`
- **Channels** — `APP` (in-app), `EMAIL` (Gmail SMTP), `SMS` (placeholder)
- **Database** — MySQL (`skybooker_notification_db`), tables: `notifications`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/notifications/user/{userId}` | PASSENGER / AIRLINE_STAFF / ADMIN | All notifications for a user |
| GET | `/api/v1/notifications/user/{userId}/unread` | PASSENGER / AIRLINE_STAFF / ADMIN | Unread notifications |
| GET | `/api/v1/notifications/user/{userId}/unread/count` | PASSENGER / AIRLINE_STAFF / ADMIN | Unread count |
| PUT | `/api/v1/notifications/{id}/read` | PASSENGER / AIRLINE_STAFF / ADMIN | Mark as read |
| PUT | `/api/v1/notifications/user/{userId}/read-all` | PASSENGER / AIRLINE_STAFF / ADMIN | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | PASSENGER / ADMIN | Delete a notification |
| POST | `/api/v1/notifications/admin/broadcast` | ADMIN | Send broadcast notification |

---

## API Documentation

Each microservice exposes Swagger/OpenAPI documentation:

| Service | Swagger URL |
|---------|-------------|
| auth-service | `http://localhost:8081/swagger-ui.html` |
| airline-service | `http://localhost:8082/swagger-ui.html` |
| flight-service | `http://localhost:8083/swagger-ui.html` |
| booking-service | `http://localhost:8084/swagger-ui.html` |
| passenger-service | `http://localhost:8085/swagger-ui.html` |
| seat-service | `http://localhost:8086/swagger-ui.html` |
| payment-service | `http://localhost:8087/swagger-ui.html` |
| notification-service | `http://localhost:8088/swagger-ui.html` |

---

## Creating an Admin User

Admin self-registration is blocked at the API level. To create an admin user for testing:

1. Register a normal user via the API or frontend
2. Directly update the role in the database:

```sql
USE skybooker_auth_db;
UPDATE users SET role = 'ADMIN' WHERE email = 'your-admin@email.com';
```

3. Log in with the updated account — you will have full admin access

---

## Docker Compose

The `docker-compose.yml` provides Kafka and Zookeeper for local development:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    ports: ["2181:2181"]
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
```

Start with: `docker-compose up -d`

---

## Testing

The project includes **~131 JUnit 5 + Mockito unit tests** across all business logic services. Tests are fast and isolated — no Spring context is loaded (`@ExtendWith(MockitoExtension.class)`).

### Test Files Summary

| Service | Test Class | Tests | Coverage Focus |
|---------|-----------|-------|----------------|
| auth-service | `AuthServiceImplTest` | 30 | Register, login, logout, token refresh, profile CRUD, password change/reset, OTP verification, deactivation |
| auth-service | `TokenBlacklistServiceImplTest` | 4 | Blacklist, check status, cleanup expired tokens |
| airline-service | `AirlineServiceImplTest` | 10 | CRUD, activate/deactivate, duplicate IATA validation |
| airline-service | `AirportServiceImplTest` | 10 | CRUD, search by keyword/city/country, delete |
| booking-service | `BookingServiceImplTest` | 13 | Create (PNR generation), cancel, confirm (idempotent), add-ons, role-based access control |
| flight-service | `FlightServiceImplTest` | 12 | Add (auto-duration), search, round-trip, status update + Kafka, Kafka failure resilience |
| passenger-service | `PassengerServiceImplTest` | 12 | Add (ticket generation), update, seat assign, access control, count |
| payment-service | `PaymentServiceImplTest` | 8 | Get by ID/booking/user, refund validation, revenue queries |
| seat-service | `SeatServiceImplTest` | 18 | Hold/release/confirm lifecycle, optimistic locking, expired hold, bulk add, seat map |
| notification-service | `NotificationServiceImplTest` | 14 | Payment success events, flight status alerts, broadcast, read/unread, email failure handling |

### Running Tests

Run all tests for a specific service:
```bash
cd <service-directory>
./mvnw test
```

Run a specific test class:
```bash
cd auth-service
./mvnw test -Dtest=AuthServiceImplTest
```

### Test Design Principles

- **No Spring context** — Pure Mockito mocking for fast execution
- **SecurityContext mocking** — Booking and Passenger tests manually set `SecurityContextHolder` for role-based access testing
- **Kafka resilience** — Flight service tests verify graceful handling when Kafka is unavailable
- **`ReflectionTestUtils`** — Used in Payment tests to inject `@Value` fields without Spring context
- **Branch coverage** — Every method's happy path + all exception branches are tested (targeting >75% coverage)

---

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Cloud Gateway** — API Gateway with CORS and route configuration
- **Spring Cloud Netflix Eureka** — Service Discovery
- **Spring Security** — JWT-based stateless authentication
- **MySQL 8** — Relational persistence
- **Apache Kafka** — Event-driven architecture (`payment-success`, `flight-status-changed`)
- **Razorpay Java SDK** — Payment processing
- **JavaMailSender** — Transactional email via Gmail SMTP
- **SpringDoc OpenAPI** — Swagger documentation for all services
- **Lombok** — Boilerplate reduction
