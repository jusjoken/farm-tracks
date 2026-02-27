package ca.jusjoken.data.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.entity.TaskPlan;

@Service
public class TaskPlanService {

    private final TaskPlanRepository taskPlanRepository;

    public TaskPlanService(TaskPlanRepository taskPlanRepository) {
        this.taskPlanRepository = taskPlanRepository;
    }

    public List<TaskPlan> findAll() {
        return taskPlanRepository.findAll();
    }

    public Optional<TaskPlan> findById(Integer id) {
        return taskPlanRepository.findById(id);
    }

    public TaskPlan save(TaskPlan taskPlan) {
        return taskPlanRepository.save(taskPlan);
    }

    public void deleteById(Integer id) {
        taskPlanRepository.deleteById(id);
    }
}
