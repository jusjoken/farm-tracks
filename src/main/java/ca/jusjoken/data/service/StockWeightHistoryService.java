/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockWeightHistory;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 *
 * @author birch
 */
@Service
public class StockWeightHistoryService {
    private StockWeightHistoryRepository weightRepository;
    private StockService stockService;

    public StockWeightHistoryService(StockWeightHistoryRepository weightRepository, StockService stockService) {
        this.weightRepository = weightRepository;
        this.stockService = stockService;
    }
    
    public void deleteAll(){
        weightRepository.deleteAll();
    }
    
    public void delete(StockWeightHistory entity, Stock stock){
        weightRepository.delete(entity);
        //now update the stock to the newest status
        saveLatestToStock(stock);
    }
    
    private void saveLatestToStock(Stock stock){
        List<StockWeightHistory> statusList = findByStockId(stock.getId());
        if(!statusList.isEmpty()){
            StockWeightHistory weightHistory = statusList.get(0);
            //System.out.println("saveLatestToStock: latest:" + statusHistory);
            stock.setWeight(weightHistory.getWeight());
            stock.setWeightDate(weightHistory.getSortDate());
            //save the stock item
            stockService.save(stock);
        }
    }
    
    public StockWeightHistory save(StockWeightHistory weightHistory, Stock stock){
        //save the passed status item
        StockWeightHistory updated = weightRepository.save(weightHistory);
        //update the stock item so it always has the newest status and date
        saveLatestToStock(stock);
        return updated;
    }
    
    public List<StockWeightHistory> findByStockId(Integer stockId){
        List<StockWeightHistory> list = weightRepository.findByStockId(stockId);
        Collections.sort(list, new WeightHistoryComparator().reversed());
        //System.out.println("findByStockId: sorted list:" + list);
        return list;
    }
    
    public Long getStatusCount(Integer stockId){
        Long count = weightRepository.countByStockId(stockId);
        return count;
    }

    
}
