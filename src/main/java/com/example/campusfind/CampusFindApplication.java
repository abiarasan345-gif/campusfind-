package com.example.campusfind;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.campusfind.entity.Category;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.CategoryRepository;
import com.example.campusfind.repository.UserRepository;

@SpringBootApplication
public class CampusFindApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusFindApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User(
                        "admin", passwordEncoder.encode("admin123"), UserRole.ADMIN,
                        "Campus Administrator", "admin@campusfind.local"));
                userRepository.save(new User(
                        "staff1", passwordEncoder.encode("staff123"), UserRole.STAFF,
                        "Security Desk Staff", "staff1@campusfind.local"));
            }

            if (categoryRepository.count() == 0) {
                String[] categories = {
                        "ID Card", "Bottle", "Charger", "Calculator", "Mobile Phone",
                        "Wallet", "Keys", "Bag", "Books", "Other"
                };
                for (String name : categories) {
                    categoryRepository.save(new Category(name));
                }
            }
        };
    }
}
