/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.StockType;

/**
 *
 * @author birch
 */
@Service
public class StockTypeService {
    private final StockTypeRepository typeRepository;

    public StockTypeService(StockTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }
    
    public List<StockType> findAllStockTypes(){
        return typeRepository.findAll(Sort.by("name"));
    }
    
    public StockType findById(Integer id){
        return typeRepository.findById(id).get();
    }
    
    public StockType findRabbits(){
        return typeRepository.findByName("Rabbits");
    }

    public String getGenderForType(Gender gender, StockType type){
        if(gender.equals(Gender.MALE)){
            return type.getMaleName();
        }else if(gender.equals(Gender.FEMALE)){
            return type.getFemaleName();
        }else{
            return "NA";
        }
    }
}
