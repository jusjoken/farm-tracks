/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.StockType;

/**
 *
 * @author birch
 */
@Transactional
public interface LitterRepository extends JpaRepository<Litter, Integer>  {

    public Long countByMotherId(Integer motherId);

    public Long countByFatherId(Integer fatherId);

    public List<Litter> findByMotherId(Integer id);

    public List<Litter> findByFatherId(Integer id);
    
    @Query("select l from Litter l where l.archived IS NULL and l.stockType = :stocktype")
    public List<Litter> findNotArchived(@Param("stocktype") StockType stockType);
    
    @Modifying
    @Query(value = "DELETE FROM litter", nativeQuery = true)
    public void deleteAllLittersNative();   
    
    public void deleteByFatherId(Integer stockId);
    public void deleteByMotherId(Integer stockId);
    
}
