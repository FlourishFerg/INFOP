# InfoPouch Backend

This repository contains the InfoPouch backend application.

## What is included

- Spring Boot backend with JWT authentication
- User registration and email verification
- Login, refresh token, forgot password, reset password, and logout flows
- Email sent via the Resend HTTP API (works on hosts that block outbound SMTP, e.g. Railway's free tier)
- Postman-ready endpoints for demo and validation

## Requirements

- Java 21 installed
- Maven wrapper available in the repository
- PostgreSQL and Redis available via Docker Compose
- A Resend account and API key (free, no card required) - https://resend.com

## Local setup

1. Start PostgreSQL and Redis

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    docker-compose up -d

2. Get a Resend API key

   Sign up at https://resend.com, go to API Keys -> Create API Key, and set it as the
   `RESEND_API_KEY` environment variable. No domain verification needed for local testing -
   the default sandbox sender (`onboarding@resend.dev`) works immediately, but can only send to
   the email address on your Resend account until you verify a domain.

3. Run the backend

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    .\mvnw spring-boot:run

The backend will start on http://localhost:8080.

## How to test with Postman

### 1. Register a new user

Account registration only collects the core account fields plus role selection. Membership
tier and the rest of the profile are collected later, after login (see steps 4 and 5).

- Method: POST
- URL: http://localhost:8080/api/v1/auth/register
- Headers:
  - Content-Type: application/json
- Body: raw JSON

    {
      email: test@example.com,
      password: Password123!,
      fullName: Test User,
      profileType: STUDENT
    }

Valid values:
- profileType: GUEST, STUDENT, LECTURER, PROFESSIONAL

### 2. Verify email

After registration, a verification email is sent via Resend to the registered address.

1. Check the inbox of the email you registered with
2. Open the verification email
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

Successful response returns `accessToken`, `refreshToken`, `userId`, `email`,
`membershipTier`, and `onboardingCompleted`. Use these last two fields to decide where to
route the user next:

- `membershipTier` is `null` -> show **Membership Selection** (step 4)
- `membershipTier` is set but `onboardingCompleted` is `false` -> show **Profile Completion** (step 5)
- `onboardingCompleted` is `true` -> go straight to the **Dashboard**

### 4. Membership selection

- Method: PUT
- URL: http://localhost:8080/api/v1/profile/membership
- Headers:
  - Authorization: Bearer YOUR_ACCESS_TOKEN
  - Content-Type: application/json
- Body:

    {
      membershipTier: FREE
    }

Valid values: FREE, PREMIUM. Note: GUEST accounts (chosen at registration) cannot select PREMIUM.

### 5. Profile completion

- Method: PUT
- URL: http://localhost:8080/api/v1/profile/complete
- Headers:
  - Authorization: Bearer YOUR_ACCESS_TOKEN
  - Content-Type: application/json
- Body:

    {
      phoneNumber: 08012345678,
      country: Nigeria,
      geopoliticalZone: South West,
      state: Lagos,
      city: Ikeja,
      profession: Student,
      academicQualification: B.Sc,
      gender: Male,
      dateOfBirth: 1990-01-01
    }

This marks `onboardingCompleted` as `true`. After this, the user can be taken to the Dashboard.

### 6. Get current profile

- Method: GET
- URL: http://localhost:8080/api/v1/profile/me
- Headers:
  - Authorization: Bearer YOUR_ACCESS_TOKEN

Returns the full profile, including `membershipTier` and `onboardingCompleted`, so the
frontend can re-check onboarding status at any time (e.g. on app load).

### 7. Refresh token

- Method: POST
- URL: http://localhost:8080/api/v1/auth/refresh
- Body:

    {
      refreshToken: YOUR_REFRESH_TOKEN
    }

### 8. Forgot password

- Method: POST
- URL: http://localhost:8080/api/v1/auth/forgot-password
- Body:

    {
      email: test@example.com
    }

After this request a reset email is sent via Resend, containing a clickable link with a short-lived token.

Default email link target:
- If you have a frontend: the link points to `FRONTEND_URL/reset-password?token=...` (default `http://localhost:3000`).
- If you don't have a frontend: you can still copy the token from the email and use the API directly.

### 9. Reset password

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

If you do not have a frontend, copy the token from the email and call the POST `/api/v1/auth/reset-password` directly.

### 10. Logout

- Method: POST
- URL: http://localhost:8080/api/v1/auth/logout
- Header:
  - Authorization: Bearer YOUR_REFRESH_TOKEN

### 11. Test email delivery endpoints

These endpoints send test verification or reset emails directly.

- Method: POST
- URL: http://localhost:8080/api/v1/email/send-test?recipient=test@example.com

- Method: POST
- URL: http://localhost:8080/api/v1/email/send-reset?recipient=test@example.com

Then confirm delivery on your Resend dashboard, or check the recipient inbox.

## Notes

- Use Content-Type: application/json for all POST requests.
- Email config (`RESEND_API_KEY`, `RESEND_FROM_ADDRESS`) defaults to Resend's sandbox sender in src/main/resources/application.yml. Until a domain is verified on Resend, emails can only be sent to the address on your own Resend account.
 - New config: `app.frontend-url` controls where password reset links point. Default is `http://localhost:3000`. You can override it with the `APP_FRONTEND_URL` env var or set in `application.yml`.
- If you get a validation error, check the field names and enum values.

## Useful commands

    powershell
    cd C:\Users\DELL\Desktop\InfopouchBackend
    .\mvnw -q -DskipTests compile
    .\mvnw spring-boot:run

For local database support:

    powershell
    docker-compose up -d

## Endpoints summary

- POST /api/v1/auth/register
- GET /api/v1/auth/verify?token=...
- POST /api/v1/auth/login
- PUT /api/v1/profile/membership       # Membership Selection (auth required)
- PUT /api/v1/profile/complete         # Profile Completion (auth required)
- GET /api/v1/profile/me                # Current profile + onboarding status (auth required)
- POST /api/v1/auth/refresh
- POST /api/v1/auth/forgot-password
- POST /api/v1/auth/reset-password
 - GET /api/v1/auth/reset/validate?token=...  # validate token before showing reset form
- POST /api/v1/auth/logout
- POST /api/v1/email/send-test?recipient=...
- POST /api/v1/email/send-reset?recipient=...

## Auth flow overview

Account Registration (incl. role selection) -> Email Verification -> Login -> Membership
Selection -> Profile Completion -> Dashboard
