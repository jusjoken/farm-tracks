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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import static org.springframework.data.domain.ExampleMatcher.matchingAll;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.StockSaleStatus;
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

    public List<Stock> getKitsAssignedToLitter(Litter litter){
        if (litter == null || litter.getId() == null) {
            return Collections.emptyList();
        }
        return getKitsAssignedToLitter(litter.getId());
    }

    public List<Stock> getKitsAssignedToLitter(Integer litterId){
        if (litterId == null) {
            return Collections.emptyList();
        }
        return stockRepository.findAllKitsAssignedToLitterId(litterId);
    }

    public List<Stock> getKitsForLitterDisplay(Integer litterId) {
        if (litterId == null) {
            return Collections.emptyList();
        }

        List<Stock> bornInLitter = stockRepository.findAllKitsByLitterId(litterId);
        List<Stock> fosteredIntoLitter = stockRepository.findAllByFosterLitterId(litterId);

        // Use insertion order so biological kits remain first, then foster-ins.
        Map<Integer, Stock> deduped = new LinkedHashMap<>();
        for (Stock stock : bornInLitter) {
            if (stock != null && stock.getId() != null) {
                deduped.put(stock.getId(), stock);
            }
        }
        for (Stock stock : fosteredIntoLitter) {
            if (stock != null && stock.getId() != null) {
                deduped.put(stock.getId(), stock);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    public Long getKitsAssignedCountForLitter(Integer litterId){
        if (litterId == null) {
            return 0L;
        }
        Long count = stockRepository.countAllKitsAssignedToLitterId(litterId);
        return count == null ? 0L : count;
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
        String[] ignoreFields = {"needsSaving","profileImage","defaultImageSource","sexText","sex","prefix","tattoo","fatherName","motherName","fatherId","motherId",
        "color","breed","weightText","weight","doB","acquired","regNo","champNo","legs","genotype","kitsCount","notes","litter","fosterLitter","ageInDays",
        "litterCount","kitCount","createdDate","lastModifiedDate","external","stockValue","saleStatus","invoiceNumber"};   
        Stock stock = new Stock();
        stock.setName(name);
        stock.setStockType(savedQuery.getStockType());
        stock.setBreeder(savedQuery.getBreeder());

        List<String> ignoreFieldList = new ArrayList<>(Arrays.asList(ignoreFields));

        if(savedQuery.getStockStatus().getName().equals("active")){
            // handled by applyStatusFilter via Stock.getActive()
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
            ignoreFieldList.add("status");
        }else if(savedQuery.getStockStatus().getName().equals("inactive")){
            // handled by applyStatusFilter via Stock.getActive()
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
            ignoreFieldList.add("status");
        }else if(savedQuery.getStockStatus().getName().equals("all")){
            stock.setActive(null);
            stock.setStatus(null);
            ignoreFieldList.add("active");
            ignoreFieldList.add("status");
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
        boolean includeExternal = Boolean.TRUE.equals(savedQuery.getIncludeExternalStock());

        if ("active".equals(queryStatus)) {
            return input.stream()
                    .filter(s -> Boolean.TRUE.equals(s.getActive()) || (includeExternal && Boolean.TRUE.equals(s.getExternal())))
                    .toList();
        }

        if ("inactive".equals(queryStatus)) {
            return input.stream()
                    .filter(s -> !Boolean.TRUE.equals(s.getActive()) || (includeExternal && Boolean.TRUE.equals(s.getExternal())))
                    .toList();
        }

        return input;
    }

    private List<Stock> applyExternalStockFilter(List<Stock> input, StockSavedQuery savedQuery) {
        if (savedQuery == null || Boolean.TRUE.equals(savedQuery.getIncludeExternalStock())) {
            return input;
        }

        return input.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getExternal()))
                .toList();
    }

    private Sort getSort(StockSavedQuery savedQuery){
        if (savedQuery == null) {
            return Sort.by(Sort.Order.asc("name"));
        }

        List<Sort.Order> orders = savedQuery.getSortOrders().stream()
                .map(ColumnSort::getSortOrder)
                .filter(Objects::nonNull)
                .toList();

        if (orders.isEmpty()) {
            return Sort.by(Sort.Order.asc("name"));
        }

        return Sort.by(orders);
    }

    private boolean isPrimarySortByName(StockSavedQuery savedQuery) {
        return savedQuery != null
            && !savedQuery.getSortOrders().isEmpty()
            && "name".equals(savedQuery.getSortOrders().get(0).getColumnName());
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

    private boolean isPrimarySortBySaleStatus(StockSavedQuery savedQuery) {
        return savedQuery != null
            && !savedQuery.getSortOrders().isEmpty()
            && "saleStatus".equals(savedQuery.getSortOrders().get(0).getColumnName());
    }

    private int getSaleStatusSortRank(Stock stock) {
        if (stock == null) {
            return Integer.MAX_VALUE;
        }

        StockSaleStatus saleStatus = stock.getSaleStatus();
        if (saleStatus == null) {
            return Integer.MAX_VALUE;
        }

        return switch (saleStatus) {
            case NONE -> 0;
            case LISTED -> 1;
            case DEPOSIT -> 2;
            case SOLD -> 3;
        };
    }

    private List<Stock> applyPrimarySaleStatusSort(List<Stock> input, StockSavedQuery savedQuery) {
        if (!isPrimarySortBySaleStatus(savedQuery) || input == null || input.size() < 2) {
            return input;
        }

        List<Stock> sorted = new ArrayList<>(input);
        Sort.Direction direction = savedQuery.getSortOrders().get(0).getColumnSortDirection();

        sorted.sort((left, right) -> Integer.compare(getSaleStatusSortRank(left), getSaleStatusSortRank(right)));
        if (direction == Sort.Direction.DESC) {
            Collections.reverse(sorted);
        }
        return sorted;
    }

    public List<Stock> findStockWithCustomMatcherPageable(Pageable pageable, String name, StockSavedQuery savedQuery) {
        Instant start = Instant.now();

        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), getSort(savedQuery));

        List<Stock> filtered = applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
        );

        filtered = applyExternalStockFilter(filtered, savedQuery);

        filtered = applyPrimarySaleStatusSort(filtered, savedQuery);

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
        return (long) applyExternalStockFilter(
            applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
            ),
            savedQuery
        ).size();
    }

    public List<Stock> listByExample(String name, StockSavedQuery savedQuery){
        List<Stock> filtered = applyStatusFilter(
                stockRepository.findAll(getExample(name, savedQuery), getSort(savedQuery)),
                savedQuery
        );
        filtered = applyExternalStockFilter(filtered, savedQuery);
        filtered = applyPrimarySaleStatusSort(filtered, savedQuery);
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
