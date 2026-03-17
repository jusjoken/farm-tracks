package ca.jusjoken.data.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.jusjoken.component.Badge;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.utility.TaskType;
import jakarta.transaction.Transactional;

@Service
public class TaskPlanService {

    private final TaskPlanRepository taskPlanRepository;
    private final StockService stockService;
    private final TaskService taskService;

    public TaskPlanService(TaskPlanRepository taskPlanRepository, StockService stockService, TaskService taskService) {
        this.taskPlanRepository = taskPlanRepository;
        this.stockService = stockService;
        this.taskService = taskService;
    }

    //find all task plans that have an associated task with a task type of BIRTH that is not complete
    public List<TaskPlan> findAllIncompleteBirthPlans() {
        List<TaskPlan> allPlans = taskPlanRepository.findAll();
        return allPlans.stream()
                .filter(plan -> {
                    List<Task> tasks = taskService.findByPlanId(plan.getId());
                    return tasks.stream().anyMatch(task -> !task.getCompleted() && task.getType() == TaskType.BIRTH);
                })
                .toList();
    }

    public List<TaskPlan> findAll() {
        return taskPlanRepository.findAll();
    }

    public List<TaskPlan> findByStatus(ca.jusjoken.data.Utility.TaskPlanStatus status) {
        return taskPlanRepository.findAll().stream()
                .filter(plan -> plan.getStatus() == status)
                .toList();
    }

    public Optional<TaskPlan> findById(Integer id) {
        return taskPlanRepository.findById(id);
    }

    public TaskPlan save(TaskPlan taskPlan) {
        return taskPlanRepository.save(taskPlan);
    }

    @Transactional
    public Optional<TaskPlan> markIncomplete(Integer id) {
        return taskPlanRepository.findById(id)
                .map(plan -> {
                    plan.setStatus(Utility.TaskPlanStatus.INCOMPLETE);
                    return taskPlanRepository.save(plan);
                });
    }

    @Transactional
    public Optional<TaskPlan> markActive(Integer id) {
        return taskPlanRepository.findById(id)
                .map(plan -> {
                    boolean hasIncompleteTasks = taskService.findByPlanId(plan.getId()).stream()
                            .anyMatch(task -> !Boolean.TRUE.equals(task.getCompleted()));
                    plan.setStatus(hasIncompleteTasks ? Utility.TaskPlanStatus.ACTIVE : Utility.TaskPlanStatus.INACTIVE);
                    return taskPlanRepository.save(plan);
                });
    }

    @Transactional
    public int reconcileStatusesForAllPlans() {
        int updated = 0;
        List<TaskPlan> plans = taskPlanRepository.findAll();
        for (TaskPlan plan : plans) {
            if (plan.getStatus() == Utility.TaskPlanStatus.INCOMPLETE) {
                // Keep manually abandoned plans as INCOMPLETE.
                continue;
            }

            List<Task> tasks = taskService.findByPlanId(plan.getId());
            if (tasks.isEmpty()) {
                continue;
            }

            boolean hasIncompleteTasks = tasks.stream()
                    .anyMatch(task -> !Boolean.TRUE.equals(task.getCompleted()));
            Utility.TaskPlanStatus targetStatus = hasIncompleteTasks
                    ? Utility.TaskPlanStatus.ACTIVE
                    : Utility.TaskPlanStatus.INACTIVE;

            if (plan.getStatus() != targetStatus) {
                plan.setStatus(targetStatus);
                taskPlanRepository.save(plan);
                updated++;
            }
        }
        return updated;
    }

    public void deleteById(Integer id) {
        taskPlanRepository.deleteById(id);
    }

    //add a method to create a displayname for a taskplan that includes the father, mother and the date for the task associated with a sequence of 1
    public String getDisplayName(TaskPlan taskPlan) {
        String fatherName = "Unknown Father";
        String motherName = "Unknown Mother";
        String dateInfo = "";

        if (taskPlan.getLinkFatherId() != null) {
            Stock fatherOpt = stockService.findById(taskPlan.getLinkFatherId());
            fatherName = fatherOpt.getName();
        }

        if (taskPlan.getLinkMotherId() != null) {
            Stock motherOpt = stockService.findById(taskPlan.getLinkMotherId());
            motherName = motherOpt.getName();
        }

        List<Task> tasks = taskService.findByPlanId(taskPlan.getId());
        if (!tasks.isEmpty()) {
            var firstTask = tasks.get(0);
            if (firstTask.getDate() != null) {
                dateInfo = firstTask.getDate().toString();
            }
        }

        return dateInfo + ": " + fatherName + " / " + motherName;
    }

    public Badge getDisplayNameBadge(TaskPlan taskPlan) {
        String dateInfo = "";
        String linkTypeInfo = "";

        linkTypeInfo = switch (taskPlan.getType()) {
            case BREEDER -> "Breeder";
            case LITTER -> "Litter";
            default -> "General";
        };
        List<Task> tasks = taskService.findByPlanId(taskPlan.getId());
        if (!tasks.isEmpty()) {
            var firstTask = tasks.get(0);
            if (firstTask.getDate() != null) {
                dateInfo = firstTask.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
        }
        Badge badge = new Badge(linkTypeInfo + " Plan: " + dateInfo);
        badge.addThemeVariants(BadgeVariant.PILL);
        if(linkTypeInfo.equals("Breeder")) {
            badge.addThemeVariants(BadgeVariant.SUCCESS);
        } else if (linkTypeInfo.equals("Litter")) {
            badge.addThemeVariants(BadgeVariant.CONTRAST);
        } else {
            badge.addThemeVariants(BadgeVariant.WARNING);
        }

        return badge;
    }

}
