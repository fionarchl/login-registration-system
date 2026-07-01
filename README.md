# Login Registration System

Login Registration System is a Spring Boot authentication API that provides user registration, login, JWT-based authentication, refresh tokens, email verification, rate limiting, and account lockout protection.

The project is built with Java 17, Spring Boot 3.3.1, Spring Security, Spring Data JPA, and Maven.

## Features

- User registration with request validation
- Secure password hashing with BCrypt
- Strong password validation
- Login with JWT access tokens
- Refresh token support
- Email verification token workflow
- Password change support
- Account lockout handling
- Rate limiting for sensitive authentication flows
- Centralized API responses and exception handling
- JPA persistence with H2 or PostgreSQL support
- Unit and integration tests for authentication behavior

## Tech Stack

- Java 17
- Spring Boot 3.3.1
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- JJWT
- Spring Mail
- H2 Database
- PostgreSQL
- Maven
- JUnit, Mockito, and Spring Security Test

## Project Structure

```text
src
├── main
│   ├── java/com/auth/demo
│   │   ├── config          # Security and JPA auditing configuration
│   │   ├── controller      # REST API controllers
│   │   ├── dto             # Request and response DTOs
│   │   ├── exception       # Custom exceptions and global handler
│   │   ├── model           # JPA entities
│   │   ├── repository      # Spring Data repositories
│   │   ├── security        # JWT filter and authentication entry point
│   │   ├── service         # Business service interfaces and implementations
│   │   ├── util            # Input sanitization helpers
│   │   └── validation      # Custom password validation
│   └── resources
│       └── application.properties.example
└── test
    ├── java/com/auth/demo
    │   ├── controller      # Integration tests
    │   └── service         # Service unit tests
    └── resources
```

## Requirements

- Java 17 or later
- Maven 3.9+ or the included Maven wrapper
- PostgreSQL, if running with a persistent database

For local development, the application can also be configured to use H2.

## Getting Started

Clone the repository and move into the project directory:

```bash
git clone https://github.com/fionarchl/login-registration-system.git "Login Registration System"
cd "Login Registration System"
```

Create your application configuration from the example file:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Update `application.properties` with your local database, JWT, and mail settings.

On Windows, run the app with the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use:

```bash
./mvnw spring-boot:run
```

By default, the API is typically available at:

```text
http://localhost:8080
```

## Configuration

The project includes `src/main/resources/application.properties.example` as the starting point for environment-specific settings.

Common settings include:

- Database connection URL, username, and password
- JPA and Hibernate options
- JWT secret and expiration values
- Refresh token expiration values
- Mail server settings
- Application URL used in verification links
- Rate limiting and account lockout settings

Do not commit real secrets, production passwords, private keys, or live mail credentials.

## API Overview

The API is organized around authentication and account management. Typical flows include:

- Register a new user
- Verify the user's email address
- Log in with email and password
- Use the returned JWT access token for protected routes
- Refresh an expired access token with a refresh token
- Change the authenticated user's password

Authentication-protected requests should include the access token in the `Authorization` header:

```http
Authorization: Bearer <access-token>
```

## Example Requests

Register a user:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "StrongPass123!",
    "name": "Example User"
  }'
```

Log in:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "StrongPass123!"
  }'
```

Refresh an access token:

```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<refresh-token>"
  }'
```

## Testing

Run the test suite with:

```powershell
.\mvnw.cmd test
```

The tests cover authentication service behavior and controller-level authentication flows.

## Build

Create a packaged application with:

```powershell
.\mvnw.cmd clean package
```

The compiled artifact is written to the `target` directory.

## Development Notes

- Keep `application.properties.example` updated when new configuration keys are introduced.
- Store secrets outside source control for real deployments.
- Add or update tests when changing authentication rules, token behavior, validation, or security configuration.
- Prefer DTOs for request and response payloads instead of exposing entities directly.
