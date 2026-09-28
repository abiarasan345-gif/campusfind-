package com.example.campusfind.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusfind.dto.CategoryRequest;
import com.example.campusfind.dto.CategoryResponse;
import com.example.campusfind.entity.Category;
import com.example.campusfind.entity.User;
import com.example.campusfind.entity.UserRole;
import com.example.campusfind.repository.CategoryRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public CategoryService(CategoryRepository categoryRepository, AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request, HttpSession session) {
        requireAdmin(session);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category already exists");
        }
        return toResponse(categoryRepository.save(new Category(name)));
    }

    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    private void requireAdmin(HttpSession session) {
        User user = authService.requireUser(session);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only an admin can manage categories");
        }
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
