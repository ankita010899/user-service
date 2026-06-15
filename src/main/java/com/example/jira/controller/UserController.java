package com.example.jira.controller;

import com.example.jira.dto.ApiResponse;
import com.example.jira.dto.UserRequest;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse createUser(
            @RequestBody UserRequest userRequest
    ){
        return userService.createAndSaveUser(userRequest);
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
