package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
