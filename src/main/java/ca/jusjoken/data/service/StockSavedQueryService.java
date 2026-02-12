/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 *
 * @author birch
 */
@Service
public class StockSavedQueryService {
    
    private StockSavedQueryRepository queryRepository;
    private StockTypeRepository stockTypeRepository;

    public StockSavedQueryService(StockSavedQueryRepository queryRepository, StockTypeRepository stockTypeRepository) {
        this.queryRepository = queryRepository;
        this.stockTypeRepository = stockTypeRepository;
    }

    public List<StockSavedQuery> getSavedQueryList (){
        List<StockSavedQuery> list = queryRepository.findAll();
        if(list.isEmpty()){
            list.add(getDefaultSaveQuery());
        }
        return list;
    }
    
    public List<StockSavedQuery> getSavedQueryListByType (StockType type){
        List<StockSavedQuery> list = queryRepository.findAllByType(type.getName());
        if(list.isEmpty()) {
            list.add(getDefaultSaveQueryByType(type));
        }
        return list;
    }
    
    public StockSavedQuery getSavedQueryById(String id){
        Optional<StockSavedQuery> query = queryRepository.findById(Long.valueOf(id));
        if(query.isEmpty()){
            List<StockSavedQuery> list = getSavedQueryList();
            return list.get(0);
        }
        return query.get();
    }
    
    public StockSavedQuery getDefaultSaveQuery(){
        List<StockType> defaultStockType = stockTypeRepository.findDefault(Pageable.ofSize(1));
        ColumnSort sort1 = new ColumnSort("name", Sort.Direction.ASC);
        ColumnSort sort2 = new ColumnSort("tattoo", Sort.Direction.ASC);
        return new StockSavedQuery(0,"Default stock list",true, defaultStockType.get(0), new StockStatus().getDefault(), sort1, sort2);
    }
    
    public StockSavedQuery getDefaultSaveQueryByType(StockType type){
        ColumnSort sort1 = new ColumnSort("name", Sort.Direction.ASC);
        ColumnSort sort2 = new ColumnSort("tattoo", Sort.Direction.ASC);
        return new StockSavedQuery(type.getId(),"Default " + type.getName() + " list",true, type, new StockStatus().getDefault(), sort1, sort2);
    }
    
    public Integer saveAsQuery(StockSavedQuery query, String newName){
        return saveQuery(query, true, newName);
    }
    
    public Integer saveQuery(StockSavedQuery query){
        return saveQuery(query, Boolean.FALSE, query.getSavedQueryName());
    }
    
    private Integer saveQuery(StockSavedQuery query, Boolean saveAs, String newName){
        if(saveAs || query.getId().equals(0)){  //force save as for ID 0 as that is a virtual query
            //create new query and save it
            StockSavedQuery newQuery = new StockSavedQuery();
            newQuery.setBreeder(query.getBreeder());
            newQuery.setSavedQueryName(newName);
            newQuery.setSort1(query.getSort1());
            newQuery.setSort2(query.getSort2());
            newQuery.setStockStatus(query.getStockStatus());
            newQuery.setStockType(query.getStockType());
            newQuery.setViewStyle(query.getViewStyle());
            newQuery.setVisibleColumns(query.getVisibleColumns());
            StockSavedQuery addedQuery = queryRepository.save(newQuery);
            return addedQuery.getId();
        }else{
            //save passed in query
            queryRepository.save(query);
            return query.getId();
        }
    }
    
    public void deleteQuery(String queryId){
        queryRepository.deleteById(Long.parseLong(queryId));
    }
    
}
