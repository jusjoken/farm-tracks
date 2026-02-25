package ca.jusjoken.data.entity;

import java.time.LocalDate;

import ca.jusjoken.data.Utility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    private Utility.TaskType type;
    private String name;
    private LocalDate date;
    private Utility.TaskLinkType linkType;
    private Integer linkBreederId;
    private Integer linkLitterId;
    private Boolean completed = false;
    //add a link to the taskplan table by adding a taskPlanId field and a @ManyToOne relationship to the TaskPlan entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_plan_id", nullable = true)
    private TaskPlan taskPlan;
    

    public Task() {
    }

    public Task(Utility.TaskType type, String name, LocalDate date, Utility.TaskLinkType linkType, Integer linkBreederId, Integer linkLitterId) {
        this.type = type;
        this.name = name;
        this.date = date;
        this.linkType = linkType;
        this.linkBreederId = linkBreederId;
        this.linkLitterId = linkLitterId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utility.TaskType getType() {
        return type;
    }

    public void setType(Utility.TaskType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Utility.TaskLinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(Utility.TaskLinkType linkType) {
        this.linkType = linkType;
    }

    public Integer getLinkBreederId() {
        return linkBreederId;
    }

    public void setLinkBreederId(Integer linkBreederId) {
        this.linkBreederId = linkBreederId;
    }

    public Integer getLinkLitterId() {
        return linkLitterId;
    }

    public void setLinkLitterId(Integer linkLitterId) {
        this.linkLitterId = linkLitterId;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public TaskPlan getTaskPlan() {
        return taskPlan;
    }   

    public void setTaskPlan(TaskPlan taskPlan) {
        this.taskPlan = taskPlan;
    }
    

    @Override
    public String toString() {
        return "Task [id=" + id + ", type=" + type + ", name=" + name + ", date=" + date + ", linkType=" + linkType
                + ", linkBreederId=" + linkBreederId + ", linkLitterId=" + linkLitterId + ", completed=" + completed
                + "]";
    }

    


}
