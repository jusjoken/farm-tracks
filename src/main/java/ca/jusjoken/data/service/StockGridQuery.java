package ca.jusjoken.data.service;

import java.util.List;

import ca.jusjoken.data.ColumnSort;

public record StockGridQuery(
        String namePrefix,
        Integer stockTypeId,
        Boolean breeder,
        String stockStatusName,
        Boolean includeExternalStock,
        List<ColumnSort> sortOrders) {

    public List<ColumnSort> safeSortOrders() {
        return sortOrders == null ? List.of() : sortOrders;
    }

    public boolean includeExternal() {
        return Boolean.TRUE.equals(includeExternalStock);
    }
}