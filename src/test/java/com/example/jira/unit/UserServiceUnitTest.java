package com.example.jira.unit;

import com.example.jira.dto.ApiResponse;
import com.example.jira.dto.UserRequest;
import com.example.jira.dto.UserResponse;
import com.example.jira.model.UserEntity;
import com.example.jira.model.UserRole;
import com.example.jira.repository.UserRepository;
import com.example.jira.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {

    @Mock
    public UserRepository userRepository;
    @InjectMocks
    public UserService userService;

    @Test
    public void getUserByIdSuccessTest() {
        // given
        String id = UUID.randomUUID().toString();
        UserEntity userEntity = UserEntity.builder()
                .id(id)
                .username("john")
                .email("abc@gmail.com")
                .role(UserRole.valueOf("QA")).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(userEntity));

        // when
        UserResponse userResponse = userService.getUserById(id);

        // then
        Assertions.assertEquals(id, userResponse.getId());
        Assertions.assertEquals("john", userResponse.getUsername());
        Assertions.assertEquals("abc@gmail.com", userResponse.getEmail());
        Assertions.assertEquals(UserRole.QA.toString(), userResponse.getRole());
    }

    @Test
    public void getUserByIdFailureTest() {
        // given
        String id = UUID.randomUUID().toString();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // when
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> userService.getUserById(id));

        // then
        Assertions.assertTrue(ex.getMessage().contains("User not found with id: " + id));
    }

    @Test
    public void createUserTest() {
        String id = UUID.randomUUID().toString();
        UserRequest userRequest = UserRequest.builder()
                .username("john")
                .email("abc@gmail.com")
                .role(String.valueOf(UserRole.QA)).build();
        UserEntity userEntity = UserEntity.builder()
                .id(id)
                .username("john")
                .email("abc@gmail.com")
                .role(UserRole.valueOf("QA")).build();
        given(userRepository.save(any(UserEntity.class))).willReturn(userEntity);

        // when
        ApiResponse response = userService.createAndSaveUser(userRequest);

        // then
        Assertions.assertNotNull(response.getId());
        Assertions.assertEquals(id, response.getId());
        Assertions.assertEquals("User created successfully!", response.getMessage());
    }

    // TODO : add tests for delete user endpoint
}
