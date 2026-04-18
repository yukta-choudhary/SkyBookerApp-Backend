# SkyBookerApp - Airline Ticket Booking System

## About - 
SkyBooker is a full-stack Airline Ticket Booking System inspired by MakeMyTrip and GoIbibo. It connects passengers with airlines, enabling them to search for one-way and round-trip flights, compare fare classes, select seats on an interactive seat map, enter passenger details, add ancillary services (meal preferences, extra baggage), and complete the booking with secure online payment — all from a unified platform.

## Auth Service – SkyBooker

The **Auth Service** is responsible for managing user identity, authentication, and authorization in the SkyBooker Airline Ticket Booking System.

###  Features Implemented

- **User Registration**
  - Register users with email and password
  - Default role assignment (PASSENGER)
  - Validation for unique email

- **User Login**
  - Secure login using email and password
  - JWT token generation on successful authentication

- **JWT-Based Authentication**
  - Stateless authentication using JSON Web Tokens
  - Token validation for every protected API request
  - Includes user role and identity in token

- **Role-Based Access Control**
  - Supports roles:
    - PASSENGER
    - AIRLINE_STAFF
    - ADMIN
  - Enables service-level authorization across microservices

- **Password Security**
  - Passwords stored using encrypted (hashed) format

- **Logout Functionality**
  - Token invalidation using blacklist mechanism

- **Forgot Password / Reset Password**
  - Generate secure reset tokens
  - Email-based password reset flow

- **OAuth2 Integration**

- **Database Integration**
  - MySQL-based persistence
  - Tables:
    - users
    - password_reset_tokens
    - token_blacklist

- **Microservices Ready**
  - Integrated with **Eureka Service Registry**
  - Works with **API Gateway**
  - Provides authentication for all other services

- **API Documentation**
  - Swagger/OpenAPI enabled for testing endpoints


---

## Airline Service – SkyBooker

The **Airline Service** is responsible for managing airline and airport master data in the SkyBooker Airline Ticket Booking System. It provides foundational data required for flight creation, search, and booking operations across the platform.

###  Features Implemented

- **Airline Management**
  - Create, update, activate, and deactivate airline profiles
  - Unique identification using IATA and ICAO codes
  - Store airline details such as name, logo, country, and contact information

- **Airport Management**
  - Create and manage airport profiles
  - Supports IATA and ICAO codes
  - Stores city, country, GPS coordinates, and timezone for accurate scheduling

- **Airport Search & Autocomplete**
  - Search airports by keyword (name, city, or IATA code)
  - Enables autocomplete functionality for flight search UI

- **Airline Retrieval**
  - Fetch airline details by ID or IATA code
  - List all airlines or only active airlines

- **Airport Retrieval**
  - Fetch airport details by ID or IATA code
  - Filter airports by city or country

- **Role-Based Access Control**
  - Supports roles:
    - PASSENGER
    - AIRLINE_STAFF
    - ADMIN
  - Access Rules:
    - ADMIN → Full access (create/update/delete)
    - AIRLINE_STAFF → Read-only access
    - PASSENGER / GUEST → Public read access for search

- **JWT-Based Authentication**
  - Stateless authentication using JSON Web Tokens
  - Token validation for every protected API request
  - Extracts user role for authorization

- **Database Integration**
  - MySQL-based persistence
  - Tables:
    - airlines
    - airports

---
---

## Flight Service – SkyBooker

The **Flight Service** manages flight schedules and inventory in the SkyBooker Airline Ticket Booking System. It acts as the core service for flight search and availability.

###  Features Implemented

- **Flight Management**
  - Create new flight schedules
  - Store flight number, airline reference, aircraft type
  - Maintain departure and arrival times

- **Flight Search**
  - Search flights by:
    - Origin airport
    - Destination airport
    - Date
  - Supports one-way search functionality

- **Flight Status Management**
  - Update real-time flight status:
    - ON_TIME
    - DELAYED
    - CANCELLED
    - DEPARTED
    - ARRIVED

- **Seat Inventory Tracking**
  - Maintain total seats and available seats
  - Automatically initializes available seats on flight creation

- **Airline-wise Flight Retrieval**
  - Fetch all flights for a specific airline

- **Flight Update & Deletion**
  - Update flight details such as timing, aircraft type, and pricing
  - Delete flights when required

- **Database Integration**
  - MySQL-based persistence
  - Tables:
    - flights

- **JWT-Based Authentication**
  - Validates JWT tokens issued by Auth Service
  - Ensures secure access to protected endpoints

---

## Booking Service – SkyBooker

The **Booking Service** is the central orchestration service responsible for managing the complete booking lifecycle in the SkyBooker system.

###  Features Implemented

- **Booking Creation**
  - Create booking linked to user and flight
  - Automatically generates unique **PNR code**
  - Initializes booking with `PENDING` status

- **PNR-Based Retrieval**
  - Fetch booking using PNR code
  - Enables quick lookup without full authentication flow

- **Booking Lifecycle Management**
  - Status transitions:
    - PENDING
    - CONFIRMED
    - CANCELLED
    - COMPLETED
    - NO_SHOW

- **User Booking Management**
  - Retrieve all bookings for a user
  - Retrieve bookings by flight

- **Booking Cancellation**
  - Cancel bookings
  - Update status to `CANCELLED`

- **Fare Storage**
  - Stores:
    - Base fare
    - Taxes
    - Total fare
  - Supports future extension for dynamic fare calculation

- **Trip Type Support**
  - ONE_WAY
  - ROUND_TRIP

- **Database Integration**
  - MySQL-based persistence
  - Tables:
    - bookings

- **JWT-Based Authentication**
  - Validates JWT tokens from Auth Service
  - Ensures only authorized users can manage bookings

---

## Passenger Service – SkyBooker

The **Passenger Service** manages detailed passenger information associated with each booking in the SkyBooker system.

###  Features Implemented

- **Passenger Management**
  - Add passenger details per booking
  - Store:
    - Name, DOB, gender
    - Passport details
    - Nationality

- **Ticket Number Generation**
  - Auto-generates unique ticket number for each passenger

- **Booking Linkage**
  - Each passenger is linked to a booking
  - Supports multiple passengers per booking

- **Seat Assignment**
  - Assign seat ID and seat number to passenger
  - Supports future integration with Seat Service

- **Passenger Retrieval**
  - Get passenger by ID
  - Get all passengers for a booking

- **Passenger Update**
  - Update passenger details (name, passport, etc.)

- **Passenger Deletion**
  - Remove passenger records if required

- **Passenger Count**
  - Count total passengers per booking

- **Passenger Type Support**
  - ADULT
  - CHILD
  - INFANT

- **Database Integration**
  - MySQL-based persistence
  - Tables:
    - passenger_info

- **JWT-Based Authentication**
  - Validates JWT tokens for secure access
  - Ensures role-based operations


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
