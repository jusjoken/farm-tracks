package ca.jusjoken.data.service;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ca.jusjoken.data.Utility.StockSaleStatus;

@Converter(autoApply = true)

public class StockSaleStatusConverter implements AttributeConverter<StockSaleStatus, String> {

    @Override
    public String convertToDatabaseColumn(StockSaleStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getShortName();
    }

    @Override
    public StockSaleStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return StockSaleStatus.fromShortName(dbData);
    }

}
