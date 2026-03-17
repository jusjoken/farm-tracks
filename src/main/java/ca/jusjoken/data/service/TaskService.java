package ca.jusjoken.data.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import jakarta.transaction.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskPlanRepository taskPlanRepository;

    public TaskService(TaskRepository taskRepository, TaskPlanRepository taskPlanRepository) {
        this.taskRepository = taskRepository;
        this.taskPlanRepository = taskPlanRepository;
    }

    public void save(Task entity){
        this.taskRepository.save(entity);
    }

    @Transactional
    public void saveWithPlanStatusSync(Task entity) {
        Task saved = this.taskRepository.save(entity);
        syncPlanStatusFromTask(saved);
    }

    @Transactional
    public void setTaskCompleted(Task task, boolean completed) {
        if (task == null) {
            return;
        }
        task.setCompleted(completed);
        Task saved = this.taskRepository.save(task);
        syncPlanStatusFromTask(saved);
    }

    public Optional<Task> findById(Integer id) {
        return taskRepository.findById(id);
    }

    public List<Task> findByStockId(Integer id) {
        return taskRepository.findAll().stream()
                .filter(task -> (task.getLinkType() == null || task.getLinkType() == Utility.TaskLinkType.BREEDER) 
                        && task.getLinkBreederId() != null && task.getLinkBreederId().equals(id))
                .toList();
    }

    public Object getTaskCountForStock(Stock stock) {
        //add aditional filter to only count tasks where completed is false
        return taskRepository.findAll().stream()
                .filter(task -> (task.getLinkType() == null || task.getLinkType() == Utility.TaskLinkType.BREEDER) 
                        && task.getLinkBreederId() != null && task.getLinkBreederId().equals(stock.getId())
                        && !task.getCompleted())
                .count();
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    public void deleteAllByTaskPlanId(Integer id) {
        taskRepository.deleteAllByTaskPlanId(id);
    }

    public void deleteById(Integer id) {
        taskRepository.deleteById(id);
    }

    public List<Task> findByPlanId(Integer taskPlanId) {
        return taskRepository.findByTaskPlanId(taskPlanId);
    }

    private void syncPlanStatusFromTask(Task task) {
        if (task == null) {
            return;
        }

        Integer taskPlanId = null;
        try {
            if (task.getTaskPlan() != null) {
                taskPlanId = task.getTaskPlan().getId();
            }
        } catch (Exception ignored) {
            // fallback below
        }

        if (taskPlanId == null && task.getId() != null) {
            taskPlanId = taskRepository.findById(task.getId())
                    .map(reloaded -> reloaded.getTaskPlan() != null ? reloaded.getTaskPlan().getId() : null)
                    .orElse(null);
        }

        if (taskPlanId == null) {
            return;
        }

        Optional<TaskPlan> planOpt = taskPlanRepository.findById(taskPlanId);
        if (planOpt.isEmpty()) {
            return;
        }

        TaskPlan plan = planOpt.get();
        long incompleteCount = taskRepository.countByTaskPlanIdAndCompletedFalse(taskPlanId);
        Utility.TaskPlanStatus targetStatus = incompleteCount > 0
                ? Utility.TaskPlanStatus.ACTIVE
                : Utility.TaskPlanStatus.INACTIVE;

        if (plan.getStatus() != targetStatus) {
            plan.setStatus(targetStatus);
            taskPlanRepository.save(plan);
        }
    }
    
    public void runTaskActions(Task task) {
        // Intentionally left as non-UI service hook.
        // UI actions (opening editors/dialogs) are handled in component layer (e.g., LitterEditor).
        // Keep this method only for future server-side business actions if needed.
    }


}
