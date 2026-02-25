package ca.jusjoken.data.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.TaskLinkType;
import ca.jusjoken.data.entity.PlanTemplate;

@Service
public class PlanTemplateService {

    private final PlanTemplateRepository planTemplateRepository;

    public PlanTemplateService(PlanTemplateRepository planTemplateRepository) {
        this.planTemplateRepository = planTemplateRepository;
    }

    public List<PlanTemplate> findAll() {
        return planTemplateRepository.findAll();
    }

    public Optional<PlanTemplate> findById(Integer id) {
        return planTemplateRepository.findById(id);
    }

    public PlanTemplate save(PlanTemplate planTemplate) {
        return planTemplateRepository.save(planTemplate);
    }

    public void deleteById(Integer id) {
        planTemplateRepository.deleteById(id);
    }

    public List<PlanTemplate> findAllBreederPlanTemplates() {
        return planTemplateRepository.findByType(Utility.TaskLinkType.BREEDER);
    }

    public List<PlanTemplate> findAllGeneralPlanTemplates() {
        return planTemplateRepository.findByType(Utility.TaskLinkType.GENERAL);
    }

    public List<PlanTemplate> findAllByTaskLinkType(TaskLinkType selectedTaskLinkType) {
        return planTemplateRepository.findByType(selectedTaskLinkType);
    }
}
