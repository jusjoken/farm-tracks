/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;

/**
 *
 * @author birch
 */
@Service
public class LitterService {
    private final LitterRepository litterRepository;
    private final StockService stockService;
    private final StockStatusHistoryService stockStatusHistoryService;
    private final StockWeightHistoryService stockWeightHistoryService;

    public LitterService(LitterRepository litterRepository, StockService stockService, StockStatusHistoryService stockStatusHistoryService, StockWeightHistoryService stockWeightHistoryService) {
        this.litterRepository = litterRepository;
        this.stockService = stockService;
        this.stockStatusHistoryService = stockStatusHistoryService;
        this.stockWeightHistoryService = stockWeightHistoryService;
    }
    
    public List<Litter> getLitters(Stock stock){
        if(!stock.getBreeder() || stock.getSex().equals(Utility.Gender.NA)) return List.of();
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            List<Litter> litters = litterRepository.findByMotherId(stock.getId());
            Collections.sort(litters, new LitterComparator().reversed());
            return litters;
        }else{ //male
            List<Litter> litters = litterRepository.findByFatherId(stock.getId());
            Collections.sort(litters, new LitterComparator().reversed());
            return litters;
        }
        
    }
    
    public Long getLitterCountForParent(Stock stock){
        if(!stock.getBreeder() || stock.getSex().equals(Utility.Gender.NA)) return 0L;
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            return litterRepository.countByMotherId(stock.getId());
        }else{ //male
            return litterRepository.countByFatherId(stock.getId());
        }
        
    }
    
    public List<Litter> getActiveLitters(StockType stockType){
        List<Litter> litters = litterRepository.findNotArchived(stockType);
        Collections.sort(litters, new LitterComparator().reversed());
        return litters;
    }
    
    public List<Litter> getActiveLitters(){
        List<Litter> litters = litterRepository.findNotArchived();
        Collections.sort(litters, new LitterComparator().reversed());
        return litters;
    }
    
    @Transactional
    public void deleteByStockId(Integer stockId){
        litterRepository.deleteByFatherId(stockId);
        litterRepository.deleteByMotherId(stockId);
    }

    public Litter findById(Integer linkLitterId) {
        return litterRepository.findById(linkLitterId).orElse(null);
    }

    public List<Litter> getAllLitters() {
        List<Litter> litters = litterRepository.findAll();
        Collections.sort(litters, new LitterComparator().reversed());
        return litters;
    }

    public List<Litter> getAllLitters(StockType stockType) {
        List<Litter> litters = litterRepository.findAllByStockType(stockType);
        Collections.sort(litters, new LitterComparator().reversed());
        return litters;
    }

    public Litter save(Litter litter) {
        return litterRepository.save(litter);
    }

    public String getNextLitterName(String prefixToUse) {
        List<String> litterNames = litterRepository.findLitterNamesByPrefixAndStockType(prefixToUse);
        String nextName = litterNames.isEmpty() ? null : litterNames.get(0);
        System.out.println("Next litter name: " + nextName);
        //increase the number after the prefix by 1, if there is no number after the prefix then add 1
        if(nextName == null) {
            return prefixToUse + " 1";
        }
        String numberPart = nextName.substring(prefixToUse.length()).trim();
        int nextNumber = 1;
        if(!numberPart.isEmpty()) {
            try {
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                // If parsing fails, just return the prefix with " 1"
                return prefixToUse + " 1";
            }
        }
        return prefixToUse + nextNumber;
    }

    public void deleteById(Integer id) {
        //find all stock with this litterid and delete them, then delete the litter
        List<Stock> stocksToDelete = stockService.getKitsForLitter(id);
        System.out.println("Deleting litter with ID: " + id + ". Found " + stocksToDelete.size() + " associated kits to delete.");
        for(Stock stock : stocksToDelete){
            System.out.println("Deleting stock with ID: " + stock.getId() + " associated with litter ID: " + id);
            stockStatusHistoryService.deleteByStockId(stock.getId());
            System.out.println("Deleted stock status history for stock ID: " + stock.getId());
            stockWeightHistoryService.deleteByStockId(stock.getId());
            System.out.println("Deleted stock weight history for stock ID: " + stock.getId());
            stockService.delete(stock.getId());
            System.out.println("Deleted stock with ID: " + stock.getId());
        }
        litterRepository.deleteById(id);
        System.out.println("Deleted litter with ID: " + id);
    }
}
