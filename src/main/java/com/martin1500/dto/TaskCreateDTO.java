package com.martin1500.dto;

import com.martin1500.model.util.Priority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskCreateDTO(
        String title,
        @NotNull(message = "Priority cannot be null")
        Priority priority,

        Long projectId,

        @Future(message = "Due date must be in the future")
        @NotNull(message = "Due date cannot be null")
        LocalDate dueDate,

        @Size(max = 255, message = "Comments cannot exceed 255 characters")
        String comments) {
}
