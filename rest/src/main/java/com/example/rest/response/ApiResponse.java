package com.example.rest.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final String message;
    private final T data;
    private final Map<String, List<String>> errors;
    private final Instant timestamp;

    public ApiResponse(String message, T data) {
        this(message, data, Collections.emptyMap());
    }

    public ApiResponse(String message, T data, Map<String, List<String>> errors) {
        this.message = message;
        this.data = data;
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
