package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.StaffUser;

import java.util.List;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    StaffUser findByUsername(String username);
    List<StaffUser> findByRoleIgnoreCaseOrderByWardNoAscWardZoneAscUsernameAsc(String role);
    long countByRoleIgnoreCase(String role);
}
