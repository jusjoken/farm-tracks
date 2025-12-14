/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Litter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author birch
 */
@Transactional
public interface LitterRepository extends JpaRepository<Litter, UUID>  {

    public Long countByMotherId(Integer motherId);

    public Long countByFatherId(Integer fatherId);

    public List<Litter> findByMotherId(Integer id);

    public List<Litter> findByFatherId(Integer id);
    
    @Modifying
    @Query(value = "DELETE FROM litter", nativeQuery = true)
    public void deleteAllLittersNative();   
    
}
