package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockSavedQuery.StockViewStyle;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StockViewStyleConverter  implements AttributeConverter<StockViewStyle, String>{

    @Override
    public String convertToDatabaseColumn(StockViewStyle attribute) {
        return attribute.getShortName();
    }

    @Override
    public StockViewStyle convertToEntityAttribute(String dbData) {
        return StockViewStyle.fromShortName(dbData);
    }

}
