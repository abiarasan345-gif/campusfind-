package com.example.campusfind.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusfind.dto.FoundItemRequest;
import com.example.campusfind.dto.FoundItemResponse;
import com.example.campusfind.dto.StatusUpdateRequest;
import com.example.campusfind.service.FoundItemService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/found-items")
public class FoundItemController {

    private final FoundItemService service;

    public FoundItemController(FoundItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<FoundItemResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status,
            HttpSession session) {
        return service.search(categoryId, location, keyword, status, session);
    }

    @GetMapping("/{id}")
    public FoundItemResponse getById(@PathVariable Long id, HttpSession session) {
        return service.getById(id, session);
    }

    @PostMapping
    public ResponseEntity<FoundItemResponse> create(@Valid @RequestBody FoundItemRequest request, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, session));
    }

    @PutMapping("/{id}")
    public FoundItemResponse update(@PathVariable Long id, @Valid @RequestBody FoundItemRequest request, HttpSession session) {
        return service.update(id, request, session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        service.delete(id, session);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public FoundItemResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            HttpSession session) {
        return service.updateStatus(id, request.status(), session);
    }
}
