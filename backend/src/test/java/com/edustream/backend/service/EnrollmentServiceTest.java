package com.edustream.backend.service;

import com.edustream.backend.dto.CardDataDTO;
import com.edustream.backend.exception.CourseCompletedException;
import com.edustream.backend.exception.EnrollmentAlreadyExistsException;
import com.edustream.backend.model.Course;
import com.edustream.backend.model.Enrollment;
import com.edustream.backend.model.Payment;
import com.edustream.backend.model.Student;
import com.edustream.backend.repository.CourseRepository;
import com.edustream.backend.repository.EnrollmentRepository;
import com.edustream.backend.repository.PaymentRepository;
import com.edustream.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService realPaymentService;
    private EnrollmentService enrollmentService;

    private Student mockStudent;
    private Course mockCourse;
    private CardDataDTO mockCard;

    @BeforeEach
    void setUp() {

        realPaymentService = new PaymentService(paymentGateway, paymentRepository);
        enrollmentService = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, realPaymentService);

        mockStudent = new Student("stud-123", "test@test.com", "Test", "User", "pass");
        
        mockCourse = new Course();
        mockCourse.setId("course-123");
        mockCourse.setPrice(99.99);
        mockCourse.setAvailableSeats(50);
        
        mockCard = new CardDataDTO("1234567812345678", 12, 2030, "123", "Test User");
    }

    @Test
    void enrollStudent_CourseFull_ThrowsCourseCompletedException() {
        // Arrange
        mockCourse.setAvailableSeats(0); // Plus de places
        
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));

        // Act & Assert
        assertThrows(CourseCompletedException.class, () -> {
            enrollmentService.enrollStudent("stud-123", "course-123", mockCard, null, null);
        });

        verify(paymentGateway, never()).processPayment(anyDouble(), any());
    }

    @Test
    void enrollStudent_AlreadyEnrolled_ThrowsEnrollmentAlreadyExistsException() {
        // Arrange
        Enrollment existingEnrollment = new Enrollment("enr-123", "stud-123", "course-123", "completed");
        
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123"))
                .thenReturn(Optional.of(existingEnrollment));

        // Act & Assert
        assertThrows(EnrollmentAlreadyExistsException.class, () -> {
            enrollmentService.enrollStudent("stud-123", "course-123", mockCard, null, null);
        });
    }

    @Test
    void enrollStudent_SuccessfulEnrollment_DecrementsCourseSeats() {
        // Arrange
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.empty());
        
        // Mock the gateway to succeed
        when(paymentGateway.processPayment(eq(99.99), eq(mockCard))).thenReturn("TX123");
        
        // Mock the repositories saves
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent("stud-123", "course-123", mockCard, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.getEnrollment().getStatus());
        
        // Vérifie que les places ont été décrémentées (50 -> 49)
        assertEquals(49, mockCourse.getAvailableSeats());
        verify(courseRepository, times(1)).save(mockCourse);
    }

    @Test
    void enrollStudent_WithEmailExistingStudent_Success() {
        when(studentRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.empty());
        when(paymentGateway.processPayment(eq(99.99), eq(mockCard))).thenReturn("TX123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent("test@test.com", "course-123", mockCard, "Test", "User");

        assertNotNull(result);
        assertEquals("completed", result.getEnrollment().getStatus());
        assertNotNull(result.getPaymentId());
    }

    @Test
    void enrollStudent_WithEmailNewStudent_CreatesAndEnrolls() {
        when(studentRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId(any(), eq("course-123"))).thenReturn(Optional.empty());
        when(paymentGateway.processPayment(eq(99.99), eq(mockCard))).thenReturn("TX-NEW");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent("new@test.com", "course-123", mockCard, "Nouveau", "Etudiant");

        assertNotNull(result);
        assertEquals("completed", result.getEnrollment().getStatus());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void enrollStudent_StudentNotFound_ThrowsStudentNotFoundException() {
        when(studentRepository.findById("unknown-id")).thenReturn(Optional.empty());

        assertThrows(com.edustream.backend.exception.StudentNotFoundException.class, () -> {
            enrollmentService.enrollStudent("unknown-id", "course-123", mockCard, null, null);
        });
    }

    @Test
    void enrollStudent_CourseNotFound_ThrowsCourseNotFoundException() {
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("unknown-course")).thenReturn(Optional.empty());

        assertThrows(com.edustream.backend.exception.CourseNotFoundException.class, () -> {
            enrollmentService.enrollStudent("stud-123", "unknown-course", mockCard, null, null);
        });
    }

    @Test
    void enrollStudent_ExistingPendingEnrollment_ReusesEnrollment() {
        Enrollment pendingEnrollment = new Enrollment("enr-pending", "stud-123", "course-123", "pending");
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.of(pendingEnrollment));
        when(paymentGateway.processPayment(eq(99.99), eq(mockCard))).thenReturn("TX-PENDING");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent("stud-123", "course-123", mockCard, null, null);

        assertNotNull(result);
        assertEquals("completed", result.getEnrollment().getStatus());
        assertEquals("enr-pending", result.getEnrollment().getId());
    }

    @Test
    void enrollStudent_PaymentThrowsException_MarksEnrollmentFailedAndThrows() {
        when(studentRepository.findById("stud-123")).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById("course-123")).thenReturn(Optional.of(mockCourse));
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.empty());
        when(paymentGateway.processPayment(eq(99.99), eq(mockCard))).thenThrow(new RuntimeException("Payment Error"));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(RuntimeException.class, () -> {
            enrollmentService.enrollStudent("stud-123", "course-123", mockCard, null, null);
        });

        // Verify that enrollment status was saved as failed
        verify(enrollmentRepository, atLeastOnce()).save(argThat(e -> "failed".equals(e.getStatus())));
    }

    @Test
    void getStudentEnrollments_ById_ReturnsEnrollments() {
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findByStudentId("stud-123")).thenReturn(java.util.List.of(e));

        java.util.List<Enrollment> list = enrollmentService.getStudentEnrollments("stud-123");

        assertEquals(1, list.size());
        assertEquals("e1", list.get(0).getId());
    }

    @Test
    void getStudentEnrollments_ByEmail_ResolvesStudentAndReturnsEnrollments() {
        when(studentRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockStudent));
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findByStudentId("stud-123")).thenReturn(java.util.List.of(e));

        java.util.List<Enrollment> list = enrollmentService.getStudentEnrollments("test@test.com");

        assertEquals(1, list.size());
        assertEquals("e1", list.get(0).getId());
    }

    @Test
    void isStudentEnrolled_ReturnsTrueWhenCompleted() {
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.of(e));

        boolean enrolled = enrollmentService.isStudentEnrolled("stud-123", "course-123");

        assertTrue(enrolled);
    }

    @Test
    void isStudentEnrolled_WithEmail_ReturnsTrueWhenCompleted() {
        when(studentRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockStudent));
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findByStudentIdAndCourseId("stud-123", "course-123")).thenReturn(Optional.of(e));

        boolean enrolled = enrollmentService.isStudentEnrolled("test@test.com", "course-123");

        assertTrue(enrolled);
    }

    @Test
    void isStudentEnrolled_WithUnknownEmail_ReturnsFalse() {
        when(studentRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        boolean enrolled = enrollmentService.isStudentEnrolled("unknown@test.com", "course-123");

        assertFalse(enrolled);
    }

    @Test
    void getCourseEnrollments_ReturnsList() {
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findByCourseId("course-123")).thenReturn(java.util.List.of(e));

        java.util.List<Enrollment> list = enrollmentService.getCourseEnrollments("course-123");

        assertEquals(1, list.size());
    }

    @Test
    void getEnrollmentDetails_ReturnsEnrollmentOrNull() {
        Enrollment e = new Enrollment("e1", "stud-123", "course-123", "completed");
        when(enrollmentRepository.findById("e1")).thenReturn(Optional.of(e));
        when(enrollmentRepository.findById("none")).thenReturn(Optional.empty());

        assertEquals("e1", enrollmentService.getEnrollmentDetails("e1").getId());
        assertNull(enrollmentService.getEnrollmentDetails("none"));
    }
}
