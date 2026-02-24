package ca.jusjoken.data.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.entity.PlanTemplateTask;

@Service
public class PlanTemplateTaskService {

    private final PlanTemplateTaskRepository planTemplateTaskRepository;

    public PlanTemplateTaskService(PlanTemplateTaskRepository planTemplateTaskRepository) {
        this.planTemplateTaskRepository = planTemplateTaskRepository;
    }

    public List<PlanTemplateTask> findAll() {
        return planTemplateTaskRepository.findAll();
    }

    public List<PlanTemplateTask> findAllByPlanTemplateId(Integer planTemplateId) {
        return planTemplateTaskRepository.findAllByPlanTemplateId(planTemplateId);
    }

    public Optional<PlanTemplateTask> findById(Integer id) {
        return planTemplateTaskRepository.findById(id);
    }

    public PlanTemplateTask save(PlanTemplateTask planTemplateTask) {
        return planTemplateTaskRepository.save(planTemplateTask);
    }

    public void deleteById(Integer id) {
        planTemplateTaskRepository.deleteById(id);
    }
}
