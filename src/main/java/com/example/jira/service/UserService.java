package com.example.jira.service;

import com.example.jira.dto.ApiResponse;
import com.example.jira.dto.UserRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.exception.UserNotFoundException;
import com.example.jira.model.UserEntity;
import com.example.jira.model.UserRole;
import com.example.jira.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

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

    public ApiResponse createAndSaveUser(UserRequest userRequest) {
        UserEntity userEntity = userRepository.save(buildRequest(userRequest));
        return ApiResponse.builder()
                .id(userEntity.getId())
                .message("User created successfully!")
                .build();

    }

    private UserEntity buildRequest(UserRequest userRequest) {
        return UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .role(UserRole.valueOf(userRequest.getRole()))
                .build();
    }

    @Transactional
    public void deleteUserById(String id) {
        if(userRepository.existsById(id)) {
            userRepository.deleteById(id);
            log.info("User with id {} has been deleted", id);
            kafkaTemplate.send("user-deleted-event", "temporary-deleted-key", id);
            // TODO : use something meaningful later, eg. KafkaTemplate<Integer, UserDeletedEvent> and send UserDeletedEvent instead of just user ID
            log.info("Sent 'DELETED' event to Kafka for user ID: {}", id);
        } else {
            throw new UserNotFoundException(id);
        }
    }
}
