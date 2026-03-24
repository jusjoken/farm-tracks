package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import ca.jusjoken.data.entity.Stock;

public interface StockRepositoryCustom {
    List<Stock> findAllForGrid(StockGridQuery query, Pageable pageable);

    List<Stock> findAllForGrid(StockGridQuery query);

    long countForGrid(StockGridQuery query);

    double sumStockValueForGrid(StockGridQuery query);
}