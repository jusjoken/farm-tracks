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
                + linkMotherId + "]";
    }

}
