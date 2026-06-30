# InfoPouch Backend — Frontend Integration Docs

**Base URL:** `https://infopouch.koyeb.app/api/v1`

All requests must include the header:
```
Content-Type: application/json
```

---

## Response Envelope

Every **successful** response uses this wrapper shape:

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}
```

**Error responses use a different shape** — they are not wrapped in `ApiResponse`. Any non-2xx
response body looks like this instead:

```typescript
interface ErrorResponse {
  error_code: string;
  message: string;
  timestamp: string;   // ISO datetime
  path: string;
  status: number;       // matches the HTTP status code
  trace_id: string;     // include this if asking backend devs to investigate a failure
}
```

```json
{
  "error_code": "AUTH_003",
  "message": "Invalid credentials or account status. Please verify your information.",
  "timestamp": "2026-06-29T14:30:18.154Z",
  "path": "/api/v1/auth/login",
  "status": 400,
  "trace_id": "2b902080-d224-40b0-83a4-1658e1245705"
}
```

In your axios client, check `response.success` only on 2xx responses; on error, read
`error.response.data.error_code` / `.message` instead. Don't rely on `success: false` appearing
in error bodies — it doesn't.

### Error codes reference

| `error_code` | HTTP status | Meaning |
|---|---|---|
| `VALIDATION_001` | 400 | Request body failed `@Valid` field validation (missing/malformed fields) |
| `VALIDATION_002` | 400 | Custom validation failure |
| `VALIDATION_003` | 400 | Invalid argument not specifically auth-related (e.g. bad/unknown token, malformed refresh token) |
| `AUTH_001` / `AUTH_002` | 401 | Authentication failed (bad credentials at the security layer) |
| `AUTH_003` | 400 | Invalid credentials or account state (e.g. wrong password, unverified email at login) |
| `AUTH_004` | 401 | Malformed, invalid, or expired JWT |
| `STATE_001` | 409 | Operation conflicts with current state (e.g. login attempt before email verification) |
| `CONFLICT_001` | 409 | Record already exists (e.g. duplicate email registration, including the rare case where two registrations for the same email race each other) |
| `NOT_FOUND_001` | 404 | Requested resource doesn't exist |
| `INTERNAL_ERROR` | 500 | Unexpected server error — show a generic message and the `trace_id` |

---

## TypeScript Types

Copy these into your types file:

```typescript
type ProfileType = "STUDENT" | "LECTURER" | "PROFESSIONAL" | "GUEST";
type MembershipTier = "FREE" | "PREMIUM";
type Role = "STUDENT" | "LECTURER" | "PROFESSIONAL" | "GUEST";

interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  membershipTier: MembershipTier | null;  // null = not yet selected
  onboardingCompleted: boolean;
}

interface ProfileResponse {
  id: string;
  userId: string;
  email: string;
  fullName: string;
  role: Role;
  profileType: ProfileType;
  membershipTier: MembershipTier | null;
  phoneNumber: string | null;
  country: string | null;
  geopoliticalZone: string | null;
  state: string | null;
  city: string | null;
  profession: string | null;
  academicQualification: string | null;
  gender: string | null;
  dateOfBirth: string | null;   // format: "YYYY-MM-DD"
  isVerified: boolean;
  onboardingCompleted: boolean;
  createdAt: string;            // ISO datetime
}
```

---

## Auth Flow Overview

```
Register → Email Verification → Login → Membership Selection → Profile Completion → Dashboard
```

After a successful login, use the `membershipTier` and `onboardingCompleted` fields in the
response to determine which screen to route the user to:

```typescript
if (data.membershipTier === null) {
  // redirect to Membership Selection screen
} else if (!data.onboardingCompleted) {
  // redirect to Profile Completion screen
} else {
  // redirect to Dashboard
}
```

---

## Auth Endpoints

### 1. Register

```
POST /api/v1/auth/register
```

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "fullName": "Test User",
  "profileType": "STUDENT"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| email | string | Yes | Must be a valid email address |
| password | string | Yes | Minimum 8 characters |
| fullName | string | Yes | |
| profileType | string | Yes | `GUEST`, `STUDENT`, `LECTURER`, `PROFESSIONAL` |

**Response:** `ApiResponse<null>` — HTTP 201

No tokens are returned at this stage. The user must verify their email before logging in.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_001` | Missing/invalid fields (bad email format, password too short, etc.) |
| 400 | `VALIDATION_003` | Email already registered (the normal case) |
| 409 | `CONFLICT_001` | Email already registered, detected via a database-level conflict instead — only happens if two registration requests for the same email land at the same instant |

Treat both `VALIDATION_003`-on-duplicate-email and `CONFLICT_001` the same way in the UI ("this
email is already registered, try logging in instead") — don't branch UI behavior on which one
came back, since which one you get depends on request timing, not anything the user controls.

---

### 2. Verify Email

```
GET /api/v1/auth/verify?token=<token>
```

The token comes from the verification link emailed to the user. Read it from the URL query
parameter and call this endpoint automatically when the user lands on the verification page.

**Response:** `ApiResponse<null>` — HTTP 200

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_003` | Token doesn't exist, isn't a verification token, has already been used, has been revoked, or has expired |

The verification token is single-use — calling this endpoint twice with the same token returns
400 the second time. Show a generic "this link is invalid or has already been used" message
rather than trying to distinguish the exact reason.

---

### 3. Login

```
POST /api/v1/auth/login
```

**Request body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Response:** `ApiResponse<JwtResponse>`

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "userId": "usr_abc123",
    "email": "user@example.com",
    "membershipTier": null,
    "onboardingCompleted": false
  }
}
```

Store `accessToken`, `refreshToken`, `userId`, and `email` in localStorage after login.
Use `membershipTier` and `onboardingCompleted` to decide where to route the user next.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `AUTH_003` | Wrong email/password |
| 409 | `STATE_001` | Credentials are correct but the account email isn't verified yet — prompt the user to check their inbox or resend verification |

---

### 4. Refresh Access Token

```
POST /api/v1/auth/refresh
```

**Request body:**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response:** `ApiResponse<JwtResponse>` — HTTP 200

Call this when the API returns a 401 to get a new access token using the stored refresh token.
Note the response includes the *same* `refreshToken` you sent — refresh tokens aren't rotated,
only the access token is reissued.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_003` | Token not found, expired, revoked, or not actually a refresh token (e.g. a stray verification/reset token submitted here by mistake) |

On any failure here, treat it as a hard logout: clear stored tokens and send the user to login.

---

### 5. Forgot Password

```
POST /api/v1/auth/forgot-password
```

**Request body:**
```json
{
  "email": "user@example.com"
}
```

**Response:** `ApiResponse<null>` — HTTP 200, always, regardless of input

Always returns success even if the email does not exist, and even if the email fails to send —
this prevents account enumeration (an attacker can't distinguish "no such account" from "email
provider hiccup" by watching for different response shapes). Don't build any UI logic that
expects this endpoint to ever return an error for a bad/unregistered email.

If the email is registered, a reset link is sent to the user's inbox. The link points to:
```
<FRONTEND_URL>/reset-password?token=<token>
```

---

### 6. Validate Reset Token

```
GET /api/v1/auth/reset/validate?token=<token>
```

**Response:** `ApiResponse<boolean>`

```json
{
  "success": true,
  "message": "Token validation result",
  "data": true
}
```

Call this when the user lands on the reset-password page to confirm the token is still valid
before showing the password form. If `data` is `false`, show an "invalid or expired link" message.

---

### 7. Reset Password

```
POST /api/v1/auth/reset-password
```

**Request body:**
```json
{
  "token": "<token from URL query param>",
  "newPassword": "NewPassword123!"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| token | string | Yes | From the reset link URL |
| newPassword | string | Yes | Minimum 8 characters |

**Response:** `ApiResponse<null>` — HTTP 200

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `AUTH_003` | Token doesn't exist, isn't a password-reset token, has been revoked, or has expired |

The reset token is single-use — once consumed, `GET /auth/reset/validate` for the same token
will return `false` and resubmitting it here returns 400.

---

### 8. Logout

```
POST /api/v1/auth/logout
```

**Header:**
```
Authorization: Bearer <refreshToken>
```

> **Important:** Send the **refresh token** here, not the access token.
> The backend uses it to invalidate the session.

**Response:** `ApiResponse<null>` — HTTP 200, or HTTP 400 if the `Authorization` header is
missing/malformed (no `Bearer ` prefix). A refresh token that doesn't exist in the backend
(already logged out, expired and pruned, etc.) is treated as already-logged-out and still
returns 200 — logout is idempotent.

After calling this, clear all auth keys from localStorage regardless of the response:
`accessToken`, `refreshToken`, `userId`, `userEmail`, `userFullName`, `userProfileType`

---

## Profile Endpoints

All profile endpoints require:
```
Authorization: Bearer <accessToken>
```

A missing, malformed, or expired/invalid access token returns **HTTP 403** (not 401 — Spring
Security's default access-denied behavior here, since these routes are configured as
"authenticated" rather than going through a custom 401 entry point). On a 403 from any of these
endpoints, attempt a token refresh (see endpoint 4) and retry once; if the refresh also fails,
treat it as a hard logout.

---

### 9. Get Current Profile

```
GET /api/v1/profile/me
```

**Response:** `ApiResponse<ProfileResponse>`

Use this on app load to re-check onboarding status and hydrate the user's profile in the UI.

---

### 10. Select Membership Tier

```
PUT /api/v1/profile/membership
```

**Request body:**
```json
{
  "membershipTier": "FREE"
}
```

| Value | Notes |
|---|---|
| `FREE` | Available to all profile types |
| `PREMIUM` | Not available to `GUEST` accounts |

**Response:** `ApiResponse<ProfileResponse>`

Mapping from the current membership UI to what to send the backend:

| UI Label | Send to backend |
|---|---|
| Starter | `FREE` |
| Pro | `PREMIUM` |
| Enterprise | Contact flow only — no backend tier for this yet |

---

### 11. Complete Profile

```
PUT /api/v1/profile/complete
```

All fields are optional. Send only what the user fills in.

**Request body:**
```json
{
  "phoneNumber": "08012345678",
  "country": "Nigeria",
  "geopoliticalZone": "South West",
  "state": "Lagos",
  "city": "Ikeja",
  "profession": "Software Engineer",
  "academicQualification": "B.Sc",
  "gender": "Male",
  "dateOfBirth": "1990-01-01"
}
```

| Field | Type | Notes |
|---|---|---|
| phoneNumber | string | Optional |
| country | string | Optional |
| geopoliticalZone | string | Optional |
| state | string | Optional |
| city | string | Optional |
| profession | string | Optional |
| academicQualification | string | Optional |
| gender | string | Optional |
| dateOfBirth | string | Optional. Format: `YYYY-MM-DD` |

**Response:** `ApiResponse<ProfileResponse>`

Calling this endpoint sets `onboardingCompleted = true` on the user's profile regardless of
which fields were filled. After a successful response, redirect the user to the Dashboard.

---

## Updated Service Files

### `auth.service.ts`

```typescript
import api from "@/lib/axios";

export type ProfileType = "STUDENT" | "LECTURER" | "PROFESSIONAL" | "GUEST";
export type MembershipTier = "FREE" | "PREMIUM";

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  profileType: ProfileType;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface JwtResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  membershipTier: MembershipTier | null;
  onboardingCompleted: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

const authService = {
  register: (payload: RegisterPayload) =>
    api.post<ApiResponse<null>>("/auth/register", payload),

  verifyEmail: (token: string) =>
    api.get<ApiResponse<null>>("/auth/verify", { params: { token } }),

  login: (payload: LoginPayload) =>
    api.post<ApiResponse<JwtResponse>>("/auth/login", payload),

  refreshToken: (refreshToken: string) =>
    api.post<ApiResponse<JwtResponse>>("/auth/refresh", { refreshToken }),

  forgotPassword: (email: string) =>
    api.post<ApiResponse<null>>("/auth/forgot-password", { email }),

  validateResetToken: (token: string) =>
    api.get<ApiResponse<boolean>>("/auth/reset/validate", { params: { token } }),

  resetPassword: (token: string, newPassword: string) =>
    api.post<ApiResponse<null>>("/auth/reset-password", { token, newPassword }),

  logout: async () => {
    const refreshToken = localStorage.getItem("refreshToken");
    try {
      await api.post("/auth/logout", null, {
        headers: { Authorization: `Bearer ${refreshToken}` },
      });
    } finally {
      [
        "accessToken",
        "refreshToken",
        "userId",
        "userEmail",
        "userFullName",
        "userProfileType",
      ].forEach((k) => localStorage.removeItem(k));
    }
  },
};

export default authService;
```

---

### `profile.service.ts`

```typescript
import api from "@/lib/axios";
import { MembershipTier } from "./auth.service";

export type Role = "STUDENT" | "LECTURER" | "PROFESSIONAL" | "GUEST";
export type ProfileType = "STUDENT" | "LECTURER" | "PROFESSIONAL" | "GUEST";

export interface ProfileResponse {
  id: string;
  userId: string;
  email: string;
  fullName: string;
  role: Role;
  profileType: ProfileType;
  membershipTier: MembershipTier | null;
  phoneNumber: string | null;
  country: string | null;
  geopoliticalZone: string | null;
  state: string | null;
  city: string | null;
  profession: string | null;
  academicQualification: string | null;
  gender: string | null;
  dateOfBirth: string | null;   // "YYYY-MM-DD"
  isVerified: boolean;
  onboardingCompleted: boolean;
  createdAt: string;
}

export interface SelectMembershipPayload {
  membershipTier: MembershipTier;
}

export interface CompleteProfilePayload {
  phoneNumber?: string;
  country?: string;
  geopoliticalZone?: string;
  state?: string;
  city?: string;
  profession?: string;
  academicQualification?: string;
  gender?: string;
  dateOfBirth?: string; // "YYYY-MM-DD"
}

const profileService = {
  getMyProfile: () =>
    api.get<ApiResponse<ProfileResponse>>("/profile/me"),

  selectMembership: (payload: SelectMembershipPayload) =>
    api.put<ApiResponse<ProfileResponse>>("/profile/membership", payload),

  completeProfile: (payload: CompleteProfilePayload) =>
    api.put<ApiResponse<ProfileResponse>>("/profile/complete", payload),
};

export default profileService;
```

---

## Fields Removed from Old Contract

The following fields that existed in the old `RegisterPayload` and `ProfilePayload` no longer
exist or have been renamed. Update any forms or state that reference them:

| Old field | Status | Replacement |
|---|---|---|
| `membershipTier` in RegisterPayload | Removed | Use `PUT /profile/membership` after login |
| `phoneNumber` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `country` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `geopoliticalZone` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `state` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `city` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `profession` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `academicQualification` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `gender` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `dateOfBirth` in RegisterPayload | Removed | Use `PUT /profile/complete` after login |
| `geoZone` in ProfilePayload | Renamed | `geopoliticalZone` |
| `qualification` in ProfilePayload | Renamed | `academicQualification` |
| `birthDay` + `birthMonth` in ProfilePayload | Replaced | `dateOfBirth` as `"YYYY-MM-DD"` |
| `street` in ProfilePayload | Removed | No equivalent on backend |
| `sector` in ProfilePayload | Removed | No equivalent on backend |
| `POST /profile` (createProfile) | Removed | Profile is created automatically on register |
| `PUT /profile/me` (updateProfile) | Removed | Use `/profile/membership` and `/profile/complete` |

---
