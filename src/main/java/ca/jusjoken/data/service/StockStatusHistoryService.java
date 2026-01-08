/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

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
        List<StockStatusHistory> statusList = findByStockId(stock.getId());
        StockStatusHistory statusHistory = statusList.get(0);
        stock.setStatus(statusHistory.getStatusName());
        stock.setStatusDate(statusHistory.getSortDate());
        //save the stock item
        stockService.save(stock);
    }
    
    public StockStatusHistory save(StockStatusHistory statusHistory, Stock stock, Boolean fromImport){
        //save the passed status item
        StockStatusHistory updated = statusRepository.save(statusHistory);
        //update the stock item so it always has the newest status and date
        stock.setStatus(statusHistory.getStatusName());
        if(fromImport){
            //skip an existing date from the import to maintain original dates
            if(stock.getStatusDate()==null){
                stock.setStatusDate(updated.getLastModifiedDate());
            }
        }else{
            stock.setStatusDate(updated.getSortDate());
        }
        //save the stock item
        stockService.save(stock);
        return updated;
    }
    
    public List<StockStatusHistory> findByStockId(Integer stockId){
        List<StockStatusHistory> list = statusRepository.findByStockId(stockId);
        Collections.sort(list, new StatusHistoryComparator().reversed());
        return list;
    }
    
    public Long getStatusCount(Integer stockId){
        Long count = statusRepository.countByStockId(stockId);
        System.out.println("getStatusCount: stock:" + stockId + " count:" + count);
        return count;
    }

}
