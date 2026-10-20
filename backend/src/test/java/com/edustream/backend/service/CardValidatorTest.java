package com.edustream.backend.service;

import com.edustream.backend.dto.CardDataDTO;
import com.edustream.backend.exception.InvalidCardException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardValidatorTest {

    @Test
    void validateCardData_ValidCard_DoesNotThrow() {
        CardDataDTO validCard = new CardDataDTO("1234567812345678", 12, 2026, "123", "Jean Dupont");
        assertDoesNotThrow(() -> CardValidator.validateCardData(validCard));
    }

    @Test
    void validateCardData_NullCard_ThrowsException() {
        InvalidCardException exception = assertThrows(InvalidCardException.class, 
            () -> CardValidator.validateCardData(null));
        assertTrue(exception.getMessage().contains("requises"));
    }

    @Test
    void validateCardData_InvalidCardNumberLength_ThrowsException() {
        CardDataDTO invalidCard = new CardDataDTO("123", 12, 2026, "123", "Jean Dupont");
        InvalidCardException exception = assertThrows(InvalidCardException.class, 
            () -> CardValidator.validateCardData(invalidCard));
        assertTrue(exception.getMessage().contains("16 chiffres"));
    }

    @Test
    void validateCardData_InvalidExpiryMonth_ThrowsException() {
        CardDataDTO invalidCard = new CardDataDTO("1234567812345678", 13, 2026, "123", "Jean Dupont");
        InvalidCardException exception = assertThrows(InvalidCardException.class, 
            () -> CardValidator.validateCardData(invalidCard));
        assertTrue(exception.getMessage().contains("Mois d'expiration invalide"));
    }

    @Test
    void validateCardData_InvalidExpiryYear_ThrowsException() {
        CardDataDTO invalidCard = new CardDataDTO("1234567812345678", 12, 2020, "123", "Jean Dupont");
        InvalidCardException exception = assertThrows(InvalidCardException.class, 
            () -> CardValidator.validateCardData(invalidCard));
        assertTrue(exception.getMessage().contains("expiration invalide"));
    }

    @Test
    void validateCardData_InvalidCvv_ThrowsException() {
        CardDataDTO invalidCard = new CardDataDTO("1234567812345678", 12, 2026, "12", "Jean Dupont");
        InvalidCardException exception = assertThrows(InvalidCardException.class, 
            () -> CardValidator.validateCardData(invalidCard));
        assertTrue(exception.getMessage().contains("CVV invalide"));
    }

    @Test
    void maskCardNumber_ValidNumber_ReturnsLast4Digits() {
        String masked = CardValidator.maskCardNumber("1234 5678 1234 5678");
        assertEquals("5678", masked);
    }
    
    @Test
    void maskCardNumber_ShortNumber_ReturnsUnmasked() {
        String masked = CardValidator.maskCardNumber("12");
        assertEquals("12", masked);
    }
}
