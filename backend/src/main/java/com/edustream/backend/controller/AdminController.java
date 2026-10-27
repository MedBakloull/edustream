package com.edustream.backend.controller;

import com.edustream.backend.model.Enrollment;
import com.edustream.backend.model.Payment;
import com.edustream.backend.model.Student;
import com.edustream.backend.repository.CourseRepository;
import com.edustream.backend.repository.EnrollmentRepository;
import com.edustream.backend.repository.PaymentRepository;
import com.edustream.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Value("${admin.password:mysuperdupercoopersecret}")
    private String adminPassword;

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;

    public AdminController(StudentRepository studentRepository,
                           CourseRepository courseRepository,
                           EnrollmentRepository enrollmentRepository,
                           PaymentRepository paymentRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (password == null || !password.equals(adminPassword)) {
            // Check if student with ADMIN role exists with this password
            Student student = studentRepository.findByEmail(email).orElse(null);
            if (student == null || !"ADMIN".equalsIgnoreCase(student.getRole()) || !student.getPassword().equals(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Mot de passe administrateur incorrect"));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "role", "ADMIN",
                "email", email != null ? email : "admin@edustream.ma",
                "message", "Authentification administrateur réussie"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        long totalStudents = studentRepository.count();
        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        List<Payment> completedPayments = paymentRepository.findAll().stream()
                .filter(p -> "completed".equalsIgnoreCase(p.getStatus()))
                .toList();

        double totalRevenue = completedPayments.stream()
                .mapToDouble(Payment::getAmount)
                .sum();

        return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "totalCourses", totalCourses,
                "totalEnrollments", totalEnrollments,
                "totalRevenue", totalRevenue,
                "currency", "DH"
        ));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentRepository.findAll());
    }

    @RequestMapping(value = "/reset-database", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> resetDatabase() {
        // 1. Vider les inscriptions et les paiements
        enrollmentRepository.deleteAll();
        paymentRepository.deleteAll();

        // 2. Supprimer les milliers d'utilisateurs temporaires crees par JMeter
        studentRepository.deleteAll();

        // 3. Reinitialiser et augmenter les places de tous les cours a 1 000 000 places (illimite)
        List<com.edustream.backend.model.Course> courses = courseRepository.findAll();
        for (com.edustream.backend.model.Course c : courses) {
            c.setTotalSeats(1000000);
            c.setAvailableSeats(1000000);
            courseRepository.save(c);
        }

        // 4. Restaurer les etudiants officiels de demonstration
        studentRepository.save(new Student("demo-student", "jean@example.com", "Jean", "Dupont", "password123"));
        studentRepository.save(new Student("db8e3596", "salama.harbal@example.ma", "Salama", "Harbal", "password123"));
        studentRepository.save(new Student("admin-001", "admin@edustream.ma", "System", "Admin", adminPassword, "ADMIN"));

        int reactSeats = courseRepository.findById("react")
                .map(com.edustream.backend.model.Course::getAvailableSeats)
                .orElse(1000000);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Base de données nettoyée avec succès ! Les places de tous les cours ont été portées à 1 000 000 (illimité).",
                "availableSeatsReact", reactSeats,
                "totalCourses", courseRepository.count()
        ));
    }
}
