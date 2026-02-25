package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.jusjoken.data.Utility.TaskLinkType;
import ca.jusjoken.data.entity.PlanTemplate;

@Repository
public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, Integer> {

    List<PlanTemplate> findByType(TaskLinkType breeder);

}
