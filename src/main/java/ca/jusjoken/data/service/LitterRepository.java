/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findByMotherId(Integer id);

    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findByFatherId(Integer id);

    @Query("select l.mother.id, count(l) from Litter l where l.mother.id in :ids group by l.mother.id")
    List<Object[]> countByMotherIds(@Param("ids") List<Integer> ids);

    @Query("select l.father.id, count(l) from Litter l where l.father.id in :ids group by l.father.id")
    List<Object[]> countByFatherIds(@Param("ids") List<Integer> ids);
    
    @Query("select l from Litter l where l.archived IS NULL and l.stockType = :stocktype")
    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findNotArchived(@Param("stocktype") StockType stockType);

    @Query("select l from Litter l where l.archived IS NULL")
    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findNotArchived();
    

    @Modifying
    @Query(value = "DELETE FROM litter", nativeQuery = true)
    public void deleteAllLittersNative();   
    
    public void deleteByFatherId(Integer stockId);
    public void deleteByMotherId(Integer stockId);

    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findAllByStockType(StockType stockType);

    @Override
    @EntityGraph(attributePaths = { "mother", "father", "stockType" })
    public List<Litter> findAll();

    @Query("""
        select l.name
        from Litter l
        where l.name like concat(:prefixToUse, '%')
        order by length(l.name) desc, l.name desc
    """)
    // this query will return names like "LitterA", "LitterA1", "LitterA2", etc. and we can take the first result to find the next litter name
    public List<String> findLitterNamesByPrefixAndStockType(@Param("prefixToUse") String prefixToUse);

}
