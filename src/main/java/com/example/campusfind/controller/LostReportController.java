package com.example.campusfind.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusfind.dto.LostReportRequest;
import com.example.campusfind.dto.LostReportResponse;
import com.example.campusfind.service.LostReportService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lost-reports")
public class LostReportController {

    private final LostReportService service;

    public LostReportController(LostReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<LostReportResponse> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String status) {
        return service.search(categoryId, location, keyword, status);
    }

    @GetMapping("/{id}")
    public LostReportResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<LostReportResponse> create(@Valid @RequestBody LostReportRequest request, HttpSession session) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, session));
    }

    @PutMapping("/{id}")
    public LostReportResponse update(@PathVariable Long id, @Valid @RequestBody LostReportRequest request, HttpSession session) {
        return service.update(id, request, session);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpSession session) {
        service.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
