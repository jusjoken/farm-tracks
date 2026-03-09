package ca.jusjoken.data.service;

import ca.jusjoken.utility.TaskType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskTypeConverter implements AttributeConverter<TaskType, String> {

    @Override
    public String convertToDatabaseColumn(TaskType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getShortName();
    }

    @Override
    public TaskType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return TaskType.fromShortName(dbData);
    }
}
