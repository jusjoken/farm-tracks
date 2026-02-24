package ca.jusjoken.data.entity;

import ca.jusjoken.data.Utility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class PlanTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;
    private String name;
    @ManyToOne
    private StockType stockType;  //rabbit, goat, pig, cow etc.
    private Utility.TaskLinkType type;

    public PlanTemplate() {
    }

    public PlanTemplate(String name, StockType stockType, Utility.TaskLinkType type) {
        this.name = name;
        this.stockType = stockType;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public Utility.TaskLinkType getType() {
        return type;
    }

    public void setType(Utility.TaskLinkType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PlanTemplate [id=" + id + ", name=" + name + ", stockType=" + stockType + ", type=" + type + "]";
    }

    
}
