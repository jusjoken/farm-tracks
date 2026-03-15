package ca.jusjoken.data.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.icon.Icon;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.Badge;
import ca.jusjoken.component.Layout;
import ca.jusjoken.component.Layout.FlexDirection;
import ca.jusjoken.data.Utility;
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.utility.TaskType;
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
    private TaskType type;
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
    private Icon icon;
    

    public Task() {
    }

    public Task(TaskType type, String name, LocalDate date, Utility.TaskLinkType linkType, Integer linkBreederId, Integer linkLitterId) {
        this.type = type;
        this.name = name;
        this.date = date;
        this.linkType = linkType;
        this.linkBreederId = linkBreederId;
        this.linkLitterId = linkLitterId;
        this.icon = type != null ? type.getIcon() : null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
        this.icon = type != null ? type.getIcon() : null;
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

    public String getPlanName() {
        if (this.taskPlan != null) {
            return this.taskPlan.getType().getShortName() + " Plan";
        }
        return null;
    }
    

    @Override
    public String toString() {
        return "Task [id=" + id + ", type=" + type + ", name=" + name + ", date=" + date + ", linkType=" + linkType
                + ", linkBreederId=" + linkBreederId + ", linkLitterId=" + linkLitterId + ", completed=" + completed
                + ", taskPlan=" + getPlanName() + "]";
    }

    public Layout getHeader() {
        Layout headerLayout = new Layout();
        headerLayout.setFlexDirection(FlexDirection.ROW);
        if(this.icon != null){
            headerLayout.add(this.icon);
        }
        headerLayout.add(this.name);
        return headerLayout;
    }

    public Badge getStatusBadge(){
        Badge statusBadge = new Badge();
        if(this.getCompleted()){
            statusBadge.setText("Completed");
            statusBadge.addThemeVariants(BadgeVariant.ERROR);
            return statusBadge;
        }else{
            statusBadge.setText("Active");
            statusBadge.addThemeVariants(BadgeVariant.SUCCESS);
            return statusBadge;
        }
    }

    public Badge getDueDateBadge(){
        Badge dueDateBadge = new Badge();
        if(this.getDate() != null){
            if(this.getCompleted()){
                return UIUtilities.createBadge("Completed", this.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy")), BadgeVariant.CONTRAST);
            }
            if (date.isBefore(LocalDate.now())) {
                return UIUtilities.createBadge("Overdue", this.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy")), BadgeVariant.ERROR);
            } else if (date.isAfter(LocalDate.now())) {
                return UIUtilities.createBadge("Due: ", this.getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy")), BadgeVariant.SUCCESS);
            }
        }
        dueDateBadge.setText("No Due Date");
        dueDateBadge.addThemeVariants(BadgeVariant.WARNING);
        return dueDateBadge;
    }

    public Icon getIcon() {
        return icon;
    }

    


}
