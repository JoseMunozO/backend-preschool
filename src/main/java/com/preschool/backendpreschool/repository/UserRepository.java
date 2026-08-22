package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.RoleName;
import com.preschool.backendpreschool.model.User;
import com.preschool.backendpreschool.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByStatus(UserStatus status);

    List<User> findByEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(String email, String phone);

    long countByRolesCode(RoleName code);
}
