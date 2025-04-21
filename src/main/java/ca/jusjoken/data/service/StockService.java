/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import java.util.ArrayList;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.data.domain.ExampleMatcher.*;
import org.springframework.data.domain.Sort;

/**
 *
 * @author birch
 */
@Service
@Transactional(readOnly = true)
public class StockService {
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }
    
    public List<Stock> getKitsForLitter(Litter litter){
        List<Stock> kitsForLitter = new ArrayList<>();
        kitsForLitter = stockRepository.findAllKitsByLitterId(litter.getId());
        //TODO: update to include kits that are fostered
        return kitsForLitter;
    }

    public List<Stock> getKitsForParent(Stock stock){
        List<Stock> kitsForParent = new ArrayList<>();
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            kitsForParent = stockRepository.findAllByMotherId(stock.getId());
        }else if(stock.getSex().equals(Utility.Gender.MALE)){
            kitsForParent = stockRepository.findAllByFatherId(stock.getId());
        }
        //TODO: update to include kits that are fostered
        return kitsForParent;
    }
    
    
    // Find stock with custom matching rules
    public List<Stock> findStockWithCustomMatcher(String name, Boolean breeder, StockType stockType, StockStatus status, StockSort stockSort) {
        Stock stock = new Stock();
        stock.setName(name);
        stock.setStockType(stockType);
        stock.setBreeder(breeder);
        if(status.getName().equals("active")){
            stock.setActive(Boolean.TRUE);
            stock.setStatus(null);
        }else if(status.getName().equals("inactive")){
            stock.setActive(Boolean.FALSE);
            stock.setStatus(null);
        }else if(status.getName().equals("all")){
            stock.setActive(null);
            stock.setStatus(null);
        }else{
            stock.setStatus(status.getName());
            stock.setActive(null);
        }
        System.out.println("findStockWithCustomMatcher: stock:" + stock);
        System.out.println("findStockWithCustomMatcher: sort:" + stockSort);
        
        // Create a custom ExampleMatcher
        ExampleMatcher matcher = matchingAll()
                .withIgnoreCase()                          // Ignore case for all string matches
                .withStringMatcher(StringMatcher.CONTAINING)// Use LIKE %value% for strings
                .withIgnoreNullValues()                    // Ignore null values
                .withIgnorePaths("needsSaving")
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.startsWith()); // make name startsWith

        Example<Stock> example = Example.of(stock, matcher);
        return stockRepository.findBy(example, q -> q
        .sortBy(Sort.by(stockSort.getSortOrderList())).all());
    }

    public List<Stock> findById(Long id) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public List<Stock> findAll() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
