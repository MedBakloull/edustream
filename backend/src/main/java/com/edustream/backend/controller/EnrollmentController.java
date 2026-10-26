package com.edustream.backend.controller;

import com.edustream.backend.dto.EnrollmentRequestDTO;
import com.edustream.backend.model.Enrollment;
import com.edustream.backend.service.CourseService;
import com.edustream.backend.service.EnrollmentService;
import com.edustream.backend.service.StudentService;
import com.edustream.backend.service.UnleashService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final UnleashService unleashService;

    public EnrollmentController(EnrollmentService enrollmentService, StudentService studentService,
                                CourseService courseService, UnleashService unleashService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.courseService = courseService;
        this.unleashService = unleashService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> enroll(@RequestBody @Valid EnrollmentRequestDTO request) {
        String studentIdentifier = request.getStudentId() != null ? request.getStudentId() : request.getStudentEmail();
        if (studentIdentifier == null || studentIdentifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email ou ID étudiant requis"));
        }

        // Verify course exists
        courseService.getCourseDetails(request.getCourseId());

        if (request.getCardData() == null) {
            // Step 1: Pre-registration / student info check
            studentService.findOrCreateByEmail(studentIdentifier, request.getFirstName(), request.getLastName());
            if (enrollmentService.isStudentEnrolled(studentIdentifier, request.getCourseId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "L'étudiant est déjà inscrit à ce cours",
                        "alreadyEnrolled", true,
                        "studentEmail", studentIdentifier,
                        "courseId", request.getCourseId()
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pré-inscription validée avec succès",
                    "studentEmail", studentIdentifier,
                    "courseId", request.getCourseId()
            ));
        }

        // Step 2: Full enrollment with payment
        EnrollmentService.EnrollmentResult result = enrollmentService.enrollStudent(
                studentIdentifier,
                request.getCourseId(),
                request.getCardData(),
                request.getFirstName(),
                request.getLastName()
        );

        boolean canary = unleashService.isCanaryEnrollmentEnabled();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("enrollment", result.getEnrollment());
        response.put("paymentId", result.getPaymentId());
        response.put("enrollmentId", result.getEnrollment().getId());
        response.put("canary", canary);
        response.put("version", canary ? "v2-canary" : "v1-stable");
        response.put("message", canary
                ? "✅ Enrollment confirmed! [Canary v2 — instant processing enabled]"
                : "✅ Enrollment confirmed!");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> getEnrollmentDetails(@PathVariable String id) {
        Enrollment enrollment = enrollmentService.getEnrollmentDetails(id);
        if (enrollment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(enrollment);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrollment>> getStudentEnrollments(@PathVariable String studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> getCourseEnrollments(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId));
    }
}
