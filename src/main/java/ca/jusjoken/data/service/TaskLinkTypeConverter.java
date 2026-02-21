package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskLinkTypeConverter implements AttributeConverter<Utility.TaskLinkType, String> {

    @Override
    public String convertToDatabaseColumn(Utility.TaskLinkType attribute) {
        return attribute != null ? attribute.getShortName() : null;
    }

    @Override
    public Utility.TaskLinkType convertToEntityAttribute(String dbData) {
        return dbData != null ? Utility.TaskLinkType.fromShortName(dbData) : null;
    }
}
