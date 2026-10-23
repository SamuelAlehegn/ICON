package com.eotcicon.web.dto.common;

/**
 * A single validation failure, keyed by the request field that caused it.
 */
public record FieldErrorDto(String field, String message) {
}
