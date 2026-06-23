package com.example.jira.service;

import com.example.jira.config.security.JwtService;
import com.example.jira.dto.ApiResponse;
import com.example.jira.dto.RegisterUserRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.exception.UserNotFoundException;
import com.example.jira.model.UserEntity;
import com.example.jira.model.UserRole;
import com.example.jira.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.boot.context.properties.bind.Bindable.mapOf;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getUserById(String id) {
        return userRepository.findById(id)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(String.valueOf(user.getRole()))
                        .build()
                ).orElseThrow(() -> new UserNotFoundException(id));
    }

    public ApiResponse createAndSaveUser(RegisterUserRequest registerUserRequest) {
        UserEntity userEntity = userRepository.save(buildRequest(registerUserRequest));
        return ApiResponse.builder()
                .id(userEntity.getId())
                .message("User created successfully!")
                .accessToken(getJwtToken(userEntity.getUsername(), userEntity))
                .build();

    }

    private UserEntity buildRequest(RegisterUserRequest registerUserRequest) {
        return UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .username(registerUserRequest.getUsername())
                .email(registerUserRequest.getEmail())
                .role(UserRole.valueOf(registerUserRequest.getRole()))
                .password(passwordEncoder.encode(registerUserRequest.getPassword()))
                .build();
    }

    @Transactional
    public void deleteUserById(String id) {
        if(userRepository.existsById(id)) {
            userRepository.deleteById(id);
            userRepository.flush(); // Force the database to commit right now before sending the Kafka message
            log.info("User with id {} has been deleted", id);
            kafkaTemplate.send("user-deleted-event", "temporary-deleted-key", id);
            // TODO : use something meaningful later, eg. KafkaTemplate<Integer, UserDeletedEvent> and send UserDeletedEvent instead of just user ID
            log.info("Sent 'DELETED' event to Kafka for user ID: {}", id);
        } else {
            throw new UserNotFoundException(id);
        }
    }

    public ApiResponse authenticateAndLogin(String username, String password) {
        UserEntity userEntity = userRepository.findByUsername(username).orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwordEncoder.matches(password, userEntity.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        String jwtToken = getJwtToken(username, userEntity);
        return ApiResponse.builder().message("Login successful").id(userEntity.getId()).accessToken(jwtToken).build();
    }

    private String getJwtToken(String username, UserEntity userEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userEntity.getRole());
        return jwtService.generateToken(username, claims);
    }
}
