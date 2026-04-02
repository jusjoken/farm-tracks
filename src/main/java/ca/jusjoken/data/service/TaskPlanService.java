package ca.jusjoken.data.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
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

    public List<TaskPlan> findByStockId(Integer stockId) {
        if (stockId == null) {
            return List.of();
        }

        return taskPlanRepository.findAll().stream()
                .filter(plan -> plan != null
                        && (stockId.equals(plan.getLinkMotherId()) || stockId.equals(plan.getLinkFatherId())))
                .toList();
    }

    public long countByStockId(Integer stockId) {
        return findByStockId(stockId).size();
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

    @Transactional
    public void deletePlanAndTasks(Integer id) {
        if (id == null) {
            return;
        }

        taskService.deleteAllByTaskPlanId(id);
        taskPlanRepository.deleteById(id);
    }

    //add a method to create a displayname for a taskplan that includes the father, mother and the date for the task associated with a sequence of 1
    public String getDisplayName(TaskPlan taskPlan) {
        if (taskPlan == null) {
            return "Plan";
        }

        String fatherName = "Unknown Father";
        String motherName = "Unknown Mother";
        String dateInfo = "";

        if (taskPlan.getLinkFatherId() != null) {
            Stock fatherOpt = stockService.findById(taskPlan.getLinkFatherId());
            if (fatherOpt != null && fatherOpt.getName() != null && !fatherOpt.getName().isBlank()) {
                fatherName = fatherOpt.getName();
            }
        }

        if (taskPlan.getLinkMotherId() != null) {
            Stock motherOpt = stockService.findById(taskPlan.getLinkMotherId());
            if (motherOpt != null && motherOpt.getName() != null && !motherOpt.getName().isBlank()) {
                motherName = motherOpt.getName();
            }
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

    public HorizontalLayout getHeader(TaskPlan plan) {
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setSpacing(true);

        if (plan == null) {
            headerLayout.add("General Plan");
            return headerLayout;
        }

        // Icon for plan type
        Icon typeIcon = null;
        if (plan.getType() != null) {
            switch (plan.getType()) {
                case BREEDER -> typeIcon = new Icon(ca.jusjoken.data.Utility.ICONS.TYPE_BREEDER.getIconSource());
                case LITTER -> typeIcon = new Icon(ca.jusjoken.data.Utility.ICONS.TYPE_NESTBOX.getIconSource());
                case GENERAL -> typeIcon = new Icon(ca.jusjoken.data.Utility.ICONS.TYPE_CUSTOM.getIconSource());
                default -> typeIcon = null;
            }
        }
        if (typeIcon != null) {
            typeIcon.setSize("1.2em");
            headerLayout.add(typeIcon);
        }

        String dateInfo = "";
        String linkTypeInfo = "";

        Utility.TaskLinkType linkType = plan != null ? plan.getType() : null;
        if (linkType == Utility.TaskLinkType.BREEDER) {
            linkTypeInfo = "Breeder";
        } else if (linkType == Utility.TaskLinkType.LITTER) {
            linkTypeInfo = "Litter";
        } else {
            linkTypeInfo = "General";
        }
        List<Task> tasks = taskService.findByPlanId(plan.getId());
        if (!tasks.isEmpty()) {
            var firstTask = tasks.get(0);
            if (firstTask.getDate() != null) {
                dateInfo = firstTask.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
        }

        headerLayout.add(linkTypeInfo + " Plan: " + dateInfo);
        return headerLayout;
    }



    public Badge getDisplayNameBadge(TaskPlan taskPlan) {
        if (taskPlan == null) {
            Badge fallbackBadge = new Badge("General Plan");
            fallbackBadge.addThemeVariants(BadgeVariant.WARNING);
            return fallbackBadge;
        }

        String dateInfo = "";
        String linkTypeInfo = "";

        Utility.TaskLinkType linkType = taskPlan != null ? taskPlan.getType() : null;
        if (linkType == Utility.TaskLinkType.BREEDER) {
            linkTypeInfo = "Breeder";
        } else if (linkType == Utility.TaskLinkType.LITTER) {
            linkTypeInfo = "Litter";
        } else {
            linkTypeInfo = "General";
        }
        List<Task> tasks = taskService.findByPlanId(taskPlan.getId());
        if (!tasks.isEmpty()) {
            var firstTask = tasks.get(0);
            if (firstTask.getDate() != null) {
                dateInfo = firstTask.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            }
        }
        Badge badge = new Badge(linkTypeInfo + " Plan: " + dateInfo);
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
