package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskPlanStatusConverter implements AttributeConverter<Utility.TaskPlanStatus, String> {

    @Override
    public String convertToDatabaseColumn(Utility.TaskPlanStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getShortName();
    }

    @Override
    public Utility.TaskPlanStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Utility.TaskPlanStatus.fromShortName(dbData);
    }
}
