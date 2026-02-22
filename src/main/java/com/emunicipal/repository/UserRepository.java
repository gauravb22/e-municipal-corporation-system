package com.emunicipal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.emunicipal.entity.User;
import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);
    User findByEmailIgnoreCase(String email);
    
    User findByPhone(String phone);
    User findByUsernameIgnoreCase(String username);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findByWardNo(Integer wardNo);
    List<User> findByWardZone(String wardZone);
    List<User> findByWardNoAndWardZone(Integer wardNo, String wardZone);
    List<User> findByActiveTrueOrActiveIsNull();
    long countByActiveTrue();
    long countByActiveFalse();
    long countByCreatedAtAfter(LocalDateTime since);
}
