/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

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
        if(!stock.isBreeder() || stock.getSex().equals(Utility.Gender.NA)) return List.of();
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            return litterRepository.findByMotherId(stock.getId());
        }else{ //male
            return litterRepository.findByFatherId(stock.getId());
        }
        
    }
    
}
