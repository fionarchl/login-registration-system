# Auth Demo

An Authentication API built with Spring Boot, featuring JWT-based security, email verification, rate limiting, and account lockout mechanisms.

## 🚀 Features

- **JWT Authentication**: Secure token-based authentication for API requests.
- **Email Verification**: Ensures users verify their email addresses upon registration.
- **Rate Limiting**: Protects the API from abuse and brute-force attacks.
- **Account Lockout**: Automatically locks accounts after multiple failed login attempts to enhance security.
- **Database Support**: Configured for PostgreSQL (production) and H2 (development/testing).

## 🛠 Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.3.1
- **Security**: Spring Security, JJWT (JSON Web Token)
- **Database**: PostgreSQL, H2
- **Build Tool**: Maven

## 🏁 Getting Started

### Prerequisites

- Java 17 JDK
- Maven (or use the included `./mvnw` wrapper)
- PostgreSQL (if running in production mode)

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd "Java app"
   ```

2. **Build the project:**
   ```bash
   ./mvnw clean install
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`.

## ⚙️ Configuration

Modify `src/main/resources/application.properties` to configure your database and mail server:

- `spring.datasource.url`: Your PostgreSQL connection string.
- `spring.mail.host`: Your SMTP server for email verification.
