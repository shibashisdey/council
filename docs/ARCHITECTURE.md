# Architecture

**Purpose**
This document explains the current system architecture, data flows, and service contracts for StackFul Minds.

**System Map**
1. `eureka-server` — service registry
2. `api-gateway` — JWT validation, request routing, basic role gating
3. `user-authentication-service` — auth + JWT issuance
4. `user-service` — client profile
5. `counselor-service` — therapist profile
6. `availability-service` — scheduling rules and slot blocking
7. `appointment-service` — booking orchestration
8. `payment-service` — payment state and confirmation
9. `review-service` — session notes + reviews
10. `notification-service` — PDF upload to R2 and email trigger (placeholder)

**Diagrams**

**System Map**
```mermaid
flowchart LR
  Client[Client Apps] --> Gateway[API Gateway]
  Gateway --> Auth[User Authentication Service]
  Gateway --> UserSvc[User Service]
  Gateway --> CounselorSvc[Counselor Service]
  Gateway --> ApptSvc[Appointment Service]
  ApptSvc --> AvailSvc[Availability Service]
  Gateway --> ReviewSvc[Review Service]
  ReviewSvc --> NotifSvc[Notification Service]
  NotifSvc --> R2[Cloudflare R2]
  Gateway --> PaymentSvc[Payment Service]
  PaymentSvc --> ApptSvc
  Eureka[Eureka Server] --- Auth
  Eureka --- UserSvc
  Eureka --- CounselorSvc
  Eureka --- ApptSvc
  Eureka --- AvailSvc
  Eureka --- ReviewSvc
  Eureka --- NotifSvc
  Eureka --- PaymentSvc
```

**Auth And Profile Setup**
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant A as Auth Service
  participant G as API Gateway
  participant U as User Service
  participant T as Counselor Service
  C->>A: POST /auth/register
  C->>A: POST /auth/login
  A-->>C: JWT
  C->>G: POST /users (JWT)
  G->>U: create user profile
  U-->>G: user profile
  G-->>C: user profile
  C->>G: POST /counselors (JWT)
  G->>T: create counselor profile
  T-->>G: counselor profile
  G-->>C: counselor profile
```

**Booking + Availability**
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant G as API Gateway
  participant A as Appointment Service
  participant V as Availability Service
  C->>G: POST /appointments (JWT)
  G->>A: create appointment
  A->>V: isSlotAvailable
  V-->>A: true/false
  A->>A: save appointment (PENDING_PAYMENT)
  A->>V: blockSlot (APPOINTMENT_HOLD, appointmentId)
  V-->>A: ok
  A-->>G: appointment response
  G-->>C: appointment response
```

**Payment Confirmation**
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant P as Payment Service
  participant A as Appointment Service
  participant V as Availability Service
  C->>P: POST /payments (appointmentId)
  P-->>C: payment initiated
  C->>P: POST /payments/{id}/confirm
  P->>A: confirmAppointment
  A->>V: updateBlockReason(APPOINTMENT_CONFIRMED)
  V-->>A: ok
  A-->>P: ok
  P-->>C: success
```

**Session Notes And Review**
```mermaid
sequenceDiagram
  autonumber
  participant T as Counselor
  participant G as API Gateway
  participant R as Review Service
  participant A as Appointment Service
  participant N as Notification Service
  participant R2 as Cloudflare R2
  T->>G: POST /session-notes (JWT)
  G->>R: create session note
  R->>A: GET /appointments/{id}/internal
  A-->>R: appointment details
  R-->>G: session note
  G-->>T: session note
  T->>G: PATCH /session-notes/{id}/share
  G->>R: share note
  R->>A: PUT /appointments/{id}/complete
  R->>N: POST /notifications/session-note/{id}
  N->>R: GET /session-notes/{id}/internal
  N->>R2: upload PDF
  N->>R: PATCH /session-notes/{id}/pdf
  R-->>G: shared note
  G-->>T: ok
```

**Client Note History**
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant G as API Gateway
  participant R as Review Service
  C->>G: GET /session-notes/user/{userId} (JWT)
  G->>R: fetch shared notes
  R-->>G: notes list (no privateNotes)
  G-->>C: notes list
```

**Database ER (High-Level)**
```mermaid
erDiagram
  AUTH_USER {
    bigint id PK
    string email
    string password
    string role
    boolean enabled
  }

  USER_PROFILE {
    bigint id PK
    string fullName
    string email
    string phoneNumber
    string gender
    date dateOfBirth
    string city
  }

  COUNSELOR {
    bigint id PK
    bigint userId
    string fullName
    string qualification
    int experienceYears
    string bio
    double pricePerSession
    boolean active
  }

  SPECIALIZATION {
    bigint id PK
    string name
  }

  COUNSELOR_SPECIALIZATION {
    bigint counselor_id FK
    bigint specialization_id FK
  }

  APPOINTMENT {
    bigint id PK
    bigint clientId
    bigint counselorId
    date appointmentDate
    time startTime
    time endTime
    string status
    string paymentId
    datetime slotLockedAt
  }

  PAYMENT {
    bigint id PK
    bigint appointmentId
    decimal amount
    string status
    string gatewayPaymentId
  }

  SESSION_NOTE {
    bigint id PK
    bigint appointmentId
    bigint userId
    bigint counselorId
    date sessionDate
    string summary
    string observations
    string recommendations
    string privateNotes
    boolean sharedWithClient
    string pdfObjectKey
    string pdfUrl
  }

  REVIEW {
    bigint id PK
    bigint appointmentId
    bigint userId
    bigint counselorId
    int rating
    string comment
  }

  AUTH_USER ||--|| USER_PROFILE : "id = id"
  AUTH_USER ||--o| COUNSELOR : "id = userId"
  COUNSELOR ||--o{ COUNSELOR_SPECIALIZATION : has
  SPECIALIZATION ||--o{ COUNSELOR_SPECIALIZATION : has
  AUTH_USER ||--o{ APPOINTMENT : "clientId"
  COUNSELOR ||--o{ APPOINTMENT : "counselorId"
  APPOINTMENT ||--|| PAYMENT : "appointmentId"
  APPOINTMENT ||--|| SESSION_NOTE : "appointmentId"
  APPOINTMENT ||--|| REVIEW : "appointmentId"
```

**Core Data Domains**

**Auth**
1. `auth.users`
   - `id`, `email`, `password`, `role`, `enabled`
2. JWT claims:
   - `userId`, `email`, `role`

**User Profile**
1. `users.users`
   - `id`, `fullName`, `email`, `phoneNumber`, `gender`, `dateOfBirth`, `city`

**Counselor Profile**
1. `counselors`
   - `id`, `userId`, `fullName`, `qualification`, `experienceYears`, `bio`, `pricePerSession`, `active`
2. `specializations`
   - `id`, `name`
3. `counselor_specializations`
   - `counselor_id`, `specialization_id`

**Availability**
1. `counselor_working_hours`
2. `counselor_lunch_breaks`
3. `counselor_unavailability`
4. `public_holidays`

**Appointments**
1. `appointments`
   - `id`, `clientId`, `counselorId`, `appointmentDate`, `startTime`, `endTime`
   - `status`, `paymentId`, `slotLockedAt`

**Payments**
1. `payments`
   - `id`, `appointmentId`, `amount`, `status`, `gatewayPaymentId`

**Session Notes**
1. `session_notes`
   - `appointmentId`, `userId`, `counselorId`, `sessionDate`
   - `summary`, `observations`, `recommendations`, `privateNotes`
   - `sharedWithClient`, `pdfObjectKey`, `pdfUrl`

**Reviews**
1. `reviews`
   - `appointmentId`, `userId`, `counselorId`, `rating`, `comment`

**Service Contracts**

**Auth**
1. `POST /auth/register`
2. `POST /auth/login`

**User Service**
1. `POST /users`
2. `GET /users/me`
3. `PATCH /users/me`
4. `GET /users/{id}/public` (internal)

**Counselor Service**
1. `POST /counselors`
2. `GET /counselors/me`
3. `GET /counselors`
4. `GET /counselors/{id}`
5. `PUT /counselors/{id}`

**Availability Service**
1. `GET /internal/availability/check`
2. `POST /internal/availability/block`
3. `PUT /internal/availability/block/{referenceId}/reason`
4. `POST /internal/availability/free/{referenceId}`
5. `POST /schedule/working-hours/{counselorId}`
6. `POST /schedule/lunch-break/{counselorId}`
7. `POST /schedule/unavailability/{counselorId}`
8. `DELETE /schedule/unavailability/{counselorId}/{unavailabilityId}`
9. `GET /schedule/calendar/{counselorId}`

**Appointments**
1. `POST /appointments`
2. `GET /appointments/client`
3. `GET /appointments/counselor/{counselorId}`
4. `PUT /appointments/{appointmentId}/reschedule`
5. `DELETE /appointments/{appointmentId}`
6. `PUT /appointments/{appointmentId}/confirm`
7. `GET /appointments/{appointmentId}/status`
8. `GET /appointments/{appointmentId}/internal`
9. `PUT /appointments/{appointmentId}/complete`

**Payments**
1. `POST /payments`
2. `POST /payments/{appointmentId}/confirm`
3. `POST /payments/{appointmentId}/fail`

**Review Service**
1. `POST /session-notes`
2. `PUT /session-notes/{id}`
3. `PATCH /session-notes/{id}/share`
4. `PATCH /session-notes/{id}/pdf`
5. `GET /session-notes/user/{userId}`
6. `GET /session-notes/counselor/{counselorId}`
7. `GET /session-notes/appointment/{appointmentId}`
8. `GET /session-notes/{noteId}/internal`
9. `POST /reviews`
10. `GET /reviews/counselor/{counselorId}`
11. `GET /reviews/user/{userId}`

**Notification Service**
1. `POST /notifications/session-note/{noteId}`

**Key Flows**

**1) Auth + Profile Setup**
1. Register + login via auth service
2. Gateway injects headers
3. Create user or counselor profile

**2) Booking**
1. Appointment service checks availability
2. Appointment saved in `PENDING_PAYMENT`
3. Availability slot blocked as `APPOINTMENT_HOLD`

**3) Payment**
1. Payment created (INITIATED)
2. Payment confirmed → appointment confirmed
3. Availability block updated to `APPOINTMENT_CONFIRMED`

**4) Session Notes**
1. Counselor creates note after appointment confirmed/completed
2. Counselor shares note → appointment completed + notification triggered
3. Notification service uploads PDF to R2 and updates review service
4. Client sees note + PDF link in history

**Error Handling**
1. Appointment service: fail fast on availability errors
2. Review service: 404/403/409 with proper error codes
3. Notification service: skips missing notes, logs errors

**Environment Variables**
Notification service requires R2 values:
1. `r2.endpoint`
2. `r2.access-key`
3. `r2.secret-key`
4. `r2.bucket`
5. `r2.public-base-url`
