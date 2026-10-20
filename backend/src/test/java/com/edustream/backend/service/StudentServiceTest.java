package com.edustream.backend.service;

import com.edustream.backend.dto.StudentProfileDTO;
import com.edustream.backend.exception.EmailAlreadyExistsException;
import com.edustream.backend.exception.InvalidCredentialsException;
import com.edustream.backend.exception.StudentNotFoundException;
import com.edustream.backend.model.Enrollment;
import com.edustream.backend.model.Payment;
import com.edustream.backend.model.Student;
import com.edustream.backend.repository.EnrollmentRepository;
import com.edustream.backend.repository.PaymentRepository;
import com.edustream.backend.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student mockStudent;

    @BeforeEach
    void setUp() {
        mockStudent = new Student("s-123", "youssef@example.ma", "Youssef", "Alami", "secret123");
    }

    @Test
    @DisplayName("Should create a student successfully when email is unique")
    void testCreateStudentSuccess() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = studentService.createStudent("youssef@example.ma", "Youssef", "Alami", "secret123");

        assertNotNull(result);
        assertEquals("youssef@example.ma", result.getEmail());
        assertEquals("Youssef", result.getFirstName());
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is taken")
    void testCreateStudentDuplicateEmail() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));

        assertThrows(EmailAlreadyExistsException.class, () ->
                studentService.createStudent("youssef@example.ma", "Youssef", "Alami", "secret123")
        );

        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    @DisplayName("Should authenticate student with valid credentials")
    void testAuthenticateStudentSuccess() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));

        Student authenticated = studentService.authenticateStudent("youssef@example.ma", "secret123");

        assertNotNull(authenticated);
        assertEquals("s-123", authenticated.getId());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void testAuthenticateStudentInvalidPassword() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));

        assertThrows(InvalidCredentialsException.class, () ->
                studentService.authenticateStudent("youssef@example.ma", "wrongPass")
        );
    }

    @Test
    @DisplayName("Should calculate profile total spent in MAD correctly")
    void testGetStudentProfile() {
        when(studentRepository.findById("s-123")).thenReturn(Optional.of(mockStudent));

        Enrollment enr1 = new Enrollment("e-1", "s-123", "react", "completed");
        Enrollment enr2 = new Enrollment("e-2", "s-123", "data", "pending");
        when(enrollmentRepository.findByStudentId("s-123")).thenReturn(List.of(enr1, enr2));

        Payment p1 = new Payment("p-1", "e-1", "s-123", "react", 1490.0, "completed", "1234", "TXN-1");
        Payment p2 = new Payment("p-2", "e-2", "s-123", "data", 990.0, "failed", "1234", "TXN-2");
        when(paymentRepository.findByStudentId("s-123")).thenReturn(List.of(p1, p2));

        StudentProfileDTO profile = studentService.getStudentProfile("s-123");

        assertNotNull(profile);
        assertEquals("s-123", profile.getId());
        assertEquals(1, profile.getEnrolledCourses().size());
        assertEquals("react", profile.getEnrolledCourses().get(0));
        assertEquals(1490.0, profile.getTotalSpent());
    }

    @Test
    @DisplayName("Should throw StudentNotFoundException when student profile is not found")
    void testGetStudentProfileNotFound() {
        when(studentRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> studentService.getStudentProfile("invalid"));
    }

    @Test
    @DisplayName("Should return student profile when lookup is by email")
    void testGetStudentProfileByEmail() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));
        when(enrollmentRepository.findByStudentId("s-123")).thenReturn(List.of());
        when(paymentRepository.findByStudentId("youssef@example.ma")).thenReturn(List.of());

        StudentProfileDTO profile = studentService.getStudentProfile("youssef@example.ma");

        assertNotNull(profile);
        assertEquals("s-123", profile.getId());
    }

    @Test
    @DisplayName("Should throw StudentNotFoundException when profile lookup by email fails")
    void testGetStudentProfileByEmailNotFound() {
        when(studentRepository.findByEmail("unknown@example.ma")).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class, () -> studentService.getStudentProfile("unknown@example.ma"));
    }

    @Test
    @DisplayName("Should return all students")
    void testGetAllStudents() {
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent));

        List<Student> students = studentService.getAllStudents();

        assertEquals(1, students.size());
        assertEquals("s-123", students.get(0).getId());
    }

    @Test
    @DisplayName("Should update student role to specified role or default to USER")
    void testUpdateStudentRole() {
        when(studentRepository.findById("s-123")).thenReturn(Optional.of(mockStudent));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student adminStudent = studentService.updateStudentRole("s-123", "admin");
        assertEquals("ADMIN", adminStudent.getRole());

        Student defaultStudent = studentService.updateStudentRole("s-123", null);
        assertEquals("USER", defaultStudent.getRole());
    }

    @Test
    @DisplayName("Should update student details successfully")
    void testUpdateStudentSuccess() {
        when(studentRepository.findById("s-123")).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findByEmail("new@example.ma")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student updated = studentService.updateStudent("s-123", "new@example.ma", "NewFirst", "NewLast", "newPass");

        assertEquals("new@example.ma", updated.getEmail());
        assertEquals("NewFirst", updated.getFirstName());
        assertEquals("NewLast", updated.getLastName());
        assertEquals("newPass", updated.getPassword());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when updating student with taken email")
    void testUpdateStudentDuplicateEmail() {
        when(studentRepository.findById("s-123")).thenReturn(Optional.of(mockStudent));
        Student other = new Student("s-456", "taken@example.ma", "Other", "User", "pass");
        when(studentRepository.findByEmail("taken@example.ma")).thenReturn(Optional.of(other));

        assertThrows(EmailAlreadyExistsException.class, () ->
                studentService.updateStudent("s-123", "taken@example.ma", null, null, null)
        );
    }

    @Test
    @DisplayName("Should get student by id or email")
    void testGetStudentById() {
        when(studentRepository.findById("s-123")).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findById("none")).thenReturn(Optional.empty());

        assertEquals("s-123", studentService.getStudentById("s-123").getId());
        assertEquals("s-123", studentService.getStudentById("youssef@example.ma").getId());
        assertThrows(StudentNotFoundException.class, () -> studentService.getStudentById("none"));
    }

    @Test
    @DisplayName("Should find or create student by email")
    void testFindOrCreateByEmail() {
        when(studentRepository.findByEmail("youssef@example.ma")).thenReturn(Optional.of(mockStudent));
        when(studentRepository.findByEmail("new@example.ma")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student found = studentService.findOrCreateByEmail("youssef@example.ma", "Youssef", "Alami");
        assertEquals("s-123", found.getId());

        Student created = studentService.findOrCreateByEmail("new@example.ma", null, null);
        assertNotNull(created);
        assertEquals("new@example.ma", created.getEmail());
        assertEquals("User", created.getFirstName());
    }
}
