package ca.jusjoken.data.service;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.jusjoken.data.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Integer>  {
    

}
