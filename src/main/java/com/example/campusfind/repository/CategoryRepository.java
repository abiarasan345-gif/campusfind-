package com.example.campusfind.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campusfind.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
}
