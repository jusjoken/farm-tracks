/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Litter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author birch
 */
public interface LitterRepository extends JpaRepository<Litter, UUID>  {

    public Long countByMotherId(Long motherId);

    public Long countByFatherId(Long fatherId);

    public List<Litter> findByMotherId(Long id);

    public List<Litter> findByFatherId(Long id);
    
}
