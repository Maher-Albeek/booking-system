package com.maher.booking_system.controller;

import com.maher.booking_system.model.Branch;
import com.maher.booking_system.service.BranchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public List<Branch> getAll() {
        return branchService.getAll();
    }

    @PostMapping("/admin")
    public Branch create(@RequestBody Branch branch) {
        return branchService.create(branch);
    }

    @PutMapping("/admin/{id}")
    public Branch update(@PathVariable Long id, @RequestBody Branch branch) {
        return branchService.update(id, branch);
    }

    @DeleteMapping("/admin/{id}")
    public void delete(@PathVariable Long id) {
        branchService.delete(id);
    }
}
