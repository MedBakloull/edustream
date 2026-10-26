package com.edustream.backend.controller;

import com.edustream.backend.dto.PaymentRequestDTO;
import com.edustream.backend.model.Payment;
import com.edustream.backend.service.EnrollmentService;
import com.edustream.backend.service.PaymentService;
import com.edustream.backend.service.UnleashService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final EnrollmentService enrollmentService;
    private final UnleashService unleashService;

    public PaymentController(PaymentService paymentService, EnrollmentService enrollmentService,
                             UnleashService unleashService) {
        this.paymentService = paymentService;
        this.enrollmentService = enrollmentService;
        this.unleashService = unleashService;
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody @Valid PaymentRequestDTO request) {

        // KILL SWITCH: if canary flag is OFF, enrollment is blocked instantly
        if (!unleashService.isCanaryEnrollmentEnabled()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Enrollment is temporarily unavailable. Please try again later.");
            error.put("canary", false);
            error.put("version", "v1-stable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }

        // Flag is ON — process enrollment normally (canary v2)
        String studentIdentifier = request.getStudentId() != null ? request.getStudentId() : request.getStudentEmail();

        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent(
                studentIdentifier,
                request.getCourseId(),
                request.getCardData(),
                null,
                null
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("enrollmentId", result.getEnrollment().getId());
        response.put("paymentId", result.getPaymentId());
        response.put("status", result.getEnrollment().getStatus());
        response.put("courseId", request.getCourseId());
        response.put("canary", true);
        response.put("version", "v2-canary");
        response.put("message", "Paiement et inscription traites avec succes (" + request.getAmount() + " MAD)");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentDetails(@PathVariable String id) {
        Payment payment = paymentService.getPaymentDetails(id);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Payment>> getStudentPayments(@PathVariable String studentId) {
        return ResponseEntity.ok(paymentService.getStudentPayments(studentId));
    }
}
