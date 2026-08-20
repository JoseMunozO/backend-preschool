package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.StudentConsentRequest;
import com.preschool.backendpreschool.dto.StudentConsentResponse;
import com.preschool.backendpreschool.dto.StudentNoteRequest;
import com.preschool.backendpreschool.dto.StudentNoteResponse;
import com.preschool.backendpreschool.dto.StudentRequest;
import com.preschool.backendpreschool.dto.StudentProfilePhotoRequest;
import com.preschool.backendpreschool.dto.StudentResponse;
import com.preschool.backendpreschool.dto.StudentGuardianResponse;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.service.StudentConsentService;
import com.preschool.backendpreschool.service.StudentGuardianService;
import com.preschool.backendpreschool.service.StudentNoteService;
import com.preschool.backendpreschool.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentGuardianService studentGuardianService;
    private final StudentNoteService studentNoteService;
    private final StudentConsentService studentConsentService;

    @GetMapping
    public List<StudentResponse> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) StudentStatus status
    ) {
        return studentService.getStudents(search, groupId, status);
    }

    @GetMapping("/{id}")
    public StudentResponse getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @GetMapping("/{id}/guardians")
    public List<StudentGuardianResponse> getStudentGuardians(@PathVariable Long id) {
        return studentGuardianService.getGuardiansByStudent(id);
    }

    @GetMapping("/{id}/notes")
    public List<StudentNoteResponse> getStudentNotes(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return studentNoteService.getNotes(id, authentication.getName());
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentNoteResponse createStudentNote(
            @PathVariable Long id,
            @Valid @RequestBody StudentNoteRequest request,
            Authentication authentication
    ) {
        return studentNoteService.createNote(id, request, authentication.getName());
    }

    @PutMapping("/{id}/notes/{noteId}")
    public StudentNoteResponse updateStudentNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            @Valid @RequestBody StudentNoteRequest request,
            Authentication authentication
    ) {
        return studentNoteService.updateNote(id, noteId, request, authentication.getName());
    }

    @PatchMapping("/{id}/notes/{noteId}/moderate")
    public StudentNoteResponse moderateStudentNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication authentication
    ) {
        return studentNoteService.moderateNote(id, noteId, authentication.getName());
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudentNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication authentication
    ) {
        studentNoteService.deleteNote(id, noteId, authentication.getName());
    }

    @GetMapping("/{id}/consents")
    public List<StudentConsentResponse> getStudentConsents(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return studentConsentService.getConsents(id, authentication.getName());
    }

    @PostMapping("/{id}/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentConsentResponse acceptStudentConsent(
            @PathVariable Long id,
            @Valid @RequestBody StudentConsentRequest request,
            Authentication authentication
    ) {
        return studentConsentService.acceptConsent(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/consents/{consentId}/revoke")
    public StudentConsentResponse revokeStudentConsent(
            @PathVariable Long id,
            @PathVariable Long consentId,
            Authentication authentication
    ) {
        return studentConsentService.revokeConsent(id, consentId, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse createStudent(@Valid @RequestBody StudentRequest request) {
        return studentService.createStudent(request);
    }

    @PutMapping("/{id}")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request
    ) {
        return studentService.updateStudent(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }

    @PutMapping("/{id}/profile-photo")
    public StudentResponse updateProfilePhoto(
            @PathVariable Long id,
            @Valid @RequestBody StudentProfilePhotoRequest request
    ) {
        return studentService.updateProfilePhoto(id, request);
    }

    @DeleteMapping("/{id}/profile-photo")
    public StudentResponse removeProfilePhoto(@PathVariable Long id) {
        return studentService.removeProfilePhoto(id);
    }
}
