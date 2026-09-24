# Subscription Tracker (Indian Market Edition)

A full-stack application for tracking subscriptions, avoiding unwanted auto-debits (UPI AutoPay, Cards), receiving 3-day and 1-day renewal alerts, and guarding against free trial charges.

## Tech Stack
- **Backend:** Spring Boot 3, Java 17, Maven, Spring Data JPA, Spring Security with JWT, MySQL, Lombok, springdoc-openapi, Spring Mail
- **Frontend:** React (Vite), React Router, Axios, Recharts, Plain CSS Modules

## Getting Started

### 1. Database Setup
Ensure MySQL is running on `localhost:3306` and create the database:
```sql
CREATE DATABASE IF NOT EXISTS subscription_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configuration & Profiles
Copy `.env.example` to `.env` or use Spring profiles.

Local secrets can be set in `backend/src/main/resources/application-local.yml` (which is gitignored).

To run the backend with `dev` and `local` profiles active:
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
```
Or via java jar:
```bash
java -jar target/subscription-tracker-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev,local
```
Or export the environment variable:
```bash
$env:JWT_SECRET="your_secure_random_key_at_least_32_characters_long"
mvn spring-boot:run
```

### 3. Swagger API Documentation
Once running, navigate to:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
