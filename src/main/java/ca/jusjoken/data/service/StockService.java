/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
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

    public Map<Integer, Long> getKitsAssignedCountsForLitters(List<Integer> litterIds) {
        if (litterIds == null || litterIds.isEmpty()) {
            return Map.of();
        }

        List<Integer> deduped = litterIds.stream().filter(Objects::nonNull).distinct().toList();
        if (deduped.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Long> countsByLitterId = new HashMap<>();
        for (Object[] row : stockRepository.countAllKitsAssignedToLitterIds(deduped)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Integer litterId = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            countsByLitterId.put(litterId, count);
        }

        return countsByLitterId;
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
    
    private StockGridQuery toGridQuery(String name, StockSavedQuery savedQuery) {
        if (savedQuery == null) {
            return new StockGridQuery(name, null, null, "all", false, List.of(new ColumnSort("name", Sort.Direction.ASC)));
        }

        String stockStatusName = savedQuery.getStockStatus() == null
                ? "all"
                : savedQuery.getStockStatus().getName();

        List<ColumnSort> sortOrders = savedQuery.getSortOrders();
        if (sortOrders == null || sortOrders.isEmpty()) {
            sortOrders = List.of(new ColumnSort("name", Sort.Direction.ASC));
        }

        return new StockGridQuery(
                name,
                savedQuery.getStockType() == null ? null : savedQuery.getStockType().getId(),
                savedQuery.getBreeder(),
                stockStatusName,
                savedQuery.getIncludeExternalStock(),
                sortOrders);
    }

    public List<Stock> findStockWithCustomMatcherPageable(Pageable pageable, String name, StockSavedQuery savedQuery) {
        return stockRepository.findAllForGrid(toGridQuery(name, savedQuery), pageable);
    }

    public Long findStockWithCustomMatcherCount(String name, StockSavedQuery savedQuery) {
        return stockRepository.countForGrid(toGridQuery(name, savedQuery));
    }

    public List<Stock> listByExample(String name, StockSavedQuery savedQuery) {
        return stockRepository.findAllForGrid(toGridQuery(name, savedQuery));
    }

    public double sumStockValueForGrid(String name, StockSavedQuery savedQuery) {
        return stockRepository.sumStockValueForGrid(toGridQuery(name, savedQuery));
    }

    public Map<Integer, Stock> findStockByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<Integer> deduped = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (deduped.isEmpty()) {
            return Map.of();
        }

        List<Stock> matches = stockRepository.findAllByIdIn(deduped);
        Map<Integer, Stock> byId = new HashMap<>();
        for (Stock stock : matches) {
            if (stock != null && stock.getId() != null) {
                byId.put(stock.getId(), stock);
            }
        }
        return byId;
    }

    public Map<Integer, Long> getKitCountsForParents(List<Stock> parents) {
        if (parents == null || parents.isEmpty()) {
            return Map.of();
        }

        List<Integer> motherIds = new ArrayList<>();
        List<Integer> fatherIds = new ArrayList<>();
        for (Stock parent : parents) {
            if (parent == null || parent.getId() == null || !Boolean.TRUE.equals(parent.getBreeder())) {
                continue;
            }
            if (Utility.Gender.FEMALE.equals(parent.getSex())) {
                motherIds.add(parent.getId());
            } else if (Utility.Gender.MALE.equals(parent.getSex())) {
                fatherIds.add(parent.getId());
            }
        }

        Map<Integer, Long> motherCounts = new HashMap<>();
        if (!motherIds.isEmpty()) {
            for (Object[] row : stockRepository.countByMotherIds(motherIds)) {
                if (row != null && row.length == 2 && row[0] != null && row[1] != null) {
                    motherCounts.put((Integer) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        Map<Integer, Long> fatherCounts = new HashMap<>();
        if (!fatherIds.isEmpty()) {
            for (Object[] row : stockRepository.countByFatherIds(fatherIds)) {
                if (row != null && row.length == 2 && row[0] != null && row[1] != null) {
                    fatherCounts.put((Integer) row[0], ((Number) row[1]).longValue());
                }
            }
        }

        Map<Integer, Long> result = new HashMap<>();
        for (Stock parent : parents) {
            if (parent == null || parent.getId() == null || !Boolean.TRUE.equals(parent.getBreeder())) {
                continue;
            }
            if (Utility.Gender.FEMALE.equals(parent.getSex())) {
                result.put(parent.getId(), motherCounts.getOrDefault(parent.getId(), 0L));
            } else if (Utility.Gender.MALE.equals(parent.getSex())) {
                result.put(parent.getId(), fatherCounts.getOrDefault(parent.getId(), 0L));
            }
        }

        return result;
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

    public Stock getOrCreateExternalParent(String name, StockType stockType, Gender sex){
        if (name == null || stockType == null || sex == null) {
            return null;
        }

        String normalizedName = name.trim();
        if (normalizedName.isEmpty()) {
            return null;
        }

        List<Stock> stockByType = stockRepository.findAllByStockTypeId(stockType.getId());
        for (Stock candidate : stockByType) {
            if (candidate == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(candidate.getExternal())) {
                continue;
            }
            if (!sex.equals(candidate.getSex())) {
                continue;
            }
            if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(normalizedName)) {
                return candidate;
            }
        }

        Stock externalParent = new Stock();
        externalParent.setName(normalizedName);
        externalParent.setPrefix("");
        externalParent.setExternal(true);
        externalParent.setBreeder(true);
        externalParent.setSex(sex);
        externalParent.setStockType(stockType);
        externalParent.setWeight(0);
        externalParent.setStatus("archived");
        externalParent.setStatusDate(LocalDateTime.now());

        save(externalParent);
        return externalParent;
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

    public long countByStockType(StockType stockType) {
        if (stockType == null) {
            return 0L;
        }
        Long count = stockRepository.countByStockType(stockType);
        return count == null ? 0L : count;
    }

    public List<Stock> list(Pageable pageable)   {
        return stockRepository.findAll(pageable).getContent();
    }
    
}
