# Donkey Guide: Full Project Explanation (Simple + Complete)

This file explains this project in very simple words.
Think of this as: what exists, where it lives, why it exists, and how everything talks to each other.

## 1) What this project is

This repo is a **microservices therapy platform**.
Users can:
- register/login
- create client or counselor profile
- set counselor schedule
- book sessions
- pay
- generate meeting links
- write session notes
- leave reviews

The repo root is a **multi-module Maven project**.
- Root build file: `pom.xml`
- Java version: `17`
- Spring Boot parent: `3.2.5`
- Spring Cloud BOM: `2023.0.1`

## 2) Top-level modules and purpose

From root `pom.xml`, modules are:
1. `eureka-server` - service discovery registry
2. `api-gateway` - entry point + JWT validation + route forwarding
3. `user-authentication-service` - auth + JWT issuance
4. `counselor-service` - counselor profile data
5. `link-gererator-service` - meeting links for appointments
6. `appointment-service` - booking workflow and orchestration
7. `review-service` - reviews + session notes
8. `notification-service` - PDF generation + R2 upload + notifications
9. `payment-service` - payment lifecycle and appointment confirmation trigger
10. `product-webapp` - Spring container + Angular frontend app
11. `user-service` - client profile data
12. `availability-service` - schedule, lunch, slot blocks, holidays

## 3) Core architecture idea

- **API Gateway** is the public door.
- It validates user JWT and adds trusted headers.
- It routes request to proper service.
- Services call each other using internal HTTP APIs.
- Internal service-to-service calls are protected using **internal JWT**.
- Most data is stored in PostgreSQL (service-specific DB/table sets).

## 4) Main technologies and why they are used

## 4.1 Backend platform
- **Spring Boot**: fast REST service setup, dependency injection, config.
- **Spring Web**: REST controllers and APIs.
- **Spring Data JPA**: repository pattern + ORM for DB access.
- **Hibernate**: actual ORM engine behind JPA.
- **PostgreSQL driver**: DB connectivity.
- **Spring Cloud Eureka**: service discovery.
- **Spring Cloud Gateway**: API gateway routing and filters.
- **Spring Security** (auth service): login/security stack.
- **Lombok**: reduces boilerplate (`@Getter`, `@Setter`, `@Builder`).
- **WebFlux/WebClient** in some services: non-blocking internal HTTP calls.

## 4.2 Authentication/authorization
- **JJWT (`io.jsonwebtoken`)** used in multiple services.
- Purpose:
  - parse/validate public JWT at gateway
  - generate/validate internal JWT for service-to-service protection

## 4.3 Scheduling and async behavior
- Spring scheduling used in availability and appointment flows.
- Examples:
  - clean stale holds
  - apply pending schedule changes

## 4.4 External integrations
- **AWS SDK S3**: used for Cloudflare R2 object storage (S3-compatible API).
- **PDFBox**: PDF generation in notification service.
- **Google Calendar client deps** present in availability service (currently optional/commented config).
- **Nager.Date holiday flow** represented in availability holiday sync design.

## 4.5 Frontend
- Angular app inside `product-webapp/webapp`.
- Uses HttpClient services to call gateway/backend APIs.
- Guards and auth-state manage route-level access.

## 5) Service-by-service deep explanation

## 5.1 `eureka-server`
Where:
- `eureka-server/src/main/java/.../EurekaServerApplication.java`
- `eureka-server/src/main/resources/application.properties`

Why:
- Central registry so services can discover each other.

Dependencies:
- `spring-cloud-starter-netflix-eureka-server`

## 5.2 `api-gateway`
Where:
- `api-gateway/src/main/java/.../JwtAuthenticationFilter.java`
- `api-gateway/src/main/java/.../CorsConfig.java`
- `api-gateway/src/main/resources/application.properties`

Why:
- Single public entry point.
- Validates public JWT.
- Adds `X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE` to downstream calls.
- Enforces route/role checks and forwards to services.

Dependencies:
- Spring Cloud Gateway
- Eureka client
- JJWT

## 5.3 `user-authentication-service`
Where:
- `.../controller/AuthController.java`
- `.../service/AuthServiceImpl.java`
- `.../service/JwtService.java`
- `.../security/SecurityConfig.java`

Why:
- Register user credentials.
- Login and issue JWT used by gateway.

Dependencies:
- Spring Security
- Spring Web
- Spring Data JPA
- JJWT

## 5.4 `user-service`
Where:
- `.../controller/UserController.java`
- `.../service/UserServiceImpl.java`
- `.../repository/UserRepository.java`
- `.../security/InternalAuthFilter.java`

Why:
- Store and manage client profile data.
- Internal endpoint(s) for other services to fetch user public info.

Dependencies:
- Spring Web + Data JPA + Postgres + Eureka + internal JWT

## 5.5 `counselor-service`
Where:
- `.../controller/CounselorController.java`
- `.../service/CounselorServiceImpl.java`
- `.../model/Counselor.java`, `Specialization.java`
- `.../repository/CounselorRepository.java`

Why:
- Counselor professional profile and specializations.
- Counselor identity mapping (`userId` -> counselor profile ID).

Dependencies:
- Spring Web + Data JPA + Postgres + Eureka + internal JWT

## 5.6 `availability-service`
Where:
- controllers:
  - `.../controller/CounselorScheduleController.java`
  - `.../controller/InternalAvailabilityController.java`
- services:
  - `.../service/CounselorScheduleServiceImpl.java`
  - `.../service/AvailabilityServiceImpl.java`
- scheduler:
  - `.../scheduler/PendingScheduleChangeScheduler.java`
  - `.../scheduler/AvailabilityCleanupScheduler.java`
- models:
  - `CounselorWorkingHours`
  - `LunchBreak`
  - `CounselorUnavailability`
  - `PendingScheduleChange`
  - `PublicHoliday`

Why:
- Source of truth for counselor schedule availability.
- Handles:
  - weekly work hours
  - lunch breaks
  - leave/unavailability
  - appointment holds/confirmed blocks
  - holidays

### Important: new lunch architecture (implemented now)

Lunch is now **per day** (Monday, Tuesday, ...), not counselor-global.

What changed in code:
- `LunchBreak` model now has `dayOfWeek` and unique `(counselor_id, day_of_week)`.
- Repository now supports day-specific queries.
- Safe update path writes/removes lunch by selected day only.
- Pending scheduler applies lunch changes by selected day only.
- `getSchedule` now returns `lunchBreaks` list with day mappings.
- Availability checks use lunch for that specific day.
- Legacy compatibility fallback exists for old lunch rows with null day.

Key files:
- `availability-service/src/main/java/com/council/availabilityservice/model/LunchBreak.java`
- `availability-service/src/main/java/com/council/availabilityservice/repository/LunchBreakRepository.java`
- `availability-service/src/main/java/com/council/availabilityservice/service/CounselorScheduleServiceImpl.java`
- `availability-service/src/main/java/com/council/availabilityservice/service/AvailabilityServiceImpl.java`
- `availability-service/src/main/java/com/council/availabilityservice/scheduler/PendingScheduleChangeScheduler.java`

## 5.7 `appointment-service`
Where:
- `.../controller/AppointmentController.java`
- `.../service/AppointmentServiceImpl.java`
- `.../scheduler/AppointmentExpiryScheduler.java`
- clients:
  - `AvailabilityClientImpl`
  - `CounselorClientImpl`
  - `LinkGeneratorClientImpl`

Why:
- Main booking orchestration service.
- Creates appointments, validates slot, blocks slot, handles rescheduling/cancel/confirm.
- Calls availability, link generator, payment-related flows.

Dependencies:
- Spring Web + Data JPA + WebFlux client + Postgres + Eureka + JWT

## 5.8 `link-gererator-service`
Where:
- `.../controller/MeetingLinkController.java`
- `.../service/MeetingLinkServiceImpl.java`
- `.../model/MeetingLink.java`

Why:
- Creates/updates Jitsi (or equivalent) meeting links bound to appointment.

Dependencies:
- Spring Web + Data JPA + Postgres + Eureka + internal JWT

## 5.9 `payment-service`
Where:
- `.../controller/PaymentController.java`
- `.../service/PaymentServiceImpl.java`
- `.../repository/PaymentRepository.java`
- `.../client/AppointmentClientImpl.java`

Why:
- Stores payment state.
- On confirm, asks appointment-service to confirm appointment.

Dependencies:
- Spring Web + Data JPA + WebFlux + Postgres + Eureka + internal JWT

## 5.10 `review-service`
Where:
- controllers:
  - `ReviewController.java`
  - `SessionNoteController.java`
- services:
  - `ReviewServiceImpl.java`
  - `SessionNoteServiceImpl.java`
- models:
  - `Review.java`
  - `SessionNote.java`
- clients:
  - appointment/counselor/notification clients

Why:
- Review domain + session notes domain.
- Session note share triggers completion + notification pipeline.

Dependencies:
- Spring Web + Data JPA + WebFlux + Postgres + Eureka + internal JWT

## 5.11 `notification-service`
Where:
- `.../controller/NotificationController.java`
- `.../service/NotificationServiceImpl.java`
- `.../service/R2StorageService.java`
- clients for review/user/counselor/appointment

Why:
- Fetch session note info.
- Generate/upload PDF to Cloudflare R2.
- Update session note with pdf metadata.

Dependencies:
- Spring Web + WebFlux + Data JPA + S3 SDK + PDFBox + Eureka + internal JWT

## 5.12 `product-webapp`
There are two parts:
1. Spring module wrapper (`product-webapp/src/main/java/...`) used for packaging/serving context.
2. Angular app in `product-webapp/webapp`.

Where in Angular:
- app shell/routing/modules:
  - `src/app/app.module.ts`
  - `src/app/app-routing.module.ts`
- auth:
  - `src/app/auth/*`
- core state/guards:
  - `src/app/core/*`
- backend API clients:
  - `src/app/services/*`
- pages:
  - dashboard, setup, profile, appointments, counselors, sessions, home

Why:
- Full therapist/client UI for all major flows.

### Frontend lunch behavior now
- Schedule page uses day cards.
- Each day can have own lunch enabled + lunch hour.
- UI reads day-wise lunch from `lunchBreaks` response.

Key file changed:
- `product-webapp/webapp/src/app/pages/profile/counselor-profile.component.ts`
- `product-webapp/webapp/src/app/services/availability.service.ts`

## 6) Security model summary

Public auth:
- user JWT issued by auth service
- validated by gateway
- forwarded as trusted headers

Internal auth:
- internal JWT generated and validated between services
- filters:
  - `InternalAuthFilter`
  - `InternalJwtService`
- prevents spoofing internal-only APIs

## 7) Data model summary (big picture)

Common important tables by domain:
- auth users
- client profiles
- counselor profiles + specializations
- availability: working hours, lunch breaks, unavailability, holidays, pending schedule changes
- appointments
- payments
- meeting links
- reviews
- session notes

## 8) Scheduling/availability definitions (simple)

- **Working hours**: when counselor can work on a weekday.
- **Lunch break**: blocked 1-hour period inside working day.
- **Unavailability**: explicit blocks (leave, appointment hold, appointment confirmed).
- **Pending schedule change**: delayed application after conflicting appointments are over.
- **Availability check**: final gate used by appointment service before booking.

## 9) Current per-day lunch rules (final behavior)

1. Lunch is stored per counselor + day.
2. Safe day update can set lunch for that day or clear lunch for that day.
3. Updating Monday lunch does not mutate Tuesday lunch.
4. Removing a working day removes lunch for that day.
5. Pending scheduled updates apply lunch changes for that day only.

## 10) Build and run basics

Multi-module backend build:
- run from root with Maven wrapper (`./mvnw ...`) or module-level wrappers.

Frontend build:
- from `product-webapp/webapp`: `npm run build`

Note in restricted/offline environments:
- Angular build may fail if font inlining tries network fetch (Google Fonts), even when code is fine.

## 11) Where to look first when debugging

Auth issues:
- gateway JWT filter
- auth-service JWT generation

Booking issues:
- appointment-service orchestration methods
- availability-service internal endpoints

Schedule issues:
- `CounselorScheduleServiceImpl`
- `PendingScheduleChangeScheduler`
- frontend `counselor-profile.component.ts`

Payment confirmation issues:
- payment-service `PaymentServiceImpl`
- appointment-service confirm path

Session note/PDF issues:
- review-service share flow
- notification-service PDF + R2 upload

## 12) What changed in this task

Implemented feature: **Lunch per day basis (Mon..Sun) without cross-day corruption**.

Also ensured:
- backward-safe fallback for legacy lunch data
- frontend now consumes day-wise lunch mappings
- backend availability and scheduler logic align with day-wise lunch model

