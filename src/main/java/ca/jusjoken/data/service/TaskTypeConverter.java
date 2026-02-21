package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskTypeConverter implements AttributeConverter<Utility.TaskType, String> {

    @Override
    public String convertToDatabaseColumn(Utility.TaskType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getShortName();
    }

    @Override
    public Utility.TaskType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Utility.TaskType.fromShortName(dbData);
    }
}
