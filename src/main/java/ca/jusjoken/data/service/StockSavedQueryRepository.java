/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockSavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author birch
 */
public interface StockSavedQueryRepository extends JpaRepository<StockSavedQuery, Long>{
    
}
