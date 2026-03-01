# StackFul Minds

**Overview**
StackFul Minds is a Spring Boot microservices system for therapy sessions. It includes authentication, user and counselor profiles, appointment booking with availability rules, payments, session notes and reviews, and notification/PDF delivery via Cloudflare R2.

**Services And Ports**
1. `eureka-server` — `8761` (service registry)
2. `api-gateway` — `8091`
3. `user-authentication-service` — `8081`
4. `counselor-service` — `8090`
5. `appointment-service` — `8083`
6. `availability-service` — `8085`
7. `link-gererator-service` — `8082`
8. `review-service` — `8086`
9. `notification-service` — `8088`
10. `payment-service` — `8087`
11. `user-service` — `8084`

**High-Level Flow**
1. Auth service registers and logs in users, issues JWT.
2. API Gateway validates JWT and injects `X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`.
3. User service stores client profiles.
4. Counselor service stores therapist profiles and specializations.
5. Appointment service orchestrates booking and calls availability service.
6. Availability service enforces working hours, lunch breaks, unavailability, holidays, and daily caps.
7. Payment service confirms appointments and updates availability.
8. Link gererator service generates Jitsi meeting links for confirmed appointments.
9. Review service stores counselor session notes and client reviews.
10. Notification service generates PDF and uploads to Cloudflare R2.

**Auth And Gateway**
1. JWT is issued by auth service.
2. Gateway validates JWT and adds trusted headers:
   - `X-USER-ID`
   - `X-USER-EMAIL`
   - `X-USER-ROLE`
3. Gateway routes:
   - `/auth/**` → auth service
   - `/counselors/**` → counselor service
   - `/users/**` → user service
   - `/appointments/**` → appointment service
   - `/reviews/**` → review service
   - `/session-notes/**` → review service
   - `/payments/**` → payment service
   - `/schedule/**` → availability service
4. Role checks currently enforced at gateway:
   - `/counselors/**` allow `THERAPIST` for write, `CLIENT|THERAPIST` for read
   - `/users/**` allow `CLIENT` only

**User Authentication Service**
1. `POST /auth/register` — register user with role (`CLIENT` or `THERAPIST`)
2. `POST /auth/login` — returns JWT

**User Service**
1. `POST /users` — create user profile (client)
2. `GET /users/me` — fetch own profile
3. `PATCH /users/me` — update own profile
4. `GET /users/{id}/public` — internal use only

**Counselor Service**
1. `POST /counselors` — create counselor profile (therapist)
2. `GET /counselors/me` — fetch own profile
3. `GET /counselors` — list active counselors
4. `GET /counselors/{id}` — fetch counselor by id
5. `PUT /counselors/{id}` — update counselor

**Availability Service**
Rules enforced:
1. Working hours per counselor per weekday.
2. Daily lunch break per counselor.
3. Unavailability blocks for leave/appointments.
4. Public holidays (Nager.Date sync).
5. Daily cap: max 7 confirmed appointments.

Endpoints:
1. `GET /internal/availability/check`
2. `POST /internal/availability/block`
3. `PUT /internal/availability/block/{referenceId}/reason`
4. `POST /internal/availability/free/{referenceId}`
5. `POST /schedule/working-hours/{counselorId}`
6. `POST /schedule/lunch-break/{counselorId}`
7. `POST /schedule/unavailability/{counselorId}`
8. `DELETE /schedule/unavailability/{counselorId}/{unavailabilityId}`
9. `GET /schedule/calendar/{counselorId}`

**Appointment Service**
Core behavior:
1. Checks client overlap.
2. Checks availability before save.
3. Blocks slot after save.
4. Expires unpaid holds after 10 minutes and frees slots.
5. Reschedule is all-or-nothing with compensation.
6. Confirm updates availability block reason to `APPOINTMENT_CONFIRMED`.
7. `counselorId` refers to counselor profile ID (not auth user id).

Endpoints:
1. `POST /appointments` — create appointment
2. `GET /appointments/client` — list client appointments
3. `GET /appointments/counselor/{counselorId}` — list counselor appointments
4. `PUT /appointments/{appointmentId}/reschedule`
5. `DELETE /appointments/{appointmentId}`
6. `PUT /appointments/{appointmentId}/confirm`
7. `GET /appointments/{appointmentId}/status`
8. `GET /appointments/{appointmentId}/internal`
9. `PUT /appointments/{appointmentId}/complete`

**Payment Service**
1. `POST /payments` — create payment (idempotent)
2. `POST /payments/{appointmentId}/confirm` — confirm payment and confirm appointment
3. `POST /payments/{appointmentId}/fail` — mark payment failed

Payment checks:
1. Appointment must be `PENDING_PAYMENT` to create payment.
2. Confirm only if appointment is `PENDING_PAYMENT` or already `CONFIRMED`.

**Link Gererator Service (Internal)**
1. `POST /internal/meeting-links` — create or get meeting link
2. `GET /internal/meeting-links/{appointmentId}` — fetch link by appointment
3. `PUT /internal/meeting-links/{appointmentId}` — update times
4. `DELETE /internal/meeting-links/{appointmentId}` — delete link

**Review Service**
Two separate domains:
1. Session Notes (counselor → user)
2. Reviews (user → counselor)

Session Notes:
1. `POST /session-notes` — therapist creates note
2. `PUT /session-notes/{id}` — therapist updates note
3. `PATCH /session-notes/{id}/share` — therapist shares note, completes appointment, triggers notification
4. `PATCH /session-notes/{id}/pdf` — internal update of PDF metadata
5. `GET /session-notes/user/{userId}` — client sees shared notes only
6. `GET /session-notes/counselor/{counselorId}` — therapist sees all notes
7. `GET /session-notes/appointment/{appointmentId}` — internal fetch
8. `GET /session-notes/{noteId}/internal` — internal fetch by note id

Reviews:
1. `POST /reviews` — client review after appointment completed
2. `GET /reviews/counselor/{counselorId}`
3. `GET /reviews/user/{userId}`

**Notification Service**
1. `POST /notifications/session-note/{noteId}`
2. Fetches note from review service
3. Generates a PDF placeholder and uploads to Cloudflare R2
4. Updates review service with `pdfObjectKey` and `pdfUrl`

**Cloudflare R2 Configuration**
Set in `notification-service/src/main/resources/application.properties`:
1. `r2.endpoint`
2. `r2.access-key`
3. `r2.secret-key`
4. `r2.bucket`
5. `r2.public-base-url`

**Local Testing Order**
1. `eureka-server`
2. `api-gateway`
3. `user-authentication-service`
4. `user-service`
5. `counselor-service`
6. `availability-service`
7. `appointment-service`
8. `link-gererator-service`
9. `payment-service`
10. `review-service`
11. `notification-service`

**Postman Quick Start**
1. Register therapist: `POST http://localhost:8081/auth/register`
2. Login therapist: `POST http://localhost:8081/auth/login`
3. Create counselor profile via gateway: `POST http://localhost:8091/counselors`
4. Register client: `POST http://localhost:8081/auth/register`
5. Login client: `POST http://localhost:8081/auth/login`
6. Create user profile via gateway: `POST http://localhost:8091/users`

**Notes**
1. Most internal calls currently use fixed `localhost` URLs, not Eureka load balancing.
2. API Gateway must be running for client-facing routes.
3. Internal endpoints (ex: `/internal/**`, `/payments/*/confirm`) require internal JWT auth.

