package com.martin1500.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskDTO {

    private Long id;

    @Size(max = 50, message = "Comments cannot exceed 50 characters")
    private String title;

    @Size(max = 255, message = "Comments cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Status cannot be null")
    private Status status;

    @NotNull(message = "Priority cannot be null")
    private Priority priority;

    @Future(message = "Due date must be in the future")
    @NotNull(message = "Due date cannot be null")
    private LocalDate dueDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Size(max = 255, message = "Comments cannot exceed 255 characters")
    private String comments;
}
