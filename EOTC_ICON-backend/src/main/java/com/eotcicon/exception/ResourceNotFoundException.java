package com.eotcicon.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String resourceName, String identifier) {
		super(resourceName + " not found: " + identifier);
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
