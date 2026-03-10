/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import static org.springframework.data.domain.ExampleMatcher.matchingAll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;

/**
 *
 * @author birch
 */
@Service
@Transactional
public class StockService {
    private final StockRepository stockRepository;
    //private final StockWeightHistoryService weightService;
    //private final StockStatusHistoryService statusService;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }
    
    public List<Stock> getKitsForLitter(Litter litter){
        List<Stock> kitsForLitter = stockRepository.findAllKitsByLitterId(litter.getId());
        //TODO: update to include kits that are fostered
        return kitsForLitter;
    }

    public List<Stock> getKitsForLitter(Integer litterId){
        List<Stock> kitsForLitter = stockRepository.findAllKitsByLitterId(litterId);
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
    
    @SuppressWarnings({"ConvertToStringSwitch", "CollectionsToArray"})
    private Example<Stock> getExample(String name, StockSavedQuery savedQuery){
        String[] ignoreFields = {"needsSaving","profileImage","defaultImageSource","sexText","sex","prefix","tattoo","fatherName","motherName","fatherId","motherId","color","breed","weightText","weight","doB","acquired","regNo","champNo","legs","genotype","kitsCount","notes","litter","fosterLitter","ageInDays","litterCount","kitCount","createdDate","lastModifiedDate","external","stockValue"};   
        Stock stock = new Stock();
        stock.setName(name);
        stock.setStockType(savedQuery.getStockType());
        stock.setBreeder(savedQuery.getBreeder());

        List<String> ignoreFieldList = new ArrayList<>(Arrays.asList(ignoreFields));

        if(savedQuery.getStockStatus().getName().equals("active")){
            // handled by applyStatusFilter: status in [active, deposit]
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
        }else if(savedQuery.getStockStatus().getName().equals("inactive")){
            // handled by applyStatusFilter: status NOT in [active, deposit]
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
        }else if(savedQuery.getStockStatus().getName().equals("all")){
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
        }else{
            stock.setStatus(savedQuery.getStockStatus().getName());
            stock.setActive(null);
        }

        ignoreFields = ignoreFieldList.toArray(new String[0]);

        ExampleMatcher matcher = matchingAll()
                .withIgnoreCase()                          // Ignore case for all string matches
                .withStringMatcher(StringMatcher.CONTAINING)// Use LIKE %value% for strings
                .withIgnoreNullValues()                    // Ignore null values
                .withIgnorePaths(ignoreFields)
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.startsWith()); // make name startsWith
        
        return Example.of(stock, matcher);
    }

    private List<Stock> applyStatusFilter(List<Stock> input, StockSavedQuery savedQuery) {
        if (savedQuery == null || savedQuery.getStockStatus() == null) {
            return input;
        }

        String queryStatus = savedQuery.getStockStatus().getName();

        if ("active".equals(queryStatus)) {
            return input.stream()
                    .filter(s -> Objects.equals("active", s.getStatus()) || Objects.equals("deposit", s.getStatus()) || Objects.equals("forsale", s.getStatus()))
                    .toList();
        }

        if ("inactive".equals(queryStatus)) {
            return input.stream()
                    .filter(s -> !Objects.equals("active", s.getStatus()) && !Objects.equals("deposit", s.getStatus()) && !Objects.equals("forsale", s.getStatus()))
                    .toList();
        }

        return input;
    }

    private Sort getSort(StockSavedQuery savedQuery){
        Sort thisSort;
        if(savedQuery.getSort2() == null || savedQuery.getSort2().getSortOrder() == null){
            thisSort = Sort.by(savedQuery.getSort1().getSortOrder());
        }else{
            thisSort = Sort.by(savedQuery.getSort1().getSortOrder(), savedQuery.getSort2().getSortOrder());
        }
        return thisSort;
    }

    private boolean isPrimarySortByName(StockSavedQuery savedQuery) {
        return savedQuery != null
                && savedQuery.getSort1() != null
                && savedQuery.getSort1().getSortOrder() != null
                && "name".equals(savedQuery.getSort1().getSortOrder().getProperty());
    }

    private List<Stock> moveBlankNamesToEnd(List<Stock> input, StockSavedQuery savedQuery) {
        if (!isPrimarySortByName(savedQuery) || input == null || input.isEmpty()) {
            return input;
        }

        List<Stock> withName = new ArrayList<>();
        List<Stock> blankName = new ArrayList<>();

        for (Stock s : input) {
            String n = (s == null) ? null : s.getName();
            if (n == null || n.trim().isEmpty()) {
                blankName.add(s);
            } else {
                withName.add(s);
            }
        }

        withName.addAll(blankName);
        return withName;
    }

    public List<Stock> findStockWithCustomMatcherPageable(Pageable pageable, String name, StockSavedQuery savedQuery) {
        Instant start = Instant.now();

        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), getSort(savedQuery));

        List<Stock> filtered = applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
        );

        filtered = moveBlankNamesToEnd(filtered, savedQuery);

        int fromIndex = Math.min((int) newPageable.getOffset(), filtered.size());
        int toIndex = Math.min(fromIndex + newPageable.getPageSize(), filtered.size());
        List<Stock> returnList = filtered.subList(fromIndex, toIndex);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        // System.out.println("***Stock query time:" + duration.toMillis() + " milliseconds");
        return returnList;
    }

    public Long findStockWithCustomMatcherCount(String name, StockSavedQuery savedQuery) {
        return (long) applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
        ).size();
    }

    public List<Stock> listByExample(String name, StockSavedQuery savedQuery){
        List<Stock> filtered = applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
        );
        return moveBlankNamesToEnd(filtered, savedQuery);
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
        List<Stock> stockList = stockRepository.findAllMothers(stockType.getId());
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
        List<Stock> stockList = stockRepository.findAllFathers(stockType.getId());
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
    
    public List<Stock> findAllBreeders(StockType type){
        List<Stock> stockList = stockRepository.findAllBreeders(type.getId());
        Collections.sort(stockList, new StockComparator());
        return stockList;
    }

    public List<Stock> findAllBreeders(){
        List<Stock> stockList = stockRepository.findAllBreeders();
        Collections.sort(stockList, new StockComparator());
        return stockList;
    }

    public List<Stock> list(Pageable pageable)   {
        return stockRepository.findAll(pageable).getContent();
    }
    
}
