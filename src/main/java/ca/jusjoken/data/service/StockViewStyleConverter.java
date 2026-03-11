package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockSavedQuery.StockViewStyle;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StockViewStyleConverter implements AttributeConverter<StockViewStyle, String> {

    @Override
    public String convertToDatabaseColumn(StockViewStyle attribute) {
        return (attribute == null ? StockViewStyle.TILE : attribute).getShortName();
    }

    @Override
    public StockViewStyle convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return StockViewStyle.TILE;
        }
        try {
            return StockViewStyle.fromShortName(dbData); // Tile/List/Value/ValueTile
        } catch (IllegalArgumentException ex) {
            return StockViewStyle.valueOf(dbData); // TILE/LIST/VALUE/VALUE_TILE
        }
    }
}
