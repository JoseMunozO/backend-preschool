package com.preschool.backendpreschool.controller;

import com.preschool.backendpreschool.dto.AssignRoleRequest;
import com.preschool.backendpreschool.dto.ChangeUserStatusRequest;
import com.preschool.backendpreschool.dto.CreateUserRequest;
import com.preschool.backendpreschool.dto.UpdateUserRequest;
import com.preschool.backendpreschool.dto.UserResponse;
import com.preschool.backendpreschool.model.UserStatus;
import com.preschool.backendpreschool.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getAllUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search
    ) {
        return userService.getAllUsers(status, search);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(userId, request);
    }

    @PostMapping("/{userId}/roles")
    public UserResponse assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        return userService.assignRole(userId, request);
    }

    @DeleteMapping("/{userId}/roles")
    public UserResponse removeRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        return userService.removeRole(userId, request);
    }

    @PatchMapping("/{userId}/deactivate")
    public UserResponse deactivateUser(@PathVariable Long userId) {
        return userService.deactivateUser(userId);
    }

    @PatchMapping("/{userId}/activate")
    public UserResponse activateUser(@PathVariable Long userId) {
        return userService.activateUser(userId);
    }

    @PatchMapping("/{userId}/status")
    public UserResponse changeStatus(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return userService.changeStatus(userId, request);
    }
}
