package ca.jusjoken.data.entity;
import ca.jusjoken.data.Utility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


@Entity
public class TaskPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    private Utility.TaskLinkType type;

    private Integer linkFatherId;
    private Integer linkMotherId;

    private Utility.TaskPlanStatus status = Utility.TaskPlanStatus.ACTIVE;

    public TaskPlan() {
    }

    public TaskPlan(Utility.TaskLinkType type, Integer linkFatherId, Integer linkMotherId) {
        this.type = type;
        this.linkFatherId = linkFatherId;
        this.linkMotherId = linkMotherId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utility.TaskLinkType getType() {
        return type;
    }

    public void setType(Utility.TaskLinkType type) {
        this.type = type;
    }

    public Integer getLinkFatherId() {
        return linkFatherId;
    }

    public void setLinkFatherId(Integer linkFatherId) {
        this.linkFatherId = linkFatherId;
    }

    public Integer getLinkMotherId() {
        return linkMotherId;
    }

    public void setLinkMotherId(Integer linkMotherId) {
        this.linkMotherId = linkMotherId;
    }

    @Override
    public String toString() {
        return "TaskPlan [id=" + id + ", type=" + type + ", linkFatherId=" + linkFatherId + ", linkMotherId="
                + linkMotherId + ", status=" + status + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TaskPlan other)) return false;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Utility.TaskPlanStatus getStatus() {
        return status;
    }

    public void setStatus(Utility.TaskPlanStatus status) {
        this.status = status;
    }

    public String getStatusIncompleteName() {
        if(getType() == Utility.TaskLinkType.BREEDER) {
            return "Missed";
        }
        return Utility.TaskPlanStatus.INCOMPLETE.getShortName();    
    }

}
