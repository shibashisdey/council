package com.council.paymentservice.dto.response;

import com.council.paymentservice.model.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long appointmentId;
    private PaymentStatus status;
}
