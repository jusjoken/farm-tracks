package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.PlanTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
}
