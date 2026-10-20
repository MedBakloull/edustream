package com.edustream.backend.service;

import com.edustream.backend.dto.CardDataDTO;
import com.edustream.backend.exception.PaymentRefusedException;
import com.edustream.backend.model.Payment;
import com.edustream.backend.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private CardDataDTO validCard;

    @BeforeEach
    void setUp() {
        // Préparation d'une carte valide (selon l'algorithme de Luhn si applicable)
        validCard = new CardDataDTO("4242424242424242", 12, 2030, "123", "Jean Dupont");
    }

    @Test
    void processPayment_SuccessfulTransaction_ReturnsSavedPayment() {
        // Arrange (Préparation)
        String enrollmentId = "enr-123";
        String studentId = "stud-456";
        String courseId = "course-789";
        double amount = 99.99;
        String fakeTransactionId = "TX-SUCCESS-123";

        // Comportement simulé du mock
        when(paymentGateway.processPayment(amount, validCard)).thenReturn(fakeTransactionId);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act (Exécution)
        Payment result = paymentService.processPayment(enrollmentId, studentId, courseId, amount, validCard);

        // Assert (Vérification)
        assertNotNull(result);
        assertEquals("completed", result.getStatus());
        assertEquals(fakeTransactionId, result.getTransactionId());
        assertEquals("4242", result.getCardLast4());
        assertEquals(amount, result.getAmount());

        // Vérifie que repository.save a bien été appelé une fois
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_GatewayThrowsException_SavesFailedPaymentAndThrows() {
        // Arrange
        String enrollmentId = "enr-123";
        String studentId = "stud-456";
        String courseId = "course-789";
        double amount = 99.99;

        // On simule un refus de la banque (Fonds insuffisants par exemple)
        when(paymentGateway.processPayment(amount, validCard))
                .thenThrow(new PaymentRefusedException("Fonds insuffisants"));
        
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        PaymentRefusedException exception = assertThrows(PaymentRefusedException.class, () -> {
            paymentService.processPayment(enrollmentId, studentId, courseId, amount, validCard);
        });

        assertTrue(exception.getMessage().contains("Fonds insuffisants"));

        // Vérifier que le statut 'failed' a bien été enregistré en BDD malgré l'erreur
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());

        Payment savedFailedPayment = paymentCaptor.getValue();
        assertEquals("failed", savedFailedPayment.getStatus());
        assertTrue(savedFailedPayment.getFailureReason().contains("Fonds insuffisants"));
        assertTrue(savedFailedPayment.getTransactionId().startsWith("FAILED-"));
    }

    @Test
    void getPaymentDetails_ReturnsPaymentOrNull() {
        Payment payment = new Payment("p1", "e1", "s1", "c1", 100.0, "completed", "4242", "TX1");
        when(paymentRepository.findById("p1")).thenReturn(Optional.of(payment));
        when(paymentRepository.findById("none")).thenReturn(Optional.empty());

        assertEquals("p1", paymentService.getPaymentDetails("p1").getId());
        assertNull(paymentService.getPaymentDetails("none"));
    }

    @Test
    void getStudentPayments_ReturnsListOfPayments() {
        Payment payment = new Payment("p1", "e1", "s1", "c1", 100.0, "completed", "4242", "TX1");
        when(paymentRepository.findByStudentId("s1")).thenReturn(java.util.List.of(payment));

        java.util.List<Payment> list = paymentService.getStudentPayments("s1");

        assertEquals(1, list.size());
        assertEquals("p1", list.get(0).getId());
    }
}
