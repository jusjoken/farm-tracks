/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;

/**
 *
 * @author birch
 */
public interface StockRepository extends JpaRepository<Stock, Long>  {
    @Query("select s from Stock s where s.name = :stockName and s.tattoo = :stockTattoo")
    Stock findByNameAndTattoo(@Param("stockName") String stockName, @Param("stockTattoo") String stockTattoo);
    
    @Query("select s from Stock s where s.motherId = null")
    List<Stock> findAllMothersMissingId();
    
    @Query("select s from Stock s where s.fatherId = null")
    List<Stock> findAllFathersMissingId();

    @Query("select s from Stock s where s.tattoo like :litterName%")
    List<Stock> findAllKitsByLitterName(@Param("litterName") String litterName);
    
    @Query("select s from Stock s where s.motherId = :motherId and s.fatherId = :fatherId and s.doB = :doB")
    List<Stock> findAllKitsByMotherFatherDoB(@Param("motherId") Integer motherId, @Param("fatherId") Integer fatherId, @Param("doB") LocalDate doB);

    @Query("select s from Stock s where s.breeder = true and s.stockType.id = :stock_type_id")
    List<Stock> findAllBreeders(@Param("stock_type_id") Integer stockTypeId);

    public Long countByMotherId(Integer id);

    public Long countByFatherId(Integer id);
    
    public Long countByStockType(StockType stockType);

    public List<Stock> findAllKitsByLitterId(Integer id);

    public List<Stock> findAllByMotherId(Integer id);

    public List<Stock> findAllByFatherId(Integer id);

    @Query("select s from Stock s where s.breeder = true and s.sex = 'F' and s.stockType.id = :stock_type_id order by s.name")
    List<Stock> findAllMothers(@Param("stock_type_id") Integer stockTypeId);
    
    @Query("select s from Stock s where s.breeder = true and s.sex = 'M' and s.stockType.id = :stock_type_id order by s.name")
    List<Stock> findAllFathers(@Param("stock_type_id") Integer stockTypeId);
    
    public Stock findAllById(Integer id);
    
    public List<Stock> findAllByStockTypeId(Integer id);
    
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM stock WHERE stock_type_id = :stock_type_id", nativeQuery = true)
    public int deleteByStockType(@Param("stock_type_id") Integer stockTypeId);

}
