/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 *
 * @author birch
 */
public interface StockRepository extends JpaRepository<Stock, Long>, QueryByExampleExecutor<Stock>  {
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

    @Query("select s from Stock s where s.breeder = true")
    List<Stock> findAllBreeders();

    public Long countByMotherId(Integer id);

    public Long countByFatherId(Integer id);
    
    public Long countByStockType(StockType stockType);

    public List<Stock> findAllKitsByLitterId(Integer id);

    public List<Stock> findAllByMotherId(Integer id);

    public List<Stock> findAllByFatherId(Integer id);

    @Query("select s from Stock s where s.breeder = true and s.sex = 'F' order by s.name")
    List<Stock> findAllMothers();
    
    @Query("select s from Stock s where s.breeder = true and s.sex = 'M' order by s.name")
    List<Stock> findAllFathers();
    
    public Stock findAllById(Integer id);
    
    
}
