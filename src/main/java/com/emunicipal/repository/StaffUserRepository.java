package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.StaffUser;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    StaffUser findByUsername(String username);
}
