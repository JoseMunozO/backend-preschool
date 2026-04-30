package com.preschool.backendpreschool.repository;

import com.preschool.backendpreschool.model.Role;
import com.preschool.backendpreschool.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleName code);

    boolean existsByCode(RoleName code);
}
