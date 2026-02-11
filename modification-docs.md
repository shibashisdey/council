# Modification Docs

This file summarizes the changes applied after the project review and where they were made.

## 1) Internal Service Authentication (JWT)
Added a shared internal JWT and enforced it on internal endpoints. All inter-service calls now attach an internal `Authorization: Bearer <token>` header.

**Added internal auth filter + JWT service**
- `appointment-service/src/main/java/com/council/appointmentservice/security/InternalAuthFilter.java`
- `appointment-service/src/main/java/com/council/appointmentservice/security/InternalJwtService.java`
- `availability-service/src/main/java/com/council/availabilityservice/security/InternalAuthFilter.java`
- `availability-service/src/main/java/com/council/availabilityservice/security/InternalJwtService.java`
- `review-service/src/main/java/com/council/reviewservice/security/InternalAuthFilter.java`
- `review-service/src/main/java/com/council/reviewservice/security/InternalJwtService.java`
- `notification-service/src/main/java/com/council/notificationservice/security/InternalAuthFilter.java`
- `notification-service/src/main/java/com/council/notificationservice/security/InternalJwtService.java`
- `payment-service/src/main/java/com/council/paymentservice/security/InternalAuthFilter.java`
- `payment-service/src/main/java/com/council/paymentservice/security/InternalJwtService.java`
- `user-service/src/main/java/com/council/userservice/security/InternalAuthFilter.java`
- `user-service/src/main/java/com/council/userservice/security/InternalJwtService.java`
- `counselor-service/src/main/java/com/council/counselorservice/security/InternalAuthFilter.java`
- `counselor-service/src/main/java/com/council/counselorservice/security/InternalJwtService.java`

**Internal endpoints now protected**
- Appointment internal endpoints: `appointment-service/src/main/java/com/council/appointmentservice/security/InternalAuthFilter.java`
- Availability internal endpoints: `availability-service/src/main/java/com/council/availabilityservice/security/InternalAuthFilter.java`
- Review internal endpoints: `review-service/src/main/java/com/council/reviewservice/security/InternalAuthFilter.java`
- Notification endpoints: `notification-service/src/main/java/com/council/notificationservice/security/InternalAuthFilter.java`
- Payment confirm/fail: `payment-service/src/main/java/com/council/paymentservice/security/InternalAuthFilter.java`
- User public/internal: `user-service/src/main/java/com/council/userservice/security/InternalAuthFilter.java`
- Counselor `GET /counselors/user/{userId}`: `counselor-service/src/main/java/com/council/counselorservice/security/InternalAuthFilter.java`

**Client calls now attach internal JWT**
- `appointment-service/src/main/java/com/council/appointmentservice/client/AvailabilityClientImpl.java`
- `appointment-service/src/main/java/com/council/appointmentservice/client/CounselorClientImpl.java`
- `review-service/src/main/java/com/council/reviewservice/client/AppointmentClientImpl.java`
- `review-service/src/main/java/com/council/reviewservice/client/NotificationClientImpl.java`
- `review-service/src/main/java/com/council/reviewservice/client/CounselorClientImpl.java`
- `notification-service/src/main/java/com/council/notificationservice/client/AppointmentClientImpl.java`
- `notification-service/src/main/java/com/council/notificationservice/client/ReviewClientImpl.java`
- `notification-service/src/main/java/com/council/notificationservice/client/UserClientImpl.java`
- `notification-service/src/main/java/com/council/notificationservice/client/CounselorClientImpl.java`
- `payment-service/src/main/java/com/council/paymentservice/client/AppointmentClientImpl.java`

**Internal JWT configuration**
- Added `internal.jwt.secret` and `internal.jwt.expiration.ms` to:
  - `appointment-service/src/main/resources/application.properties`
  - `availability-service/src/main/resources/application.properties`
  - `review-service/src/main/resources/application.properties`
  - `notification-service/src/main/resources/application.properties`
  - `payment-service/src/main/resources/application.properties`
  - `user-service/src/main/resources/application.properties`
  - `counselor-service/src/main/resources/application.properties`

**Dependencies**
- Added `io.jsonwebtoken` dependencies to:
  - `appointment-service/pom.xml`
  - `availability-service/pom.xml`
  - `review-service/pom.xml`
  - `notification-service/pom.xml`
  - `payment-service/pom.xml`
  - `user-service/pom.xml`
  - `counselor-service/pom.xml`

## 2) Counselor ID Semantics (Use Counselor Profile ID)
Standardized counselor IDs to always mean counselor profile ID, not auth user ID.

**Appointment data and docs**
- `appointment-service/src/main/java/com/council/appointmentservice/model/Appointment.java`
- `appointment-service/src/main/java/com/council/appointmentservice/dto/request/CreateAppointmentRequest.java`
- `README.md`
- `docs/ARCHITECTURE.md`

**Authorization mapping for counselor endpoints**
- `appointment-service/src/main/java/com/council/appointmentservice/controller/AppointmentController.java`
- `appointment-service/src/main/java/com/council/appointmentservice/service/AppointmentServiceImpl.java`
- `review-service/src/main/java/com/council/reviewservice/controller/ReviewController.java`
- `review-service/src/main/java/com/council/reviewservice/controller/SessionNoteController.java`

**New counselor lookup DTOs/clients**
- `appointment-service/src/main/java/com/council/appointmentservice/client/CounselorClient.java`
- `appointment-service/src/main/java/com/council/appointmentservice/client/CounselorClientImpl.java`
- `appointment-service/src/main/java/com/council/appointmentservice/dto/response/CounselorResponse.java`
- `review-service/src/main/java/com/council/reviewservice/client/CounselorClient.java`
- `review-service/src/main/java/com/council/reviewservice/client/CounselorClientImpl.java`
- `review-service/src/main/java/com/council/reviewservice/dto/response/CounselorResponse.java`
- `notification-service/src/main/java/com/council/notificationservice/client/CounselorClient.java`
- `notification-service/src/main/java/com/council/notificationservice/client/CounselorClientImpl.java`
- `notification-service/src/main/java/com/council/notificationservice/service/NotificationServiceImpl.java`

**New downstream configuration**
- `appointment-service/src/main/resources/application.properties` (`appointment.counselor.base-url`)
- `review-service/src/main/resources/application.properties` (`review.counselor.base-url`)

## 3) Appointment Reschedule State Fix
Reschedule now preserves the original status instead of forcing `RESCHEDULED`, avoiding broken payment flows.
- `appointment-service/src/main/java/com/council/appointmentservice/service/AppointmentServiceImpl.java`

## 4) Gateway JWT Parsing Fix
Avoids `ClassCastException` by parsing `userId` from claims as `Number`.
- `api-gateway/src/main/java/com/council/apigateway/security/JwtAuthenticationFilter.java`

## 5) Gateway Routes + Docs Alignment
Added missing gateway routes and updated documentation/ports.
- `api-gateway/src/main/resources/application.properties`
- `README.md`

## 6) Internal Access Header Removal
Removed spoofable `X-INTERNAL-CALL` check on user public endpoint (now enforced by internal JWT filter).
- `user-service/src/main/java/com/council/userservice/controller/UserController.java`

## 7) Dev Payment Endpoint
Added a dev-only endpoint to simulate payment success for the frontend.
- `payment-service/src/main/java/com/council/paymentservice/controller/PaymentController.java`

## 8) Eureka Server Settings
No change applied. `eureka.client.register-with-eureka` and `eureka.client.fetch-registry` remain `false` per request.

## 9) Frontend (Angular)
Built end-to-end Angular UI in `product-webapp/webapp` using the existing app:
- Added dashboard UI and wiring:
  - `product-webapp/webapp/src/app/pages/dashboard/dashboard.component.ts`
  - `product-webapp/webapp/src/app/pages/dashboard/dashboard.component.html`
  - `product-webapp/webapp/src/app/pages/dashboard/dashboard.component.css`
- Added auth state + guard:
  - `product-webapp/webapp/src/app/core/auth-state.service.ts`
  - `product-webapp/webapp/src/app/core/auth.guard.ts`
- Added API services:
  - `product-webapp/webapp/src/app/services/user.service.ts`
  - `product-webapp/webapp/src/app/services/counselor.service.ts`
  - `product-webapp/webapp/src/app/services/availability.service.ts`
  - `product-webapp/webapp/src/app/services/appointment.service.ts`
  - `product-webapp/webapp/src/app/services/payment.service.ts`
  - `product-webapp/webapp/src/app/services/review.service.ts`
  - `product-webapp/webapp/src/app/services/session-note.service.ts`
- Updated app wiring:
  - `product-webapp/webapp/src/app/app.module.ts`
  - `product-webapp/webapp/src/app/app-routing.module.ts`
  - `product-webapp/webapp/src/app/app.component.ts`
  - `product-webapp/webapp/src/app/app.component.html`
  - `product-webapp/webapp/src/app/app.component.css`
  - `product-webapp/webapp/src/app/auth/auth.service.ts`
  - `product-webapp/webapp/src/app/auth/login/login.component.ts`
- Updated environment API base:
  - `product-webapp/webapp/src/environments/environment.ts`
  - `product-webapp/webapp/src/environments/environment.prod.ts`

## 10) Frontend Upgrade to Angular 18
Upgraded the Angular app to run on Node 24 and latest Angular/Material.
- `product-webapp/webapp/package.json` (Angular/Material/RxJS/TypeScript versions, removed deprecated tooling)
- `product-webapp/webapp/angular.json` (removed tslint/protractor targets)
- `product-webapp/webapp/tsconfig.json` (modern target/module)
- `product-webapp/webapp/src/polyfills.ts` (zone.js import update)
