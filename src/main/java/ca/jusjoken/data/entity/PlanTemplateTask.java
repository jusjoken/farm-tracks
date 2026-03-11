package ca.jusjoken.data.entity;

import ca.jusjoken.data.Utility;
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
public class PlanTemplateTask {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    private TaskType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_template_id", nullable = false)
    private PlanTemplate planTemplate;

    private Integer daysFromStart;
    private Integer sequence;
    private String customName = null;

    public PlanTemplateTask() {
    }

    public PlanTemplateTask(TaskType type, PlanTemplate planTemplate, Integer daysFromStart, Integer sequence) {
        this.type = type;
        this.planTemplate = planTemplate;
        this.daysFromStart = daysFromStart;
        this.sequence = sequence;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public String getDisplayName() {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return type != null ? type.getDisplayName() : "Unnamed Task";
    }

    public Integer getDaysFromStart() {
        return daysFromStart;
    }

    public void setDaysFromStart(Integer daysFromStart) {
        this.daysFromStart = daysFromStart;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public PlanTemplate getPlanTemplate() {
        return planTemplate;
    }

    public void setPlanTemplate(PlanTemplate planTemplate) {
        this.planTemplate = planTemplate;
    }

    @Override
    public String toString() {
        return "PlanTemplateTask [id=" + id + ", type=" + type + ", planTemplate=" + planTemplate + ", daysFromStart="
                + daysFromStart + ", sequence=" + sequence + ", customName=" + customName + "]";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
}
