package ca.jusjoken.data.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.TaskPlan;

@Repository
public interface TaskPlanRepository extends JpaRepository<TaskPlan, Integer> {
	java.util.List<TaskPlan> findByStatus(Utility.TaskPlanStatus status);
}
