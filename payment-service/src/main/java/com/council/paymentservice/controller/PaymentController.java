package com.council.paymentservice.controller;

import com.council.paymentservice.dto.request.CreatePaymentRequest;
import com.council.paymentservice.dto.response.PaymentResponse;
import com.council.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * CLIENT → Create payment intent
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody CreatePaymentRequest request
    ) {
        return new ResponseEntity<>(
                paymentService.createPayment(request),
                HttpStatus.CREATED
        );
    }

    /**
     * PAYMENT GATEWAY → Payment success callback
     */
    @PostMapping("/{appointmentId}/confirm")
    public ResponseEntity<Void> confirmPayment(
            @PathVariable Long appointmentId,
            @RequestParam String gatewayPaymentId
    ) {
        paymentService.confirmPayment(appointmentId, gatewayPaymentId);
        return ResponseEntity.ok().build();
    }

    /**
     * PAYMENT GATEWAY → Payment failed callback
     */
    @PostMapping("/{appointmentId}/fail")
    public ResponseEntity<Void> failPayment(
            @PathVariable Long appointmentId
    ) {
        paymentService.failPayment(appointmentId);
        return ResponseEntity.ok().build();
    }
}
