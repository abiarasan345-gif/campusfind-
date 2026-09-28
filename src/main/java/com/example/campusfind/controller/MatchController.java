package com.example.campusfind.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusfind.dto.ConfirmMatchRequest;
import com.example.campusfind.dto.CaseResolutionRequest;
import com.example.campusfind.dto.MatchResponse;
import com.example.campusfind.service.MatchingService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchingService matchingService;

    public MatchController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping
    public List<MatchResponse> matches(@RequestParam(required = false) Long lostId, HttpSession session) {
        return matchingService.findMatches(lostId, session);
    }

    @GetMapping("/confirmed")
    public List<MatchResponse> confirmedMatches(HttpSession session) {
        return matchingService.findConfirmedMatches(session);
    }

    @PostMapping("/confirm")
    public ResponseEntity<MatchResponse> confirm(
            @Valid @RequestBody ConfirmMatchRequest request,
            HttpSession session) {
        return ResponseEntity.ok(matchingService.confirmMatch(request, session));
    }

    @PatchMapping("/{lostReportId}/resolution")
    public MatchResponse resolution(
            @PathVariable Long lostReportId,
            @Valid @RequestBody CaseResolutionRequest request,
            HttpSession session) {
        return matchingService.updateResolution(lostReportId, request, session);
    }
}
