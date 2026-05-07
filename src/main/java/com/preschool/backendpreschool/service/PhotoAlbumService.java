package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.PhotoAlbumPhotoRequest;
import com.preschool.backendpreschool.dto.PhotoAlbumPhotoResponse;
import com.preschool.backendpreschool.dto.PhotoAlbumRequest;
import com.preschool.backendpreschool.dto.PhotoAlbumResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.ClassGroup;
import com.preschool.backendpreschool.model.PhotoAlbum;
import com.preschool.backendpreschool.model.PhotoAlbumPhoto;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.StaffGroupAssignment;
import com.preschool.backendpreschool.model.Student;
import com.preschool.backendpreschool.model.StudentConsentType;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.repository.ClassGroupRepository;
import com.preschool.backendpreschool.repository.PhotoAlbumPhotoRepository;
import com.preschool.backendpreschool.repository.PhotoAlbumRepository;
import com.preschool.backendpreschool.repository.StaffGroupAssignmentRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.StudentConsentRepository;
import com.preschool.backendpreschool.repository.StudentRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PhotoAlbumService {

    private final PhotoAlbumRepository photoAlbumRepository;
    private final PhotoAlbumPhotoRepository photoAlbumPhotoRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StudentRepository studentRepository;
    private final StudentConsentRepository studentConsentRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StaffGroupAssignmentRepository staffGroupAssignmentRepository;

    public List<PhotoAlbumResponse> getAlbums(Long groupId, Long studentId, String requesterEmail) {
        User requester = findUser(requesterEmail);
        List<PhotoAlbum> albums;

        if (studentId != null) {
            albums = photoAlbumRepository.findByStudentStudentIdAndActiveTrueOrderByEventDateDescCreatedAtDesc(studentId);
        } else if (groupId != null) {
            albums = photoAlbumRepository.findByClassGroupGroupIdAndActiveTrueOrderByEventDateDescCreatedAtDesc(groupId);
        } else {
            albums = photoAlbumRepository.findByActiveTrueOrderByEventDateDescCreatedAtDesc();
        }

        return albums.stream()
                .filter(album -> canAccessAlbum(requester, album))
                .map(this::toResponseWithPhotos)
                .toList();
    }

    public PhotoAlbumResponse getAlbum(Long albumId, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        return toResponseWithPhotos(album);
    }

    @Transactional
    public PhotoAlbumResponse createAlbum(PhotoAlbumRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        ClassGroup group = findGroup(request.groupId());
        Student student = findStudent(request.studentId());

        ensureAlbumScopeIsCoherent(group, student);
        ensurePhotoConsentIfStudentScoped(student);
        ensureCanWriteAlbum(requester, group, student);

        PhotoAlbum album = PhotoAlbum.builder()
                .title(request.title().trim())
                .description(trimToNull(request.description()))
                .classGroup(group)
                .student(student)
                .createdByUser(requester)
                .eventDate(request.eventDate())
                .active(true)
                .build();

        return toResponseWithPhotos(photoAlbumRepository.save(album));
    }

    @Transactional
    public PhotoAlbumResponse updateAlbum(Long albumId, PhotoAlbumRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        ClassGroup group = findGroup(request.groupId());
        Student student = findStudent(request.studentId());
        ensureAlbumScopeIsCoherent(group, student);
        ensurePhotoConsentIfStudentScoped(student);
        ensureCanWriteAlbum(requester, group, student);

        album.setTitle(request.title().trim());
        album.setDescription(trimToNull(request.description()));
        album.setClassGroup(group);
        album.setStudent(student);
        album.setEventDate(request.eventDate());

        return toResponseWithPhotos(photoAlbumRepository.save(album));
    }

    @Transactional
    public void deleteAlbum(Long albumId, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        album.setActive(false);
        photoAlbumRepository.save(album);
    }

    @Transactional
    public PhotoAlbumPhotoResponse addPhoto(Long albumId, PhotoAlbumPhotoRequest request, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        Student taggedStudent = findStudent(request.studentId());
        Student consentStudent = taggedStudent != null ? taggedStudent : album.getStudent();
        ensurePhotoConsentIfStudentScoped(consentStudent);
        ensurePhotoStudentFitsAlbum(album, taggedStudent);

        PhotoAlbumPhoto photo = PhotoAlbumPhoto.builder()
                .photoAlbum(album)
                .student(taggedStudent)
                .uploadedByUser(requester)
                .photoUrl(request.photoUrl().trim())
                .caption(trimToNull(request.caption()))
                .approved(hasInternalAdminRole(requester))
                .deleted(false)
                .build();

        return toPhotoResponse(photoAlbumPhotoRepository.save(photo));
    }

    @Transactional
    public PhotoAlbumPhotoResponse approvePhoto(Long albumId, Long photoId, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        PhotoAlbumPhoto photo = findActivePhoto(photoId);
        ensurePhotoBelongsToAlbum(photo, albumId);

        photo.setApproved(true);
        return toPhotoResponse(photoAlbumPhotoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(Long albumId, Long photoId, String requesterEmail) {
        User requester = findUser(requesterEmail);
        PhotoAlbum album = findActiveAlbum(albumId);
        ensureCanAccessAlbum(requester, album);

        PhotoAlbumPhoto photo = findActivePhoto(photoId);
        ensurePhotoBelongsToAlbum(photo, albumId);

        photo.setDeleted(true);
        photo.setDeletedAt(LocalDateTime.now());
        photoAlbumPhotoRepository.save(photo);
    }

    private void ensurePhotoConsentIfStudentScoped(Student student) {
        if (student == null) {
            return;
        }

        boolean hasConsent = studentConsentRepository.existsByStudentStudentIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                student.getStudentId(),
                StudentConsentType.PHOTO_ALBUM
        );
        if (!hasConsent) {
            throw new BadRequestException("Se requiere consentimiento activo PHOTO_ALBUM para usar fotos de este estudiante");
        }
    }

    private void ensureAlbumScopeIsCoherent(ClassGroup group, Student student) {
        if (group != null && student != null && student.getClassGroup() != null
                && !student.getClassGroup().getGroupId().equals(group.getGroupId())) {
            throw new BadRequestException("El estudiante no pertenece al grupo indicado");
        }
    }

    private void ensurePhotoStudentFitsAlbum(PhotoAlbum album, Student taggedStudent) {
        if (taggedStudent == null) {
            return;
        }

        if (album.getStudent() != null && !album.getStudent().getStudentId().equals(taggedStudent.getStudentId())) {
            throw new BadRequestException("La foto no corresponde al estudiante del album");
        }

        if (album.getClassGroup() != null && taggedStudent.getClassGroup() != null
                && !taggedStudent.getClassGroup().getGroupId().equals(album.getClassGroup().getGroupId())) {
            throw new BadRequestException("La foto no corresponde al grupo del album");
        }
    }

    private boolean canAccessAlbum(User requester, PhotoAlbum album) {
        if (hasInternalAdminRole(requester)) {
            return true;
        }

        if (!hasRole(requester, RoleName.TEACHER)) {
            return false;
        }

        Long groupId = resolveAlbumGroupId(album);
        return groupId != null && isAssignedToGroup(requester, groupId);
    }

    private void ensureCanAccessAlbum(User requester, PhotoAlbum album) {
        if (!canAccessAlbum(requester, album)) {
            throw new ForbiddenException("No tienes permiso para gestionar este album");
        }
    }

    private void ensureCanWriteAlbum(User requester, ClassGroup group, Student student) {
        if (hasInternalAdminRole(requester)) {
            return;
        }

        if (!hasRole(requester, RoleName.TEACHER)) {
            throw new ForbiddenException("No tienes permiso para gestionar albumes");
        }

        Long groupId = student != null && student.getClassGroup() != null
                ? student.getClassGroup().getGroupId()
                : group != null ? group.getGroupId() : null;
        if (groupId != null && isAssignedToGroup(requester, groupId)) {
            return;
        }

        throw new ForbiddenException("No tienes permiso para gestionar albumes de este grupo o estudiante");
    }

    private Long resolveAlbumGroupId(PhotoAlbum album) {
        if (album.getClassGroup() != null) {
            return album.getClassGroup().getGroupId();
        }
        if (album.getStudent() != null && album.getStudent().getClassGroup() != null) {
            return album.getStudent().getClassGroup().getGroupId();
        }
        return null;
    }

    private boolean isAssignedToGroup(User requester, Long groupId) {
        LocalDate today = LocalDate.now();
        return staffRepository.findByUserEmail(requester.getEmail())
                .map(Staff::getStaffId)
                .map(staffId -> staffGroupAssignmentRepository.findByStaffStaffIdOrderByStartDateDesc(staffId)
                        .stream()
                        .filter(assignment -> assignment.getClassGroup().getGroupId().equals(groupId))
                        .anyMatch(assignment -> isActiveAssignment(assignment, today)))
                .orElse(false);
    }

    private boolean isActiveAssignment(StaffGroupAssignment assignment, LocalDate today) {
        return !assignment.getStartDate().isAfter(today)
                && (assignment.getEndDate() == null || !assignment.getEndDate().isBefore(today));
    }

    private PhotoAlbum findActiveAlbum(Long albumId) {
        PhotoAlbum album = photoAlbumRepository.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album no encontrado"));
        if (!Boolean.TRUE.equals(album.getActive())) {
            throw new ResourceNotFoundException("Album no encontrado");
        }
        return album;
    }

    private PhotoAlbumPhoto findActivePhoto(Long photoId) {
        PhotoAlbumPhoto photo = photoAlbumPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"));
        if (Boolean.TRUE.equals(photo.getDeleted())) {
            throw new ResourceNotFoundException("Foto no encontrada");
        }
        return photo;
    }

    private void ensurePhotoBelongsToAlbum(PhotoAlbumPhoto photo, Long albumId) {
        if (!photo.getPhotoAlbum().getPhotoAlbumId().equals(albumId)) {
            throw new ResourceNotFoundException("Foto no encontrada");
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private ClassGroup findGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return classGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
    }

    private Student findStudent(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));
    }

    private boolean hasInternalAdminRole(User user) {
        return hasRole(user, RoleName.SUPER_ADMIN)
                || hasRole(user, RoleName.ADMIN)
                || hasRole(user, RoleName.DIRECTOR);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.getCode() == roleName);
    }

    private PhotoAlbumResponse toResponseWithPhotos(PhotoAlbum album) {
        return toResponse(album, photoAlbumPhotoRepository
                .findByPhotoAlbumPhotoAlbumIdAndDeletedFalseOrderByCreatedAtDesc(album.getPhotoAlbumId())
                .stream()
                .map(this::toPhotoResponse)
                .toList());
    }

    private PhotoAlbumResponse toResponse(PhotoAlbum album, List<PhotoAlbumPhotoResponse> photos) {
        ClassGroup group = album.getClassGroup();
        Student student = album.getStudent();
        User createdBy = album.getCreatedByUser();

        return new PhotoAlbumResponse(
                album.getPhotoAlbumId(),
                album.getTitle(),
                album.getDescription(),
                group != null ? group.getGroupId() : null,
                group != null ? group.getName() : null,
                student != null ? student.getStudentId() : null,
                student != null ? student.getFirstName() + " " + student.getLastName() : null,
                createdBy.getUserId(),
                createdBy.getEmail(),
                album.getEventDate(),
                album.getActive(),
                photos,
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }

    private PhotoAlbumPhotoResponse toPhotoResponse(PhotoAlbumPhoto photo) {
        Student student = photo.getStudent();
        User uploadedBy = photo.getUploadedByUser();

        return new PhotoAlbumPhotoResponse(
                photo.getPhotoAlbumPhotoId(),
                photo.getPhotoAlbum().getPhotoAlbumId(),
                student != null ? student.getStudentId() : null,
                student != null ? student.getFirstName() + " " + student.getLastName() : null,
                uploadedBy.getUserId(),
                uploadedBy.getEmail(),
                photo.getPhotoUrl(),
                photo.getCaption(),
                photo.getApproved(),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
