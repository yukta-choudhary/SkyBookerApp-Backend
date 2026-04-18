# SkyBookerApp — Airline Ticket Booking System

## About

SkyBooker is a full-stack Airline Ticket Booking System inspired by MakeMyTrip and GoIbibo. It connects passengers with airlines, enabling them to search for one-way and round-trip flights, compare fare classes, select seats on an interactive seat map, enter passenger details, add ancillary services (meal preferences, extra baggage), and complete the booking with secure online payment — all from a unified platform.

The system is architected as **10 independently deployable microservices** backed by MySQL, integrated through a Spring Cloud API Gateway with JWT-based stateless security, and event-driven via Apache Kafka.

---

## Auth Service – SkyBooker

The **Auth Service** is responsible for managing user identity, authentication, and authorization across the entire SkyBooker platform. It issues JWT tokens on login, maintains a token blacklist for logout, supports password reset via email, and exposes Admin endpoints for user management.

### Features Implemented

- **User Registration**
  - Register with email and password
  - Default role: `PASSENGER` (can be set to `AIRLINE_STAFF` or `ADMIN`)
  - Unique email validation with structured error response

- **User Login**
  - Secure login using email + password
  - Returns `accessToken` and `refreshToken` on success

- **JWT-Based Authentication**
  - Stateless authentication using JSON Web Tokens
  - Token validated on every protected API request via `JwtAuthFilter`
  - Includes `userId`, `email`, and `role` claims

- **Token Refresh**
  - Issues a new access token using a valid refresh token

- **Logout**
  - Invalidates the current JWT via an in-memory token blacklist

- **Profile Management**
  - `GET /me` — fetch current user's profile
  - `PUT /me` — full profile update (name, phone, passport, nationality)
  - `PATCH /me` — partial profile update

- **Password Management**
  - Change password (authenticated)
  - Forgot password — generates a secure reset token sent via email
  - Reset password — validates token and sets new password

- **Account Deactivation**
  - Self-deactivation via `DELETE /me`

- **Role-Based Access Control**
  - Roles: `PASSENGER`, `AIRLINE_STAFF`, `ADMIN`
  - Method-level security via `@PreAuthorize`

- **Admin User Management**
  - List all platform users
  - Filter users by role

- **Security**
  - Passwords hashed with BCrypt
  - JWT secret shared across services via `application.yml`
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `users`, `password_reset_tokens`, `token_blacklist`
  - Database: `skybooker_auth_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | PUBLIC | Register a new user |
| POST | `/api/v1/auth/login` | PUBLIC | Login and receive JWT tokens |
| POST | `/api/v1/auth/logout` | Authenticated | Invalidate current JWT |
| POST | `/api/v1/auth/refresh` | PUBLIC | Get new access token via refresh token |
| GET | `/api/v1/auth/validate` | PUBLIC | Validate a JWT token |
| GET | `/api/v1/auth/me` | Authenticated | Get current user profile |
| PUT | `/api/v1/auth/me` | Authenticated | Full profile update |
| PATCH | `/api/v1/auth/me` | Authenticated | Partial profile update |
| PUT | `/api/v1/auth/me/change-password` | Authenticated | Change password |
| DELETE | `/api/v1/auth/me` | Authenticated | Deactivate account |
| POST | `/api/v1/auth/forgot-password` | PUBLIC | Request password reset email |
| POST | `/api/v1/auth/reset-password` | PUBLIC | Reset password via token |
| GET | `/api/v1/auth/admin/users` | ADMIN | List all users |
| GET | `/api/v1/auth/admin/users/role/{role}` | ADMIN | List users by role |

---

## Airline Service – SkyBooker

The **Airline Service** manages airline and airport master data for the SkyBooker platform. It stores IATA/ICAO codes, airline profiles, and airport details including GPS coordinates and timezone — all used by `flight-service` for scheduling and by the frontend for autocomplete search.

### Features Implemented

- **Airline Management**
  - Create, update, activate, and deactivate airline profiles
  - Unique IATA and ICAO code identification
  - Stores name, logo URL, country, contact email, and phone

- **Airport Management**
  - Create and manage airport profiles
  - Stores IATA/ICAO codes, city, country, GPS coordinates, and timezone

- **Airport Search & Autocomplete**
  - Keyword search across name, city, and IATA code
  - Powers the frontend flight search autocomplete field

- **Airline Retrieval**
  - Fetch by ID or IATA code
  - List all airlines or only active ones

- **Airport Retrieval**
  - Fetch by ID or IATA code
  - Filter by city or country

- **Role-Based Access Control**
  - `ADMIN` — full CRUD access
  - `AIRLINE_STAFF` — read-only
  - `PASSENGER` / Guest — public read access for search

- **Security**
  - JWT-based authentication for protected endpoints
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `airlines`, `airports`
  - Database: `skybooker_airline_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

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

## Flight Service – SkyBooker

The **Flight Service** manages flight schedules and inventory. It supports one-way and round-trip search, real-time status updates, and publishes a Kafka `flight-status-changed` event whenever a flight's status changes — automatically triggering passenger alerts via `notification-service`.

### Features Implemented

- **Flight Schedule Management**
  - Create new flights with number, airline, route, times, aircraft type, and pricing
  - Update flight details and schedules
  - Delete flights

- **One-Way Flight Search**
  - Search by origin airport code, destination airport code, and departure date
  - Returns all matching available flights

- **Round-Trip Flight Search**
  - Single call returns both outbound and return flight legs
  - Response: `{ outboundFlights: [...], returnFlights: [...] }`

- **Real-Time Flight Status**
  - Update status: `ON_TIME`, `DELAYED`, `CANCELLED`, `DEPARTED`, `ARRIVED`
  - Publishes `FlightStatusChangedEvent` to Kafka topic `flight-status-changed` on every status update
  - `notification-service` consumes the event and alerts all booked passengers

- **Seat Inventory Tracking**
  - Maintains `totalSeats` and `availableSeats` per flight
  - `availableSeats` initialized from `totalSeats` at flight creation

- **Airline-wise Retrieval**
  - Fetch all flights for a specific airline (used by Airline Staff dashboard)

- **Security**
  - JWT-based authentication for protected endpoints
  - `AIRLINE_STAFF` and `ADMIN` required for create/update/delete
  - Search and read endpoints are public

- **Kafka Integration**
  - Producer: publishes to `flight-status-changed` topic (consumed by notification-service)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `flights`
  - Database: `skybooker_flight_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/flights` | AIRLINE_STAFF / ADMIN | Create a new flight schedule |
| GET | `/api/v1/flights/{id}` | PUBLIC | Get flight by ID |
| GET | `/api/v1/flights/airline/{airlineId}` | PUBLIC | List flights by airline |
| GET | `/api/v1/flights/search?origin=&destination=&date=` | PUBLIC | One-way flight search |
| GET | `/api/v1/flights/search/round-trip?origin=&destination=&departureDate=&returnDate=` | PUBLIC | Round-trip flight search |
| PUT | `/api/v1/flights/{id}` | AIRLINE_STAFF / ADMIN | Update flight details |
| PUT | `/api/v1/flights/{id}/status?status=` | AIRLINE_STAFF / ADMIN | Update flight status (triggers Kafka event) |
| DELETE | `/api/v1/flights/{id}` | AIRLINE_STAFF / ADMIN | Delete a flight |

---

## Booking Service – SkyBooker

The **Booking Service** is the central orchestration service managing the complete booking lifecycle. It generates unique PNR codes, stores fare breakdowns, manages ancillary add-ons (meal/baggage), exposes an "Upcoming Bookings" dashboard query, and runs a scheduled **no-show detection** job every 30 minutes.

### Features Implemented

- **Booking Creation**
  - Linked to `userId` and `flightId`
  - Auto-generates a unique 6-character alphanumeric **PNR code**
  - Initializes status as `PENDING`; stores `departureTime` for scheduler queries

- **Booking Lifecycle**
  - Status transitions: `PENDING` → `CONFIRMED` → `COMPLETED` / `CANCELLED` / `NO_SHOW`
  - `CONFIRMED` by payment-service after successful payment
  - `CANCELLED` by passenger or admin

- **PNR-Based Lookup**
  - Public endpoint — retrieve any booking by PNR without authentication

- **My Bookings Dashboard**
  - `GET /user/{id}` — all bookings for a user
  - `GET /user/{id}/upcoming` — only PENDING/CONFIRMED bookings with future departure

- **Ancillary Add-Ons**
  - `POST /{id}/addon` — add/update meal preference (`VEG`, `NON_VEG`, `JAIN`, `VEGAN`)
  - Add extra luggage in kg; recalculates `totalFare` with `additionalCost`

- **Fare Storage**
  - Stores `baseFare`, `taxes`, and `totalFare` per booking
  - Supports `ONE_WAY` and `ROUND_TRIP` trip types

- **No-Show Detection Scheduler**
  - Runs every 30 minutes via `@Scheduled`
  - Marks `CONFIRMED` bookings to `NO_SHOW` when departure was > 1 hour ago

- **Internal Scheduler Endpoint**
  - `GET /internal/departing?from=&to=` — used by notification-service check-in reminder

- **Security**
  - JWT-based authentication; passengers can only view/modify their own bookings
  - PNR lookup and internal scheduler endpoint are public

- **Database Integration**
  - MySQL-based persistence
  - Tables: `bookings`
  - Database: `skybooker_booking_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/bookings` | PASSENGER | Create a new booking |
| GET | `/api/v1/bookings/{id}` | PASSENGER / ADMIN | Get booking by ID |
| GET | `/api/v1/bookings/pnr/{pnr}` | PUBLIC | Retrieve booking by PNR code |
| GET | `/api/v1/bookings/user/{userId}` | PASSENGER / ADMIN | All bookings for a user |
| GET | `/api/v1/bookings/user/{userId}/upcoming` | PASSENGER / ADMIN | Upcoming bookings (dashboard) |
| GET | `/api/v1/bookings/flight/{flightId}` | AIRLINE_STAFF / ADMIN | All bookings for a flight |
| PUT | `/api/v1/bookings/{id}/cancel` | PASSENGER / ADMIN | Cancel a booking |
| PUT | `/api/v1/bookings/{id}/confirm` | Authenticated (internal) | Confirm after payment |
| POST | `/api/v1/bookings/{id}/addon` | PASSENGER / ADMIN | Add meal preference or extra baggage |
| GET | `/api/v1/bookings/internal/departing?from=&to=` | PUBLIC (internal) | Departing bookings for scheduler |

---

## Passenger Service – SkyBooker

The **Passenger Service** manages detailed travel information for each individual traveller on a booking. It stores passport details, generates ticket numbers, handles seat assignment post-booking, and supports multi-passenger bookings (ADULT, CHILD, INFANT).

### Features Implemented

- **Passenger Management**
  - Add multiple passengers per booking
  - Stores title, first/last name, date of birth, gender, passport number, nationality, and passport expiry

- **Ticket Number Generation**
  - Auto-generates a unique ticket number for each passenger at creation

- **Passenger Types**
  - `ADULT`, `CHILD`, `INFANT` — enables age-based fare calculations

- **Seat Assignment**
  - `PUT /{id}/assign-seat` — links a passenger to a specific `seatId` and `seatNumber`
  - Used during web check-in flow

- **Passenger Retrieval**
  - Fetch by passenger ID
  - Fetch all passengers for a booking (used by Airline Staff for passenger manifest)
  - Fetch by passport number

- **Passenger Update**
  - Update all travel document and personal details

- **Passenger Deletion**
  - Remove individual passenger records

- **Passenger Count**
  - Count total passengers per booking

- **Role-Based Access Control**
  - `PASSENGER` — add/update/delete their own passengers
  - `AIRLINE_STAFF` — read passenger manifest for their flights
  - `ADMIN` — full access

- **Security**
  - JWT-based authentication for all endpoints
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `passenger_info`
  - Database: `skybooker_passenger_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

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

## Seat Service – SkyBooker

The **Seat Service** manages the seat inventory and interactive seat map for each flight. It enforces the 15-minute seat hold lifecycle using optimistic locking to prevent double-booking, supports class-based filtering (ECONOMY, BUSINESS, FIRST), and runs a scheduler to auto-release expired holds every 2 minutes.

### Features Implemented

- **Seat Map Configuration**
  - Add seats in bulk for a flight with class, row, column, window/aisle flags, extra legroom, and price multiplier
  - Delete all seats for a flight

- **Interactive Seat Map**
  - Fetch complete seat map for a flight showing all seats with their current status
  - Colour-coded status: `AVAILABLE`, `HELD`, `CONFIRMED`, `BLOCKED`

- **Seat Hold Lifecycle**
  - `AVAILABLE` → `HELD` (during 15-min payment window)
  - `HELD` → `CONFIRMED` (after successful payment)
  - `HELD` → `AVAILABLE` (auto-released after 15 minutes by scheduler)

- **Seat Hold Expiry Scheduler**
  - Runs every 2 minutes via `@Scheduled`
  - Releases any `HELD` seats older than 15 minutes back to `AVAILABLE`

- **Availability Queries**
  - All available seats for a flight
  - Filter available seats by class (`ECONOMY`, `BUSINESS`, `FIRST`)
  - Count available seats per class

- **Seat Update**
  - Update individual seat properties (class, features, price multiplier)

- **Seat Class Pricing**
  - Each seat has a `priceMultiplier` applied on top of the flight's `basePrice`
  - e.g., `1.0` for Economy, `1.5` for Business, `2.0` for First

- **Role-Based Access Control**
  - `AIRLINE_STAFF` / `ADMIN` — configure seat maps, add/delete seats
  - `PASSENGER` — hold, release seats; view availability and map

- **Security**
  - JWT-based authentication for all endpoints
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `seats`
  - Database: `skybooker_seat_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/seats` | AIRLINE_STAFF / ADMIN | Add seats in bulk for a flight |
| GET | `/api/v1/seats/{seatId}` | PASSENGER / AIRLINE_STAFF / ADMIN | Get seat by ID |
| GET | `/api/v1/seats/flight/{flightId}/available` | PASSENGER / AIRLINE_STAFF / ADMIN | All available seats for a flight |
| GET | `/api/v1/seats/flight/{flightId}/class/{seatClass}` | PASSENGER / AIRLINE_STAFF / ADMIN | Available seats by class |
| GET | `/api/v1/seats/flight/{flightId}/map` | PASSENGER / AIRLINE_STAFF / ADMIN | Full seat map for a flight |
| GET | `/api/v1/seats/flight/{flightId}/count?seatClass=` | PASSENGER / AIRLINE_STAFF / ADMIN | Count available seats by class |
| PUT | `/api/v1/seats/hold` | PASSENGER / AIRLINE_STAFF / ADMIN | Hold a seat (15-min lock) |
| PUT | `/api/v1/seats/{seatId}/release` | PASSENGER / AIRLINE_STAFF / ADMIN | Release a held seat |
| PUT | `/api/v1/seats/{seatId}/confirm` | PASSENGER / AIRLINE_STAFF / ADMIN | Confirm a held seat after payment |
| PUT | `/api/v1/seats/{seatId}` | AIRLINE_STAFF / ADMIN | Update seat details |
| DELETE | `/api/v1/seats/flight/{flightId}` | AIRLINE_STAFF / ADMIN | Delete all seats for a flight |

---

---

## Payment Service – SkyBooker

The **Payment Service** handles all financial transactions in the SkyBooker platform. It integrates with the **Razorpay** payment gateway to initiate orders, verify webhook callbacks, process refunds, and publish payment success events to downstream services via **Kafka**.

### Features Implemented

- **Payment Initiation**
  - Creates a Razorpay order for a booking
  - Returns `razorpayOrderId`, `amount`, and `currency` to the frontend checkout
  - Stores the payment record in `PENDING` status

- **Payment Verification (Webhook/Callback)**
  - Validates Razorpay signature using HMAC-SHA256 to prevent fraud
  - Transitions payment status `PENDING` → `PAID` on success
  - Calls `booking-service` to confirm the booking (`PENDING` → `CONFIRMED`)
  - Publishes a `PaymentSuccessEvent` to Kafka topic `payment-success`

- **Refund Processing**
  - Initiates a Razorpay refund for eligible cancelled bookings
  - Updates payment status to `REFUNDED` with refund amount and timestamp
  - Supports partial and full refunds

- **Payment Retrieval**
  - Fetch payment record by booking ID
  - Fetch all payments for a user
  - Check payment status by payment ID

- **Revenue Aggregation**
  - Aggregate total revenue across all PAID transactions (Admin use)

- **Payment Status Lifecycle**
  - `PENDING` → `PAID` → (optionally) `REFUNDED`
  - `PENDING` → `FAILED` (on gateway error)

- **Kafka Integration**
  - Publishes `PaymentSuccessEvent` to `payment-success` topic
  - Consumed by `notification-service` to trigger booking confirmation email and in-app alert

- **Security**
  - JWT-based authentication for all endpoints
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `payments`
  - Database: `skybooker_payment_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/payments/initiate` | PASSENGER | Create Razorpay order for a booking |
| POST | `/api/v1/payments/verify` | PASSENGER | Verify payment signature and confirm booking |
| POST | `/api/v1/payments/refund/{paymentId}` | PASSENGER / ADMIN | Initiate refund |
| GET | `/api/v1/payments/booking/{bookingId}` | PASSENGER / ADMIN | Get payment by booking |
| GET | `/api/v1/payments/user/{userId}` | PASSENGER / ADMIN | Get all payments for a user |
| GET | `/api/v1/payments/revenue` | ADMIN | Get total platform revenue |

---

## Notification Service – SkyBooker

The **Notification Service** is the multi-channel alert hub of the SkyBooker platform. It listens to Kafka events from `payment-service` and `flight-service`, dispatches transactional emails via **JavaMailSender (Gmail SMTP)**, stores in-app notifications with read/unread state, and runs a scheduled **check-in reminder** job every hour.

### Features Implemented

- **Kafka Event-Driven Notifications**
  - Listens to `payment-success` topic → sends booking confirmation email + in-app notification
  - Listens to `flight-status-changed` topic → sends flight delay / cancellation alerts to affected passengers

- **Booking Confirmation Notification**
  - Triggered automatically after successful payment via Kafka
  - Sends a rich HTML email with PNR code, flight number, route, amount paid, and transaction ID
  - Creates an in-app `BOOKING_CONFIRMED` notification with booking deep-link

- **Flight Status Alerts**
  - Triggered when airline staff marks a flight as DELAYED or CANCELLED
  - Sends in-app alerts to all booked passengers for the affected flight

- **Check-In Reminder Scheduler**
  - Runs every hour via `@Scheduled(cron = "0 0 * * * *")`
  - Calls `booking-service` to find confirmed bookings departing in 23–25 hours
  - Sends in-app + email check-in reminder notifications automatically

- **In-App Notification Centre**
  - Persistent storage of all notifications per user
  - Read / unread status with toggle support
  - Unread count badge for the frontend notification bell
  - Mark single or all notifications as read
  - Delete individual notifications

- **Admin Broadcast**
  - Send a platform-wide notification to a list of user IDs
  - Supports optional `targetRole` field for role-based targeting

- **Notification Types Supported**
  - `BOOKING_CONFIRMED`, `PAYMENT_SUCCESS`, `FLIGHT_DELAY`, `FLIGHT_CANCELLATION`, `GATE_CHANGE`, `CHECKIN_REMINDER`, `BOARDING`, `GENERAL`

- **Notification Channels**
  - `APP` — in-app notification centre
  - `EMAIL` — transactional HTML emails via Gmail SMTP
  - `SMS` — placeholder for future Twilio integration

- **Security**
  - JWT-based authentication for all endpoints
  - CORS configured for Angular frontend (localhost:4200)

- **Database Integration**
  - MySQL-based persistence
  - Tables: `notifications`
  - Database: `skybooker_notification_db`

- **API Documentation**
  - Swagger/OpenAPI enabled at `/swagger-ui.html`

### Key Endpoints

| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/notifications/user/{userId}` | PASSENGER / ADMIN | Get all notifications for a user |
| GET | `/api/v1/notifications/user/{userId}/unread` | PASSENGER / ADMIN | Get unread notifications |
| GET | `/api/v1/notifications/user/{userId}/unread/count` | PASSENGER / ADMIN | Get unread notification count |
| PUT | `/api/v1/notifications/{id}/read` | PASSENGER / ADMIN | Mark a notification as read |
| PUT | `/api/v1/notifications/user/{userId}/read-all` | PASSENGER / ADMIN | Mark all notifications as read |
| DELETE | `/api/v1/notifications/{id}` | PASSENGER / ADMIN | Delete a notification |
| POST | `/api/v1/notifications/admin/broadcast` | ADMIN | Send broadcast notification to a list of users |

---

## Microservices Summary

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| service-registry | 8761 | — | Eureka Service Discovery |
| api-gateway | 8080 | — | Spring Cloud Gateway — JWT validation + routing |
| auth-service | 8081 | skybooker_auth_db | User registration, login, JWT, OAuth2 |
| airline-service | 8082 | skybooker_airline_db | Airlines & airports master data |
| flight-service | 8083 | skybooker_flight_db | Flight schedules, search, status + Kafka producer |
| booking-service | 8084 | skybooker_booking_db | Booking lifecycle, PNR, ancillary add-ons, schedulers |
| passenger-service | 8085 | skybooker_passenger_db | Passenger details, seat assignment, check-in |
| seat-service | 8086 | skybooker_seat_db | Seat map, hold/release/confirm (optimistic locking) |
| payment-service | 8087 | skybooker_payment_db | Razorpay integration, refunds, Kafka publisher |
| notification-service | 8088 | skybooker_notification_db | Email, in-app alerts, Kafka consumer, schedulers |

---
