package com.martin1500.exception;

public class MissingSecretKeyException extends RuntimeException{
    public MissingSecretKeyException(String message) {
        super(message);
    }
}
