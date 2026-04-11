# SkyBookerApp - Airline Ticket Booking System

## About - 
SkyBooker is a full-stack Airline Ticket Booking System inspired by MakeMyTrip and GoIbibo. It connects passengers with airlines, enabling them to search for one-way and round-trip flights, compare fare classes, select seats on an interactive seat map, enter passenger details, add ancillary services (meal preferences, extra baggage), and complete the booking with secure online payment — all from a unified platform.

## Auth Service – SkyBooker

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