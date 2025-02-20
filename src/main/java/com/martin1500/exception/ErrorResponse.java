package com.martin1500.exception;

import org.springframework.http.HttpStatus;

public record ErrorResponse(HttpStatus status, String message) {
}
