package com.tutor.springjourney.datavalidation;

import lombok.Data;

@Data
public class ValidationError {
    private final String id;
    private final String level; // FATAL, ERROR, WARN, INFO
    private final String message;
    private final int line;
    private final String path;
    private final Object value;

    public ValidationError(String id, String level, String message, int line, String path, Object value) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.line = line;
        this.path = path;
        this.value = value;
    }

    @Override
    public String toString() {
        String icon = "WARN".equals(level) ? "⚠️" : "❌";
        return String.format("%s [%s] @第%d行 [%s]: %s (实际值: %s)",
                icon, level, line, path, message, value);
    }
    // Getter 略...
}