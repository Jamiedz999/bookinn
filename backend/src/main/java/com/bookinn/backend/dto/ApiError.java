package com.bookinn.backend.dto;

import java.time.Instant;

/**
 * The uniform error body returned for every failed request.
 *
 * @param timestamp when the error was produced
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message human-readable detail
 * @param path request path that failed
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {}
