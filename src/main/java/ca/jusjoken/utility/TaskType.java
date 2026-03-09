package ca.jusjoken.utility;

import java.util.HashMap;
import java.util.Map;

public enum TaskType {
    BREED("Breed"),
    NESTBOX("Nestbox"),
    BIRTH("Birth", "Create Litter"),
    CLEAN_NESTBOX("Clean Nestbox"),
    REMOVE_NESTBOX("Remove Nestbox"),
    REBREED("Rebreed"),
    WEAN("Wean"),
    BUTCHER("Butcher"),
    PREGNANCY_CHECK("Pregnancy Check"),
    MEDICAL("Medical"),
    CUSTOM("Custom");

    private final String shortName;
    private final String action;
    private static final Map<String, TaskType> LOOKUP = new HashMap<>();

    static {
        for (TaskType type : TaskType.values()) {
            LOOKUP.put(type.name(), type);
            LOOKUP.put(type.shortName, type);
        }
    }

    TaskType(String shortName) {
        this(shortName, null);
    }

    TaskType(String shortName, String action) {
        this.shortName = shortName;
        this.action = action;
    }

    public String getShortName() {
        return shortName != null ? shortName : "";
    }

    public String getAction() {
        return action;
    }

    public boolean hasAction() {
        return action != null && !action.isBlank();
    }

    public static TaskType fromShortName(String shortName) {
        TaskType type = LOOKUP.get(shortName);
        if (type != null) {
            return type;
        }
        throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
    }
}
