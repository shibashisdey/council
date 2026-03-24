# StackFul Minds

**Overview**
StackFul Minds is a Spring Boot microservices system for therapy sessions. It includes authentication, user and counselor profiles, appointment booking with availability rules, payments, session notes and reviews, notification/PDF delivery via Cloudflare R2, and async email delivery through Kafka.

**Services And Ports**
1. `eureka-server` - `8761`
2. `api-gateway` - `8091`
3. `user-authentication-service` - `8081`
4. `counselor-service` - `8090`
5. `appointment-service` - `8083`
6. `availability-service` - `8085`
7. `link-gererator-service` - `8082`
8. `review-service` - `8086`
9. `notification-service` - `8088`
10. `email-service` - `8092`
11. `payment-service` - `8087`
12. `user-service` - `8084`

**High-Level Flow**
1. Auth service registers and logs in users, issues JWT.
2. API Gateway validates JWT and injects `X-USER-ID`, `X-USER-EMAIL`, `X-USER-ROLE`.
3. User service stores client profiles.
4. Counselor service stores therapist profiles and specializations.
5. Appointment service orchestrates booking and calls availability service.
6. Payment service confirms appointments and triggers meeting-link creation.
7. Review service manages note metadata and review records.
8. Notification service uploads session-note PDFs to R2.
9. Source services publish Kafka events and `email-service` sends mail asynchronously.

**Auth**
1. `POST /auth/register`
2. `POST /auth/login`
3. `GET /auth/users/{userId}/internal` - internal lookup of auth email/role

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
10. `PUT /appointments/{appointmentId}/reschedule/request`
11. `PUT /appointments/{appointmentId}/reschedule/accept`
12. `PUT /appointments/{appointmentId}/reschedule/reject`

Appointment events now published to Kafka:
1. `APPOINTMENT_CREATED`
2. `APPOINTMENT_CANCELLED`
3. `APPOINTMENT_RESCHEDULE_REQUESTED`
4. `APPOINTMENT_RESCHEDULED`
5. `APPOINTMENT_RESCHEDULE_REJECTED`

**Payments**
1. `POST /payments`
2. `POST /payments/{appointmentId}/confirm`
3. `POST /payments/{appointmentId}/fail`
4. `POST /payments/{appointmentId}/simulate-success`

Payment events now published to Kafka:
1. `PAYMENT_CONFIRMED`
2. `PAYMENT_FAILED`

**Review / Notifications**
1. `POST /session-notes/share` - share note content, upload PDF to R2, store metadata + `pdfUrl`
2. `POST /notifications/session-note` - direct note-share pipeline into notification service
3. `notification-service` publishes `SESSION_NOTE_SHARED` to Kafka after PDF upload succeeds

**Email Service**
1. Consumes Kafka topic `council-email-events`
2. Resolves recipients from auth/profile services
3. Sends async SMTP mail for registration, booking, payment, cancellation, reschedule, and session-note-share events

**Configuration**
Kafka:
1. `spring.kafka.bootstrap-servers`
2. `email.kafka.topic`

Email service:
1. `email.sending.enabled`
2. `email.from`
3. `spring.mail.host`
4. `spring.mail.port`
5. `spring.mail.username`
6. `spring.mail.password`

Notification service R2:
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
12. `email-service`

**Notes**
1. `email-service` is safe to start with `email.sending.enabled=false`; it will log instead of sending.
2. A working Kafka broker is required for async mail delivery.
3. The local wrapper build is currently blocked until `JAVA_HOME` points to a valid JDK 17 installation.
