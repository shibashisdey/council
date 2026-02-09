package com.council.paymentservice.repository;

import com.council.paymentservice.model.Payment;
import com.council.paymentservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentIdAndStatus(
            Long appointmentId,
            PaymentStatus status
    );
}
