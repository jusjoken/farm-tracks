package ca.jusjoken.data.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.jusjoken.data.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer>  {

    void deleteAllByTaskPlanId(Integer id);
    
    List<Task> findByTaskPlanId(Integer id);
}
