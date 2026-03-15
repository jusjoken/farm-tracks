package ca.jusjoken.data.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.UserUiSettings;

@Service
public class UserUiSettingsService {

    private final UserUiSettingsRepository userUiSettingsRepository;

    public UserUiSettingsService(UserUiSettingsRepository userUiSettingsRepository) {
        this.userUiSettingsRepository = userUiSettingsRepository;
    }

    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return Optional.empty();
        }

        return Optional.of(username);
    }

    @Transactional(readOnly = true)
    public Optional<Object> getValueForCurrentUser(String key) {
        return getCurrentUsername().flatMap(username -> getValue(username, key));
    }

    @Transactional(readOnly = true)
    public Optional<Object> getValue(String username, String key) {
        if (username == null || username.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        return userUiSettingsRepository.findByUsername(username)
                .map(UserUiSettings::getSettingsJson)
                .map(this::readSettings)
                .map(settings -> settings.get(key));
    }

    @Transactional(readOnly = true)
    public boolean getBooleanForCurrentUser(String key, boolean defaultValue) {
        return getValueForCurrentUser(key)
                .map(value -> toBoolean(value, defaultValue))
                .orElse(defaultValue);
    }

    @Transactional
    public void setValueForCurrentUser(String key, Object value) {
        getCurrentUsername().ifPresent(username -> setValue(username, key, value));
    }

    @Transactional
    public void setBooleanForCurrentUser(String key, boolean value) {
        setValueForCurrentUser(key, value);
    }

    @Transactional
    public void setValue(String username, String key, Object value) {
        if (username == null || username.isBlank() || key == null || key.isBlank()) {
            return;
        }

        UserUiSettings userUiSettings = userUiSettingsRepository.findByUsername(username)
                .orElseGet(() -> new UserUiSettings(username));

        Map<String, Object> settings = readSettings(userUiSettings.getSettingsJson());
        if (value == null) {
            settings.remove(key);
        } else {
            settings.put(key, value);
        }

        userUiSettings.setSettingsJson(writeSettings(settings));
        userUiSettingsRepository.save(userUiSettings);
    }

    private Map<String, Object> readSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        String trimmedJson = settingsJson.trim();
        if (trimmedJson.isEmpty() || "{}".equals(trimmedJson)) {
            return new LinkedHashMap<>();
        }

        if (!trimmedJson.startsWith("{") || !trimmedJson.endsWith("}")) {
            throw new IllegalStateException("Unable to parse user UI settings JSON.");
        }

        String body = trimmedJson.substring(1, trimmedJson.length() - 1).trim();
        if (body.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        for (String entry : splitTopLevel(body)) {
            int separatorIndex = findTopLevelSeparator(entry);
            if (separatorIndex < 0) {
                throw new IllegalStateException("Unable to parse user UI settings JSON.");
            }

            String key = unescapeJson(stripQuotes(entry.substring(0, separatorIndex).trim()));
            String rawValue = entry.substring(separatorIndex + 1).trim();
            settings.put(key, parseValue(rawValue));
        }

        return settings;
    }

    private String writeSettings(Map<String, Object> settings) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"')
                    .append(escapeJson(entry.getKey()))
                    .append('"')
                    .append(':')
                    .append(toJsonValue(entry.getValue()));
        }
        builder.append('}');
        return builder.toString();
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return defaultValue;
    }

    private List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (int index = 0; index < body.length(); index++) {
            char currentChar = body.charAt(index);
            if (escaped) {
                current.append(currentChar);
                escaped = false;
                continue;
            }
            if (currentChar == '\\') {
                current.append(currentChar);
                escaped = true;
                continue;
            }
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                current.append(currentChar);
                continue;
            }
            if (currentChar == ',' && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    private int findTopLevelSeparator(String entry) {
        boolean inQuotes = false;
        boolean escaped = false;

        for (int index = 0; index < entry.length(); index++) {
            char currentChar = entry.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (currentChar == '\\') {
                escaped = true;
                continue;
            }
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (currentChar == ':' && !inQuotes) {
                return index;
            }
        }

        return -1;
    }

    private Object parseValue(String rawValue) {
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            return unescapeJson(stripQuotes(rawValue));
        }
        if ("true".equals(rawValue) || "false".equals(rawValue)) {
            return Boolean.parseBoolean(rawValue);
        }
        if ("null".equals(rawValue)) {
            return null;
        }
        try {
            if (rawValue.contains(".")) {
                return Double.parseDouble(rawValue);
            }
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            return rawValue;
        }
    }

    private String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return '"' + escapeJson(value.toString()) + '"';
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char currentChar = value.charAt(index);
            if (!escaped) {
                if (currentChar == '\\') {
                    escaped = true;
                } else {
                    builder.append(currentChar);
                }
                continue;
            }

            switch (currentChar) {
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                default -> builder.append(currentChar);
            }
            escaped = false;
        }

        if (escaped) {
            builder.append('\\');
        }

        return builder.toString();
    }
}