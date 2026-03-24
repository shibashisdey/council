package com.council.paymentservice.service;

import com.council.paymentservice.client.AppointmentClient;
import com.council.paymentservice.dto.request.CreatePaymentRequest;
import com.council.paymentservice.dto.response.AppointmentInternalResponse;
import com.council.paymentservice.dto.response.AppointmentStatusResponse;
import com.council.paymentservice.dto.response.PaymentResponse;
import com.council.paymentservice.messaging.EmailEventPublisher;
import com.council.paymentservice.messaging.EmailNotificationEvent;
import com.council.paymentservice.model.Payment;
import com.council.paymentservice.model.PaymentStatus;
import com.council.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentClient appointmentClient;
    private final EmailEventPublisher emailEventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            AppointmentClient appointmentClient,
            EmailEventPublisher emailEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.appointmentClient = appointmentClient;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        String appointmentStatus = getAppointmentStatusOrThrow(request.getAppointmentId());
        if (!"PENDING_PAYMENT".equals(appointmentStatus)) {
            throw new IllegalStateException("Appointment is not pending payment");
        }

        Payment payment = paymentRepository.findByAppointmentId(request.getAppointmentId())
                .orElse(null);

        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new IllegalStateException("Payment already completed");
            }

            if (payment.getStatus() == PaymentStatus.INITIATED) {
                return PaymentResponse.builder()
                        .paymentId(payment.getId())
                        .appointmentId(payment.getAppointmentId())
                        .status(payment.getStatus())
                        .build();
            }

            payment.setStatus(PaymentStatus.INITIATED);
            payment.setGatewayPaymentId(null);
            payment.setAmount(request.getAmount());
        } else {
            payment = new Payment();
            payment.setAppointmentId(request.getAppointmentId());
            payment.setAmount(request.getAmount());
            payment.setStatus(PaymentStatus.INITIATED);
        }

        Payment saved = paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(saved.getId())
                .appointmentId(saved.getAppointmentId())
                .status(saved.getStatus())
                .build();
    }

    @Override
    public void confirmPayment(Long appointmentId, String gatewayPaymentId) {

        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return; // idempotent
        }

        String appointmentStatus = getAppointmentStatusOrThrow(appointmentId);
        if (!"PENDING_PAYMENT".equals(appointmentStatus) && !"CONFIRMED".equals(appointmentStatus)) {
            throw new IllegalStateException("Appointment is not in a confirmable state");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayPaymentId(gatewayPaymentId);
        Payment saved = paymentRepository.save(payment);

        // 🔥 THIS IS THE ONLY SIDE EFFECT
        appointmentClient.confirmAppointment(appointmentId);
        publishPaymentEvent("PAYMENT_CONFIRMED", appointmentId, saved.getAmount());
    }

    @Override
    public void failPayment(Long appointmentId) {

        String appointmentStatus = getAppointmentStatusOrThrow(appointmentId);
        if ("CONFIRMED".equals(appointmentStatus)) {
            throw new IllegalStateException("Cannot fail payment for confirmed appointment");
        }

        paymentRepository.findByAppointmentId(appointmentId)
                .ifPresent(payment -> {
                    if (payment.getStatus() != PaymentStatus.SUCCESS) {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        publishPaymentEvent("PAYMENT_FAILED", appointmentId, payment.getAmount());
                    }
                });
    }

    private String getAppointmentStatusOrThrow(Long appointmentId) {
        AppointmentStatusResponse response = appointmentClient.getAppointmentStatus(appointmentId);
        if (response == null || response.getStatus() == null) {
            throw new IllegalStateException("Appointment status unavailable");
        }
        return response.getStatus();
    }

    private void publishPaymentEvent(String eventType, Long appointmentId, java.math.BigDecimal amount) {
        AppointmentInternalResponse appointment = appointmentClient.getAppointmentInternal(appointmentId);
        if (appointment == null) {
            return;
        }
        emailEventPublisher.publish(EmailNotificationEvent.builder()
                .eventType(eventType)
                .occurredAt(Instant.now())
                .appointmentId(appointment.getAppointmentId())
                .clientUserId(appointment.getClientId())
                .counselorId(appointment.getCounselorId())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .amount(amount)
                .build());
    }
}
