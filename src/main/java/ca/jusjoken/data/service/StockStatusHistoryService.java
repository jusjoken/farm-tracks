/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.Utility.StockSaleStatus;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;

/**
 *
 * @author birch
 */
@Service
public class StockStatusHistoryService {
    private StockStatusHistoryRepository statusRepository;
    private StockService stockService;

    public StockStatusHistoryService(StockStatusHistoryRepository statusRepository, StockService stockService) {
        this.statusRepository = statusRepository;
        this.stockService = stockService;
    }
    
    public void deleteAll(){
        statusRepository.deleteAll();
    }
    
    public void delete(StockStatusHistory entity, Stock stock){
        statusRepository.delete(entity);
        //now update the stock to the newest status
        saveLatestToStock(stock, Boolean.FALSE);
    }
    
    @Transactional
    public void deleteByStockId(Integer stockId){
        statusRepository.deleteAllByStockId(stockId);
    }
    
    private void saveLatestToStock(Stock stock, Boolean fromImport){
        List<StockStatusHistory> statusList = findByStockId(stock.getId());
        if(!statusList.isEmpty()){
            StockStatusHistory statusHistory = statusList.get(0);
            //System.out.println("saveLatestToStock: latest:" + statusHistory);
            String latestStatusName = statusHistory.getStatusName();
            stock.setStatus(latestStatusName);
            syncSaleStatusFromStatusName(stock, latestStatusName);
            /*
            if(fromImport){
                //skip an existing date from the import to maintain original dates
                if(stock.getStatusDate()==null){
                    stock.setStatusDate(statusHistory.getSortDate());
                }
            }else{
                stock.setStatusDate(statusHistory.getSortDate());
            }
            */
            stock.setStatusDate(statusHistory.getSortDate());
            //save the stock item
            stockService.save(stock);
        }
    }

    private void syncSaleStatusFromStatusName(Stock stock, String statusName) {
        stock.setSaleStatus(mapSaleStatusFromStatusName(statusName));
    }

    private StockSaleStatus mapSaleStatusFromStatusName(String statusName) {
        if (statusName == null || statusName.isBlank()) {
            return StockSaleStatus.NONE;
        }

        String normalized = statusName.trim().toLowerCase();
        return switch (normalized) {
            case "listed", "forsale" -> StockSaleStatus.LISTED;
            case "deposit" -> StockSaleStatus.DEPOSIT;
            case "sold" -> StockSaleStatus.SOLD;
            default -> StockSaleStatus.NONE;
        };
    }

    private String mapStatusNameFromSaleStatus(StockSaleStatus saleStatus) {
        StockSaleStatus normalizedSaleStatus = saleStatus == null ? StockSaleStatus.NONE : saleStatus;
        return switch (normalizedSaleStatus) {
            case LISTED -> "listed";
            case DEPOSIT -> "deposit";
            case SOLD -> "sold";
            case NONE -> "active";
        };
    }

    public void saveStatusHistoryForSaleStatusChange(Stock stock, StockSaleStatus previousSaleStatus) {
        if (stock == null || stock.getId() == null) {
            return;
        }

        StockSaleStatus oldSaleStatus = previousSaleStatus == null ? StockSaleStatus.NONE : previousSaleStatus;
        StockSaleStatus currentSaleStatus = stock.getSaleStatus() == null ? StockSaleStatus.NONE : stock.getSaleStatus();
        if (oldSaleStatus == currentSaleStatus) {
            return;
        }

        String mappedStatusName = mapStatusNameFromSaleStatus(currentSaleStatus);
        List<StockStatusHistory> existingStatusHistory = findByStockId(stock.getId());
        if (!existingStatusHistory.isEmpty()) {
            String latestStatusName = existingStatusHistory.get(0).getStatusName();
            if (latestStatusName != null && latestStatusName.equalsIgnoreCase(mappedStatusName)) {
                saveLatestToStock(stock, Boolean.FALSE);
                return;
            }
        }

        save(new StockStatusHistory(stock.getId(), mappedStatusName, LocalDateTime.now()), stock, Boolean.FALSE);
    }
    
    public StockStatusHistory save(StockStatusHistory statusHistory, Stock stock, Boolean fromImport){
        //save the passed status item
        StockStatusHistory updated = statusRepository.save(statusHistory);
        //update the stock item so it always has the newest status and date
        saveLatestToStock(stock, fromImport);
        return updated;
    }
    
    public List<StockStatusHistory> findByStockId(Integer stockId){
        List<StockStatusHistory> list = statusRepository.findByStockId(stockId);
        Collections.sort(list, new StatusHistoryComparator().reversed());
        //System.out.println("findByStockId: sorted list:" + list);
        return list;
    }
    
    public Long getStatusCount(Integer stockId){
        Long count = statusRepository.countByStockId(stockId);
        return count;
    }

}
