/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ca.jusjoken.data.entity.StockSavedQuery;

/**
 *
 * @author birch
 */
public interface StockSavedQueryRepository extends JpaRepository<StockSavedQuery, Long>{
    @Query("select s from StockSavedQuery s where s.stockType.name = :type")
    List<StockSavedQuery> findAllByType(@Param("type") String type);
    
}
