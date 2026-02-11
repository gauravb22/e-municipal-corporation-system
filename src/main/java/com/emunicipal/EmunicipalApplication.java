package com.emunicipal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.emunicipal.entity.StaffUser;
import com.emunicipal.repository.StaffUserRepository;

@SpringBootApplication
public class EmunicipalApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmunicipalApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner seedAdminUser(StaffUserRepository staffUserRepository) {
        return args -> {
            StaffUser adminUser = staffUserRepository.findByUsername("admin");
            if (adminUser == null) {
                adminUser = new StaffUser();
                adminUser.setUsername("admin");
            }
            adminUser.setPassword("admin123");
            adminUser.setRole("ADMIN");
            staffUserRepository.save(adminUser);
        };
    }
}
