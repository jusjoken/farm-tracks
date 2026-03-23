package ca.jusjoken.utility;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.icon.Icon;

import ca.jusjoken.data.Utility;

public enum TaskType {
    BREEDING("Breeding", "Create Breed Plan"),
    BREED("Breed"),
    NESTBOX("Nestbox"),
    BIRTH("Birth", "Due Date", "Create Litter"),
    CLEAN_NESTBOX("Clean Nestbox"),
    REMOVE_NESTBOX("Remove Nestbox"),
    REBREED("Rebreed"),
    WEAN("Wean"),
    BUTCHER("Butcher"),
    PREGNANCY_CHECK("Pregnancy Check"),
    MEDICAL("Medical"),
    CUSTOM("Custom");

    private final String shortName;   // persisted value
    private final String displayName; // UI label
    private final String action;
    private static final Map<String, TaskType> LOOKUP = new HashMap<>();

    static {
        for (TaskType type : TaskType.values()) {
            LOOKUP.put(type.name(), type);
            LOOKUP.put(type.shortName, type);
            LOOKUP.put(type.displayName, type);
        }
    }

    TaskType(String shortName) {
        this(shortName, shortName, null);
    }

    TaskType(String shortName, String action) {
        this(shortName, shortName, action);
    }

    TaskType(String shortName, String displayName, String action) {
        this.shortName = shortName;
        this.displayName = displayName;
        this.action = action;
    }

    public String getShortName() {
        return shortName != null ? shortName : "";
    }

    public String getDisplayName() {
        return displayName != null ? displayName : getShortName();
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

    public Icon getIcon() {
        // Lazy resolution avoids enum initialization recursion at startup.
        // System.out.println("Resolving icon for TaskType: " + this.name());
        return Utility.ICONS.getIconForTaskType(this);
    }
}
