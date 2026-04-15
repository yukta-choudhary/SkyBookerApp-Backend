# SkyBookerApp - Airline Ticket Booking System

## About - 
SkyBooker is a full-stack Airline Ticket Booking System inspired by MakeMyTrip and GoIbibo. It connects passengers with airlines, enabling them to search for one-way and round-trip flights, compare fare classes, select seats on an interactive seat map, enter passenger details, add ancillary services (meal preferences, extra baggage), and complete the booking with secure online payment — all from a unified platform.

## Auth Service 

The **Auth Service** is responsible for managing user identity, authentication, and authorization in the SkyBooker Airline Ticket Booking System.

### 🚀 Features Implemented

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

## Airline Service 

The **Airline Service** is responsible for managing airline and airport master data in the SkyBooker Airline Ticket Booking System. It provides foundational data required for flight creation, search, and booking operations across the platform.

### 🚀 Features Implemented

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

## Flight Service 

The **Flight Service** manages flight schedules and inventory in the SkyBooker Airline Ticket Booking System. It acts as the core service for flight search and availability.

### 🚀 Features Implemented

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
