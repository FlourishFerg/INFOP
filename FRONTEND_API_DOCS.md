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
| `AUTH_005` | 403 | Authenticated, but lacks the required role (e.g. a non-admin calling an admin-only research endpoint) |
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

type ResearchStatus = "PENDING" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";

interface ResearchResponse {
  id: string;
  userId: string;
  title: string;
  authors: string[];
  institution: string;
  publicationYear: number;
  researchField: string;
  countryOfStudy: string;
  methodology: string;
  fileType: string;
  abstractText: string;
  fileUrl: string;              // signed, time-limited view URL — regenerated on every response
  status: ResearchStatus;
  rejectionReason: string | null;
  keywords: string[];
  createdAt: string;            // ISO datetime
}

// Slimmer shape used by list endpoints (own research, admin queue, public search)
interface ResearchPaperResponse {
  id: string;
  title: string;
  institution: string;
  publicationYear: number;
  researchField: string;
  status: ResearchStatus;
  createdAt: string;
}

interface ShareLinkResponse {
  shareUrl: string;             // "<FRONTEND_URL>/research/shared/<token>"
  token: string;
  expiresAt: string;            // ISO datetime, 7 days from creation
}

interface PresignedUrlResponse {
  uploadUrl: string;            // PUT the file directly here, bypassing the backend
  fileKey: string;              // pass this into CreateResearchRequest.fileKey
  downloadUrl: string;          // not generally needed — ResearchResponse.fileUrl is regenerated fresh
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

## Research Endpoints

Submitting a research paper is a two-step flow: first get a presigned upload URL and upload the
file directly to cloud storage, then create the research record referencing that file.

```
1. POST /api/v1/uploads/presigned-url  -> { uploadUrl, fileKey, downloadUrl }
2. PUT <uploadUrl> with the raw file bytes (direct to cloud storage, not through this backend)
3. POST /api/v1/research with fileKey + fileUrl (use downloadUrl from step 1) + the rest of the form
```

### 12. Get Presigned Upload URL

```
POST /api/v1/uploads/presigned-url
```

**Requires:** `Authorization: Bearer <accessToken>`

**Request body:**
```json
{
  "fileName": "my-thesis.pdf",
  "contentType": "application/pdf",
  "fileSizeBytes": 2400000
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| fileName | string | Yes | |
| contentType | string | Yes | e.g. `application/pdf` |
| fileSizeBytes | number | Yes | |

**Response:** `ApiResponse<PresignedUrlResponse>` — HTTP 200

After getting `uploadUrl`, `PUT` the raw file bytes to it directly from the browser — do not
proxy the file through this backend. Keep `fileKey` for step 3.

---

### 13. Create Research

```
POST /api/v1/research
```

**Requires:** `Authorization: Bearer <accessToken>`

**Request body:**
```json
{
  "title": "Effects of X on Y",
  "authors": ["Jane Doe", "John Smith"],
  "institution": "University of Lagos",
  "publicationYear": 2024,
  "researchField": "Computer Science",
  "countryOfStudy": "Nigeria",
  "methodology": "Quantitative",
  "fileType": "PDF",
  "abstractText": "A sufficiently long abstract describing the research...",
  "fileKey": "<from presigned-url response>",
  "fileUrl": "<downloadUrl from presigned-url response>",
  "fileSizeBytes": 2400000,
  "keywords": ["machine learning", "education", "africa", "policy", "data"]
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| title | string | Yes | Max 255 chars |
| authors | string[] | Yes | At least 1 |
| institution | string | Yes | |
| publicationYear | number | Yes | Between 1900–2100 |
| researchField | string | Yes | |
| countryOfStudy | string | Yes | |
| methodology | string | Yes | |
| fileType | string | Yes | |
| abstractText | string | Yes | Minimum 20 chars |
| fileKey | string | Yes | From the presigned-url step |
| fileUrl | string | Yes | From the presigned-url step |
| fileSizeBytes | number | Yes | |
| keywords | string[] | Yes | **Minimum 5** — this is enforced server-side |

**Response:** `ApiResponse<ResearchResponse>` — HTTP 201

New submissions always start at `status: "PENDING"` and are not publicly visible until an admin
approves them.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_001` | Missing/invalid field (including fewer than 5 keywords) |

---

### 14. List My Research

```
GET /api/v1/research
```

**Requires:** `Authorization: Bearer <accessToken>`

**Response:** `ApiResponse<ResearchPaperResponse[]>` — HTTP 200

Returns only the authenticated user's own submissions, in the slim list shape (no abstract,
authors, file URL, etc. — use endpoint 15 for full details on one item).

---

### 15. Get Research by ID

```
GET /api/v1/research/{id}
```

**Requires:** `Authorization: Bearer <accessToken>`

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID |

---

### 16. Update Research

```
PUT /api/v1/research/{id}
```

**Requires:** `Authorization: Bearer <accessToken>` (must be the owner)

**Request body:** Same shape as Create Research (endpoint 13) — this replaces the whole record,
not a partial patch.

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200

Updating resets `status` back to `"PENDING"` and clears `rejectionReason` — the paper re-enters
the review queue.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_001` | Missing/invalid field |
| 404 | `NOT_FOUND_001` | No research with this ID |
| 409 | `STATE_001` | You're not the owner, **or** the paper is already `APPROVED` (approved papers can't be edited — block the edit UI for those) |

---

### 17. Delete Research

```
DELETE /api/v1/research/{id}
```

**Requires:** `Authorization: Bearer <accessToken>` (must be the owner)

**Response:** `ApiResponse<null>` — HTTP 200

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID |
| 409 | `STATE_001` | You're not the owner |

---

### 18. View/Download Research (own account)

```
GET /api/v1/research/{id}/download
```

**Requires:** `Authorization: Bearer <accessToken>`

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200, including a freshly-signed `fileUrl`

Despite the name, this doesn't trigger a file download response — it returns the research record
with a time-limited signed URL your frontend can open/embed. The owner can always access their
own paper regardless of status; anyone else can only access it if `status === "APPROVED"`.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID, **or** you're not the owner and it isn't `APPROVED` yet |

---

### 19. Create Share Link

```
POST /api/v1/research/{id}/share
```

**Requires:** `Authorization: Bearer <accessToken>`

**Response:** `ApiResponse<ShareLinkResponse>` — HTTP 200

Generates a public, no-login-required link valid for **7 days**. The owner can share a paper in
any status (including `PENDING`); a non-owner can only generate a share link for an already
`APPROVED` paper. Anyone with the link can view it via endpoint 23, regardless of their own
auth state — treat this like any other shareable link (don't put anything sensitive behind it
beyond what the paper itself contains).

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID, **or** you're not the owner and it isn't `APPROVED` yet |

---

## Admin Research Endpoints

All endpoints below require an **ADMIN** role, not just a logged-in user. A non-admin
authenticated user gets a 403, distinct from the 401 an unauthenticated user gets.

### 20. List Research by Status

```
GET /api/v1/admin/research?status=PENDING
```

**Requires:** `Authorization: Bearer <accessToken>` (ADMIN role)

| Query param | Required | Notes |
|---|---|---|
| status | No | One of `PENDING`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`. Defaults to `PENDING` |

**Response:** `ApiResponse<ResearchPaperResponse[]>` — HTTP 200

Use this to build the admin review queue — default view (no query param) shows what's awaiting
review.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 403 | `AUTH_005` | Authenticated but not an admin |

---

### 21. Approve Research

```
POST /api/v1/admin/research/{id}/approve
```

**Requires:** `Authorization: Bearer <accessToken>` (ADMIN role)

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200, `status` becomes `"APPROVED"`

Triggers an in-app notification and an email to the researcher. Once approved, the paper becomes
publicly visible (endpoints 22–25) and the owner can no longer edit it (endpoint 16).

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID |
| 403 | `AUTH_005` | Authenticated but not an admin |

---

### 22. Reject Research

```
POST /api/v1/admin/research/{id}/reject
```

**Requires:** `Authorization: Bearer <accessToken>` (ADMIN role)

**Request body:**
```json
{
  "reason": "Methodology section needs more detail on sample size."
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| reason | string | Yes | Shown to the researcher |

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200, `status` becomes `"REJECTED"`,
`rejectionReason` set to the given reason

Triggers an in-app notification and an email to the researcher including the reason. The
researcher can edit and resubmit (endpoint 16), which resets status back to `PENDING`.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_001` | Missing/blank `reason` |
| 404 | `NOT_FOUND_001` | No research with this ID |
| 403 | `AUTH_005` | Authenticated but not an admin |

---

## Public Research Endpoints

These don't require authentication — safe to call from a logged-out landing page. All of them
(except the share-link one) only ever return papers with `status === "APPROVED"`.

### 23. Search Public Research Catalog

```
GET /api/v1/research/public/search
```

| Query param | Required | Notes |
|---|---|---|
| query | No | Free-text, case-insensitive substring match |
| researchField | No | |
| institution | No | |
| year | No | Publication year |
| country | No | Country of study |

All filters are optional and combine (AND) when multiple are present; omit all for the full
approved catalog.

**Response:** `ApiResponse<ResearchPaperResponse[]>` — HTTP 200

---

### 24. View Public Research

```
GET /api/v1/research/public/{id}
```

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200, including a freshly-signed `fileUrl`

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | No research with this ID, **or** it isn't `APPROVED` |

---

### 25. Generate Citation

```
GET /api/v1/research/public/{id}/citation?format=APA
```

| Query param | Required | Notes |
|---|---|---|
| format | No | `APA` or `MLA` (case-insensitive). Defaults to `APA` |

**Response:** `ApiResponse<string>` — HTTP 200, `data` is the formatted citation string, e.g.:
```json
{
  "success": true,
  "message": "Citation generated.",
  "data": "Doe, J., & Smith, J. (2024). Effects of X on Y. University of Lagos."
}
```

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 400 | `VALIDATION_002` | `format` is something other than `APA`/`MLA` |
| 404 | `NOT_FOUND_001` | No research with this ID, **or** it isn't `APPROVED` |

---

### 26. View Shared Research

```
GET /api/v1/research/shared/{token}
```

The `token` comes from a share link generated via endpoint 19 (the part after
`/research/shared/` in `shareUrl`).

**Response:** `ApiResponse<ResearchResponse>` — HTTP 200, including a freshly-signed `fileUrl`

Unlike the other public endpoints, this works for a paper in **any** status, since the share link
itself (not approval status) is what grants access.

**Failure cases:**
| Status | error_code | When |
|---|---|---|
| 404 | `NOT_FOUND_001` | Token doesn't exist, **or** it's expired (7 days after creation) |

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

### `research.service.ts`

```typescript
import api from "@/lib/axios";

export type ResearchStatus = "PENDING" | "UNDER_REVIEW" | "APPROVED" | "REJECTED";

export interface ResearchResponse {
  id: string;
  userId: string;
  title: string;
  authors: string[];
  institution: string;
  publicationYear: number;
  researchField: string;
  countryOfStudy: string;
  methodology: string;
  fileType: string;
  abstractText: string;
  fileUrl: string;
  status: ResearchStatus;
  rejectionReason: string | null;
  keywords: string[];
  createdAt: string;
}

export interface ResearchPaperResponse {
  id: string;
  title: string;
  institution: string;
  publicationYear: number;
  researchField: string;
  status: ResearchStatus;
  createdAt: string;
}

export interface ShareLinkResponse {
  shareUrl: string;
  token: string;
  expiresAt: string;
}

export interface PresignedUrlResponse {
  uploadUrl: string;
  fileKey: string;
  downloadUrl: string;
}

export interface CreateResearchPayload {
  title: string;
  authors: string[];
  institution: string;
  publicationYear: number;
  researchField: string;
  countryOfStudy: string;
  methodology: string;
  fileType: string;
  abstractText: string;
  fileKey: string;
  fileUrl: string;
  fileSizeBytes: number;
  keywords: string[]; // minimum 5
}

export interface PresignedUrlPayload {
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
}

const researchService = {
  getPresignedUploadUrl: (payload: PresignedUrlPayload) =>
    api.post<ApiResponse<PresignedUrlResponse>>("/uploads/presigned-url", payload),

  createResearch: (payload: CreateResearchPayload) =>
    api.post<ApiResponse<ResearchResponse>>("/research", payload),

  getMyResearch: () =>
    api.get<ApiResponse<ResearchPaperResponse[]>>("/research"),

  getResearchById: (id: string) =>
    api.get<ApiResponse<ResearchResponse>>(`/research/${id}`),

  updateResearch: (id: string, payload: CreateResearchPayload) =>
    api.put<ApiResponse<ResearchResponse>>(`/research/${id}`, payload),

  deleteResearch: (id: string) =>
    api.delete<ApiResponse<null>>(`/research/${id}`),

  viewResearch: (id: string) =>
    api.get<ApiResponse<ResearchResponse>>(`/research/${id}/download`),

  createShareLink: (id: string) =>
    api.post<ApiResponse<ShareLinkResponse>>(`/research/${id}/share`),

  // Public, no auth required
  searchPublic: (params: {
    query?: string;
    researchField?: string;
    institution?: string;
    year?: number;
    country?: string;
  }) => api.get<ApiResponse<ResearchPaperResponse[]>>("/research/public/search", { params }),

  viewPublic: (id: string) =>
    api.get<ApiResponse<ResearchResponse>>(`/research/public/${id}`),

  getCitation: (id: string, format: "APA" | "MLA" = "APA") =>
    api.get<ApiResponse<string>>(`/research/public/${id}/citation`, { params: { format } }),

  viewShared: (token: string) =>
    api.get<ApiResponse<ResearchResponse>>(`/research/shared/${token}`),
};

export default researchService;
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

*Generated against backend commit `2345f08` — InfoPouch Backend v0.0.1*
