package com.example.jira.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse {
    private String message;
    private String id;
}
