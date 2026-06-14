package com.pet.module.complaint.controller;

import com.pet.common.Result;
import com.pet.module.complaint.entity.Complaint;
import com.pet.module.complaint.service.ComplaintService;
import com.pet.security.JwtAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public Result<List<Complaint>> listMyComplaints(@AuthenticationPrincipal JwtAuthenticationToken token) {
        return Result.success(complaintService.listByOwner(token.getUserId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public Result<List<Complaint>> listAll() {
        return Result.success(complaintService.listAll());
    }

    @PostMapping
    public Result<Complaint> create(@AuthenticationPrincipal JwtAuthenticationToken token,
                                     @RequestBody Complaint complaint) {
        complaint.setOwnerId(token.getUserId());
        return Result.success(complaintService.create(complaint));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public Result<Complaint> resolve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result"), "resolved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE')")
    public Result<Complaint> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(complaintService.process(id, body.get("result"), "rejected"));
    }
}
