package com.council.paymentservice.service;

import com.council.paymentservice.dto.request.CreatePaymentRequest;
import com.council.paymentservice.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    void confirmPayment(Long appointmentId, String gatewayPaymentId);

    void failPayment(Long appointmentId);
}
