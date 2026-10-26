package com.edustream.backend.controller;

import com.edustream.backend.dto.PaymentRequestDTO;
import com.edustream.backend.model.Payment;
import com.edustream.backend.service.EnrollmentService;
import com.edustream.backend.service.PaymentService;
import com.edustream.backend.service.UnleashService;
import jakarta.validation.Valid;
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
        String studentIdentifier = request.getStudentId() != null ? request.getStudentId() : request.getStudentEmail();

        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent(
                studentIdentifier,
                request.getCourseId(),
                request.getCardData(),
                null,
                null
        );

        boolean canary = unleashService.isCanaryEnrollmentEnabled();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("enrollmentId", result.getEnrollment().getId());
        response.put("paymentId", result.getPaymentId());
        response.put("status", result.getEnrollment().getStatus());
        response.put("courseId", request.getCourseId());
        response.put("canary", canary);
        response.put("version", canary ? "v2-canary" : "v1-stable");
        response.put("message", canary
                ? "Paiement confirme [Canary v2 - traitement instantane] (" + request.getAmount() + " MAD)"
                : "Paiement et inscription traites avec succes (" + request.getAmount() + " MAD)");
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
