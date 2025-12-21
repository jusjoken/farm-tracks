/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockType;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 *
 * @author birch
 */
@Service
public class StockTypeService {
    private StockTypeRepository typeRepository;

    public StockTypeService(StockTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }
    
    public List<StockType> findAllStockTypes(){
        return typeRepository.findAll(Sort.by("name"));
    }
}
