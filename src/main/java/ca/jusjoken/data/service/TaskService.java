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
        return taskRepository.findAll().stream()
                .filter(task -> (task.getLinkType() == null || task.getLinkType() == Utility.TaskLinkType.BREEDER) 
                        && task.getLinkBreederId() != null && task.getLinkBreederId().equals(stock.getId()))
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
    

}
