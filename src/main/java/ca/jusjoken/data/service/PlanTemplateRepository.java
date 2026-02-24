package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.PlanTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, Integer> {

}
