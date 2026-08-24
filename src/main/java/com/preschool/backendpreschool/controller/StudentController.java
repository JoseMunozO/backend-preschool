package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.StudentConsentRequest;
import com.preschool.backendpreschool.dto.StudentConsentResponse;
import com.preschool.backendpreschool.dto.StudentEmergencyContactRequest;
import com.preschool.backendpreschool.dto.StudentEmergencyContactResponse;
import com.preschool.backendpreschool.dto.StudentHealthReportEntryResponse;
import com.preschool.backendpreschool.dto.StudentNoteAuditLogResponse;
import com.preschool.backendpreschool.dto.StudentNoteHistoryEntryResponse;
import com.preschool.backendpreschool.dto.StudentNoteRequest;
import com.preschool.backendpreschool.dto.StudentNoteResponse;
import com.preschool.backendpreschool.dto.StudentRequest;
import com.preschool.backendpreschool.dto.StudentResponse;
import com.preschool.backendpreschool.dto.StudentGuardianResponse;
import com.preschool.backendpreschool.model.StudentStatus;
import com.preschool.backendpreschool.service.StudentConsentService;
import com.preschool.backendpreschool.service.StudentEmergencyContactService;
import com.preschool.backendpreschool.service.StudentGuardianService;
import com.preschool.backendpreschool.service.StudentNoteService;
import com.preschool.backendpreschool.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final StudentGuardianService studentGuardianService;
    private final StudentNoteService studentNoteService;
    private final StudentConsentService studentConsentService;
    private final StudentEmergencyContactService studentEmergencyContactService;

    @GetMapping
    public List<StudentResponse> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) Boolean includeDeleted
    ) {
        return studentService.getStudents(search, groupId, status, includeDeleted);
    }

    @GetMapping("/{id}")
    public StudentResponse getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @GetMapping("/reports/health")
    public List<StudentHealthReportEntryResponse> getHealthReport(@RequestParam(required = false) Long groupId) {
        return studentService.getHealthReport(groupId);
    }

    @GetMapping("/{id}/guardians")
    public List<StudentGuardianResponse> getStudentGuardians(@PathVariable Long id) {
        return studentGuardianService.getGuardiansByStudent(id);
    }

    @GetMapping("/{id}/emergency-contacts")
    public List<StudentEmergencyContactResponse> getStudentEmergencyContacts(@PathVariable Long id) {
        return studentEmergencyContactService.getContacts(id);
    }

    @PostMapping("/{id}/emergency-contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentEmergencyContactResponse createStudentEmergencyContact(
            @PathVariable Long id,
            @Valid @RequestBody StudentEmergencyContactRequest request
    ) {
        return studentEmergencyContactService.createContact(id, request);
    }

    @PutMapping("/{id}/emergency-contacts/{contactId}")
    public StudentEmergencyContactResponse updateStudentEmergencyContact(
            @PathVariable Long id,
            @PathVariable Long contactId,
            @Valid @RequestBody StudentEmergencyContactRequest request
    ) {
        return studentEmergencyContactService.updateContact(id, contactId, request);
    }

    @DeleteMapping("/{id}/emergency-contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudentEmergencyContact(
            @PathVariable Long id,
            @PathVariable Long contactId
    ) {
        studentEmergencyContactService.deleteContact(id, contactId);
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

    @GetMapping("/{id}/reports/notes-history")
    public List<StudentNoteHistoryEntryResponse> getStudentNotesHistory(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return studentNoteService.getNotesHistory(id, authentication.getName());
    }

    @GetMapping("/{id}/notes/{noteId}/audit-log")
    public List<StudentNoteAuditLogResponse> getStudentNoteAuditLog(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication authentication
    ) {
        return studentNoteService.getAuditLog(id, noteId, authentication.getName());
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

    @PostMapping("/{id}/restore")
    public StudentResponse restoreStudent(@PathVariable Long id) {
        return studentService.restoreStudent(id);
    }

    @PutMapping(value = "/{id}/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentResponse updateProfilePhoto(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return studentService.updateProfilePhoto(id, file);
    }

    @DeleteMapping("/{id}/profile-photo")
    public StudentResponse removeProfilePhoto(@PathVariable Long id) {
        return studentService.removeProfilePhoto(id);
    }
}
