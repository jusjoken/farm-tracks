package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;

@Service
public class TaskService {

    private TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void save(Task entity){
        this.taskRepository.save(entity);
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
    
    public void runTaskActions(Task task) {
        // Intentionally left as non-UI service hook.
        // UI actions (opening editors/dialogs) are handled in component layer (e.g., LitterEditor).
        // Keep this method only for future server-side business actions if needed.
    }


}
