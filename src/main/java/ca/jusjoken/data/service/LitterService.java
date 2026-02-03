/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author birch
 */
@Service
public class LitterService {
    private LitterRepository litterRepository;

    public LitterService(LitterRepository litterRepository) {
        this.litterRepository = litterRepository;
    }
    
    public List<Litter> getLitters(Stock stock){
        List<Litter> litters = new ArrayList<>();
        if(!stock.getBreeder() || stock.getSex().equals(Utility.Gender.NA)) return List.of();
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            return litterRepository.findByMotherId(stock.getId());
        }else{ //male
            return litterRepository.findByFatherId(stock.getId());
        }
        
    }
    
    public Long getLitterCountForParent(Stock stock){
        List<Litter> litters = new ArrayList<>();
        if(!stock.getBreeder() || stock.getSex().equals(Utility.Gender.NA)) return 0L;
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            return litterRepository.countByMotherId(stock.getId());
        }else{ //male
            return litterRepository.countByFatherId(stock.getId());
        }
        
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
}
