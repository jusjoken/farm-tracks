/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.ExampleMatcher.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 * @author birch
 */
@Service
//@Transactional(readOnly = true)
@Transactional
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
    
    public Long getKitCountForParent(Stock stock){
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            return stockRepository.countByMotherId(stock.getId());
        }else if(stock.getSex().equals(Utility.Gender.MALE)){
            return stockRepository.countByFatherId(stock.getId());
        }
        return 0L;
    }
    
    public String getStockNameById(Integer id){
        if(id==null) return "N/A";
        Optional<Stock> stockForName = stockRepository.findById(Long.valueOf(id));
        if(stockForName.isPresent()){
            return stockForName.get().getName();
        }
        return "N/A";
    }
    
    public List<Stock> findStockWithCustomMatcherPageable(Pageable pageable, String name, StockSavedQuery savedQuery) {
        Instant start = Instant.now();
        Stock stock = new Stock();
        stock.setName(name);
        stock.setStockType(savedQuery.getStockType());
        stock.setBreeder(savedQuery.getBreeder());
        if(savedQuery.getStockStatus().getName().equals("active")){
            stock.setActive(Boolean.TRUE);
            stock.setStatus("active");
        }else if(savedQuery.getStockStatus().getName().equals("inactive")){
            stock.setActive(Boolean.FALSE);
            stock.setStatus(null);
        }else if(savedQuery.getStockStatus().getName().equals("all")){
            stock.setActive(null);
            stock.setStatus(null);
        }else{
            stock.setStatus(savedQuery.getStockStatus().getName());
            stock.setActive(null);
        }
        System.out.println("findStockWithCustomMatcher: stock:" + stock);
        System.out.println("findStockWithCustomMatcher: sort:" + savedQuery.getSort1().getColumnName() + "/" + savedQuery.getSort1().getColumnSortDirection() + "," + savedQuery.getSort2().getColumnName() + "/" + savedQuery.getSort2().getColumnSortDirection());
        
        // Create a custom ExampleMatcher
        ExampleMatcher matcher = matchingAll()
                .withIgnoreCase()                          // Ignore case for all string matches
                .withStringMatcher(StringMatcher.CONTAINING)// Use LIKE %value% for strings
                .withIgnoreNullValues()                    // Ignore null values
                .withIgnorePaths("needsSaving","profileImage","defaultImageSource","sexText","sex","category","prefix","tattoo","cage","fatherName","motherName","fatherId","motherId","color","breed","weightText","weight","doB","acquired","regNo","champNo","legs","genotype","kitsCount","notes","litter","fosterLitter","ageInDays","litterCount","kitCount","createdDate","lastModifiedDate")
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.startsWith()); // make name startsWith
        
        Example<Stock> example = Example.of(stock, matcher);

        Sort thisSort;
        if(savedQuery.getSort2()==null || savedQuery.getSort2().getSortOrder()==null){
            thisSort = Sort.by(savedQuery.getSort1().getSortOrder());
        }else{
            thisSort = Sort.by(savedQuery.getSort1().getSortOrder(), savedQuery.getSort2().getSortOrder());
        }

        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),thisSort);
        System.out.println("findStockWithCustomMatcher: pageNumber:" + pageable.getPageNumber() + " pageSize:" + pageable.getPageSize());
        Page<Stock> page = stockRepository.findAll(example, newPageable);
        List<Stock> returnList = page.getContent();
        //System.out.println("findStockWithCustomMatcher: first stock item:" + returnList.get(0));
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("***Stock query time:" + duration.toMillis() + " milliseconds");
        return returnList;
        
    }

    public Long findStockWithCustomMatcherCount(String name, StockSavedQuery savedQuery) {
        Stock stock = new Stock();
        stock.setName(name);
        stock.setStockType(savedQuery.getStockType());
        stock.setBreeder(savedQuery.getBreeder());
        if(savedQuery.getStockStatus().getName().equals("active")){
            stock.setActive(Boolean.TRUE);
            stock.setStatus("active");
        }else if(savedQuery.getStockStatus().getName().equals("inactive")){
            stock.setActive(Boolean.FALSE);
            stock.setStatus(null);
        }else if(savedQuery.getStockStatus().getName().equals("all")){
            stock.setActive(null);
            stock.setStatus(null);
        }else{
            stock.setStatus(savedQuery.getStockStatus().getName());
            stock.setActive(null);
        }
        System.out.println("findStockWithCustomMatcherCount: stock:" + stock);
        
        // Create a custom ExampleMatcher
        ExampleMatcher matcher = matchingAll()
                .withIgnoreCase()                          // Ignore case for all string matches
                .withStringMatcher(StringMatcher.CONTAINING)// Use LIKE %value% for strings
                .withIgnoreNullValues()                    // Ignore null values
                .withIgnorePaths("needsSaving","profileImage","defaultImageSource","sexText","sex","category","prefix","tattoo","cage","fatherName","motherName","fatherId","motherId","color","breed","weightText","weight","doB","acquired","regNo","champNo","legs","genotype","kitsCount","notes","litter","fosterLitter","ageInDays","litterCount","kitCount")
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.startsWith()); // make name startsWith
        
        Example<Stock> example = Example.of(stock, matcher);

        return stockRepository.count(example);
        
    }

    public Stock findById(Integer id) {
        return stockRepository.findAllById(id);
    }

    public List<Stock> findAll() {
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        return null;
    }

    public void save(Stock entity){
        this.stockRepository.save(entity);
    }
    
    public void delete(Integer id){
        this.stockRepository.deleteById(id.longValue());
    }
    
    public Boolean checkInUse(Stock entity){
        if(getKitCountForParent(entity)>0L) return Boolean.TRUE;
        return Boolean.FALSE;
    }
    
    public List<Stock> getMothers(String name, StockType stockType){
        List<Stock> stockList = stockRepository.findAllMothers();
        if(name==null || name.isEmpty()){
            Collections.sort(stockList, new StockComparator());
            return stockList;
        }
        //add the extParent to the list
        stockList.add(getParentExt(name, stockType));
        Collections.sort(stockList, new StockComparator());
        return stockList;
    }
    
    public List<Stock> getFathers(String name, StockType stockType){
        List<Stock> stockList = stockRepository.findAllFathers();
        if(name==null || name.isEmpty()){
            Collections.sort(stockList, new StockComparator());
            return stockList;
        }
        //add the extParent to the list
        stockList.add(getParentExt(name, stockType));
        Collections.sort(stockList, new StockComparator());
        return stockList;
    }
    
    public Stock getFather(Stock stock){
        if(stock.getFatherExtName()==null || stock.getFatherExtName().isEmpty()){
            return findById(stock.getFatherId());
        }
        return getParentExt(stock.getFatherExtName(), stock.getStockType());
    }
    
    public Stock getMother(Stock stock){
        if(stock.getMotherExtName()==null || stock.getMotherExtName().isEmpty()){
            return findById(stock.getMotherId());
        }
        return getParentExt(stock.getMotherExtName(), stock.getStockType());
    }
    
    public Stock getParentExt(String name, StockType stockType){
        return new Stock(name, Boolean.FALSE, stockType);
    }
    
    public List<Stock> findAllBreeders(){
        return stockRepository.findAllBreeders();
    }

}
