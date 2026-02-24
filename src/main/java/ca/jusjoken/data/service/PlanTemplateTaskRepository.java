package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.jusjoken.data.entity.PlanTemplateTask;

@Repository
public interface PlanTemplateTaskRepository extends JpaRepository<PlanTemplateTask, Integer> {

    List<PlanTemplateTask> findAllByPlanTemplateId(Integer planTemplateId);
}
