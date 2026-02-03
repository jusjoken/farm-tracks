/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockWeightHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author birch
 */
public interface StockWeightHistoryRepository extends JpaRepository<StockWeightHistory, UUID> {
    
    public List<StockWeightHistory> findByStockId(Integer stockId);
    
    public Long countByStockId(Integer stockId);
    
    public void deleteAllByStockId(Integer stockId);
}
