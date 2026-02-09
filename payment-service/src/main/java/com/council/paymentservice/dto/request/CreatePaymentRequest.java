package com.council.paymentservice.dto.request;

import lombok.Getter;

import java.math.BigDecimal;
@Getter
public class CreatePaymentRequest {
    private Long appointmentId;
    private BigDecimal amount;
}
