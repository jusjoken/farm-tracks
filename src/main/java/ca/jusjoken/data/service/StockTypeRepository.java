/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 *
 * @author birch
 */
public interface StockTypeRepository extends JpaRepository<StockType, UUID> {

    @Query("select s from StockType s where s.name = :stockTypeName")
    StockType findByName(@Param("stockTypeName") String stockTypeName);

    @Query("select s from StockType s where s.defaultType = true")
    List<StockType> findDefault(Pageable limit);
    
    @Query("select s from StockType s")
    List<StockType> findFirst(PageRequest limit);

    public StockType findFirstByOrderByDefaultType();

    public StockType findFirstByOrderByDefaultTypeDesc();
    
    public StockType findById(Integer id);
    
}
