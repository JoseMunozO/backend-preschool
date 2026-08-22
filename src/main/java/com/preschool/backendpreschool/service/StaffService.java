package com.preschool.backendpreschool.service;

import com.preschool.backendpreschool.dto.RoleResponse;
import com.preschool.backendpreschool.dto.StaffRequest;
import com.preschool.backendpreschool.dto.StaffResponse;
import com.preschool.backendpreschool.exception.BadRequestException;
import com.preschool.backendpreschool.exception.ConflictException;
import com.preschool.backendpreschool.exception.ForbiddenException;
import com.preschool.backendpreschool.exception.ResourceNotFoundException;
import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.Staff;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.repository.RoleRepository;
import com.preschool.backendpreschool.repository.StaffRepository;
import com.preschool.backendpreschool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<StaffResponse> getAllStaff(Boolean includeDeleted) {
        List<Staff> staff = Boolean.TRUE.equals(includeDeleted)
                ? staffRepository.findAll()
                : staffRepository.findAllByDeletedAtIsNull();

        return staff.stream()
                .map(this::toResponse)
                .toList();
    }

    public StaffResponse getStaffById(Long staffId) {
        return toResponse(findStaff(staffId));
    }

    @Transactional
    public void deleteStaff(Long staffId, String requesterEmail) {
        Staff staff = findStaff(staffId);
        User requester = findUser(requesterEmail);
        ensureCanManageStaffUser(requester, staff.getUser());

        staff.setDeletedAt(LocalDateTime.now());
        if (staff.getUser() != null) {
            staff.getUser().setStatus(UserStatus.INACTIVE);
        }
        staffRepository.save(staff);
    }

    @Transactional
    public StaffResponse restoreStaff(Long staffId) {
        Staff staff = staffRepository.findByStaffIdAndDeletedAtIsNotNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff eliminado no encontrado"));

        staff.setDeletedAt(null);
        if (staff.getUser() != null) {
            staff.getUser().setStatus(UserStatus.ACTIVE);
        }

        return toResponse(staffRepository.save(staff));
    }

    private void ensureCanManageStaffUser(User requester, User targetUser) {
        if (targetUser == null) {
            return;
        }

        int requesterMaxRank = maxRank(requester);
        int targetMaxRank = maxRank(targetUser);
        if (targetMaxRank > requesterMaxRank) {
            throw new ForbiddenException("No puedes dar de baja un puesto con un rango de acceso superior al tuyo");
        }

        if (targetUser.getRoles().stream().anyMatch(role -> role.getCode() == RoleName.SUPER_ADMIN)
                && userRepository.countByRolesCode(RoleName.SUPER_ADMIN) <= 1) {
            throw new ConflictException("No se puede dar de baja al ultimo SUPER_ADMIN del sistema");
        }
    }

    @Transactional
    public StaffResponse createStaff(StaffRequest request, String requesterEmail) {
        String email = normalizeEmail(request.email());
        String phone = normalizePhone(request.phone());

        if (email != null && userRepository.existsByEmail(email)) {
            throw new BadRequestException("El email ya existe");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("El telefono ya existe");
        }

        User linkedUser = null;
        if (hasText(request.password())) {
            if (!hasText(email)) {
                throw new BadRequestException("El email es requerido para crear la cuenta de acceso del staff");
            }
            if (request.roles() == null || request.roles().isEmpty()) {
                throw new BadRequestException("Se requiere al menos un rol para crear la cuenta de acceso");
            }

            User requester = findUser(requesterEmail);
            int requesterMaxRank = maxRank(requester);

            Set<Role> roles = request.roles()
                    .stream()
                    .map(this::findRole)
                    .collect(Collectors.toSet());

            for (Role role : roles) {
                if (role.getRankLevel() > requesterMaxRank) {
                    throw new ForbiddenException("No puedes otorgar el rol " + role.getCode() + ": es de rango superior al tuyo");
                }
            }

            linkedUser = User.builder()
                    .email(email)
                    .phone(phone)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .status(UserStatus.ACTIVE)
                    .emailVerified(false)
                    .phoneVerified(false)
                    .roles(roles)
                    .build();
            linkedUser = userRepository.save(linkedUser);
        }

        Staff staff = Staff.builder()
                .user(linkedUser)
                .employeeCode(trimToNull(request.employeeCode()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .phone(phone)
                .positionTitle(request.positionTitle().trim())
                .staffType(request.staffType().trim())
                .hireDate(request.hireDate())
                .status("active")
                .notes(trimToNull(request.notes()))
                .build();

        return toResponse(staffRepository.save(staff));
    }

    private Staff findStaff(Long staffId) {
        return staffRepository.findByStaffIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff no encontrado"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByCode(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private int maxRank(User user) {
        return user.getRoles().stream()
                .mapToInt(Role::getRankLevel)
                .max()
                .orElse(0);
    }

    private StaffResponse toResponse(Staff staff) {
        User user = staff.getUser();
        Set<RoleResponse> roles = user != null
                ? user.getRoles().stream()
                        .map(role -> new RoleResponse(
                                role.getRoleId(),
                                role.getCode(),
                                role.getName(),
                                role.getDescription(),
                                role.getRankLevel(),
                                role.getCreatedAt()
                        ))
                        .collect(Collectors.toSet())
                : Set.of();

        return new StaffResponse(
                staff.getStaffId(),
                user != null ? user.getUserId() : null,
                staff.getEmployeeCode(),
                staff.getFirstName(),
                staff.getLastName(),
                staff.getEmail(),
                staff.getPhone(),
                staff.getPositionTitle(),
                staff.getStaffType(),
                staff.getHireDate(),
                staff.getStatus(),
                staff.getNotes(),
                roles,
                staff.getCreatedAt(),
                staff.getUpdatedAt(),
                staff.getDeletedAt()
        );
    }

    private String normalizeEmail(String email) {
        return hasText(email) ? email.trim().toLowerCase() : null;
    }

    private String normalizePhone(String phone) {
        return hasText(phone) ? phone.trim() : null;
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
