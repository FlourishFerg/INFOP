# InfoPouch Backend

This repository contains the InfoPouch backend application.

## What is included

- Spring Boot backend with JWT authentication
- User registration and email verification
- Login, refresh token, forgot password, reset password, and logout flows
- Local SMTP support via Mailpit for email testing
- Postman-ready endpoints for demo and validation

## Requirements

- Java 21 installed
- Maven wrapper available in the repository
- PostgreSQL and Redis available via Docker Compose
- Docker installed for Mailpit

## Local setup

1. Start PostgreSQL and Redis

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    docker-compose up -d

2. Start Mailpit for local email testing

    powershell
    docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

Open Mailpit UI at http://localhost:8025.

3. Run the backend

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    .\mvnw spring-boot:run

The backend will start on http://localhost:8080.

## How to test with Postman

### 1. Register a new user

- Method: POST
- URL: http://localhost:8080/api/v1/auth/register
- Headers:
  - Content-Type: application/json
- Body: raw JSON

    {
       email: test@example.com,
      password: Password123!,
      fullName: Test User,
      phoneNumber: 08012345678,
      country: Nigeria,
      geopoliticalZone: South West,
      state: Lagos,
      city: Ikeja,
      profession: Student,
      profileType: STUDENT,
      membershipTier: FREE,
      academicQualification: B.Sc,
      gender: Male,
      dateOfBirth: 1990-01-01
    }

Valid values:
- profileType: GUEST, STUDENT, LECTURER, PROFESSIONAL
- membershipTier: FREE, PREMIUM

Note: GUEST cannot be paired with PREMIUM.

### 2. Verify email

After registration, Mailpit will receive a verification email.

1. Open http://localhost:8025
2. Find the latest verification email
3. Copy the verification link or token
4. In Postman use:
   - Method: GET
   - URL: http://localhost:8080/api/v1/auth/verify?token=PASTE_TOKEN_HERE

### 3. Login

- Method: POST
- URL: http://localhost:8080/api/v1/auth/login
- Body:

    {
      email: test@example.com,
      password: Password123!
    }

Successful response returns accessToken and refreshToken.

### 4. Refresh token

- Method: POST
- URL: http://localhost:8080/api/v1/auth/refresh
- Body:

    {
      refreshToken: YOUR_REFRESH_TOKEN
    }

### 5. Forgot password

- Method: POST
- URL: http://localhost:8080/api/v1/auth/forgot-password
- Body:

    {
      email: test@example.com
    }

After this request Mailpit will receive a reset email containing a clickable link with a short-lived token.

Default email link target:
- If you have a frontend: the link points to `FRONTEND_URL/reset-password?token=...` (default `http://localhost:3000`).
- If you don't have a frontend: you can still copy the token from Mailpit and use the API directly.

### 6. Reset password

- Method: POST
- URL: http://localhost:8080/api/v1/auth/reset-password
- Body:

    {
      token: YOUR_RESET_TOKEN,
      newPassword: NewPassword123!
    }

Recommended (click-to-reset) flow:
1. Click the link in the reset email — your frontend should read the `token` query parameter.
2. Frontend calls `GET /api/v1/auth/reset/validate?token=...` to confirm the token is valid.
3. Frontend shows a password form and then POSTs to `/api/v1/auth/reset-password` with `{ token, newPassword }`.

If you do not have a frontend, copy the token from Mailpit and call the POST `/api/v1/auth/reset-password` directly.

### 7. Logout

- Method: POST
- URL: http://localhost:8080/api/v1/auth/logout
- Header:
  - Authorization: Bearer YOUR_REFRESH_TOKEN

### 8. Test email delivery endpoints

These endpoints send test verification or reset emails directly.

- Method: POST
- URL: http://localhost:8080/api/v1/email/send-test?recipient=test@example.com

- Method: POST
- URL: http://localhost:8080/api/v1/email/send-reset?recipient=test@example.com

Then confirm the message appears in Mailpit UI.

## Notes

- Use Content-Type: application/json for all POST requests.
- The default SMTP config is already set for Mailpit in src/main/resources/application.yml.
 - New config: `app.frontend-url` controls where password reset links point. Default is `http://localhost:3000`. You can override it with the `APP_FRONTEND_URL` env var or set in `application.yml`.
- If you get a validation error, check the field names and enum values.

## Useful commands

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    .\mvnw -q -DskipTests compile
    .\mvnw spring-boot:run

For Mailpit:

    powershell
    docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit

For local database support:

    powershell
    docker-compose up -d

## Endpoints summary

- POST /api/v1/auth/register
- GET /api/v1/auth/verify?token=...
- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/auth/forgot-password
- POST /api/v1/auth/reset-password
 - GET /api/v1/auth/reset/validate?token=...  # validate token before showing reset form
- POST /api/v1/auth/logout
- POST /api/v1/email/send-test?recipient=...
- POST /api/v1/email/send-reset?recipient=...
