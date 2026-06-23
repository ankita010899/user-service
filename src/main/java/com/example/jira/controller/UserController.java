package com.example.jira.controller;

import com.example.jira.dto.ApiResponse;
import com.example.jira.dto.LoginUserRequest;
import com.example.jira.dto.RegisterUserRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserById(
            @PathVariable String id
    ) {
        return userService.getUserById(id);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse registerUser(
            @RequestBody RegisterUserRequest registerUserRequest
    ){
        return userService.createAndSaveUser(registerUserRequest);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse loginUser(
            @RequestBody LoginUserRequest loginUserRequest
    ){
        return userService.authenticateAndLogin(loginUserRequest.username, loginUserRequest.password);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserById(
            @PathVariable String id
    ) {
        log.info("Attempting to delete user with id {}", id);
        userService.deleteUserById(id);
    }
}
