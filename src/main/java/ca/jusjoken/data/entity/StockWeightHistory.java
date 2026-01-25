/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import ca.jusjoken.data.Utility;
import ca.jusjoken.utility.AgeBetween;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author birch
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class StockWeightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "stock_id", nullable = false)
    private Integer stockId;
    
    @Column(name = "weight", nullable = false)
    private Integer weight;
    
    @Column(name = "custom_date", nullable = true)
    private LocalDateTime customDate;

    @Column(name = "note", nullable = true)
    private String note;
    
    //START Common Audit fields for any entity
    //need to add the below for any entity using these audit fields
    //@EntityListeners(AuditingEntityListener.class)
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        //if NULL date passed then use the default 1970 date to identify original imported records
        if(createdDate==null) this.createdDate = Utility.nullDate;
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    //END Common time stamps for records

    public StockWeightHistory() {
    }

    public StockWeightHistory(Integer stockId, Integer weight) {
        this.stockId = stockId;
        this.weight = weight;
    }

    public StockWeightHistory(Integer stockId, Integer weight, LocalDateTime customDate) {
        this.stockId = stockId;
        this.weight = weight;
        this.customDate = customDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStockId() {
        return stockId;
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public LocalDateTime getCustomDate() {
        return customDate;
    }

    public void setCustomDate(LocalDateTime customDate) {
        this.customDate = customDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean hasNote(){
        if(note==null || note.isEmpty()) return Boolean.FALSE;
        else return Boolean.TRUE;
    }

    public LocalDateTime getSortDate(){
        if(getCustomDate()!=null) return getCustomDate();
        if(getLastModifiedDate()==null) return LocalDateTime.now();
        return getLastModifiedDate();
    }

    public String getWeightInLbsOz() {
        if (getWeight() > 0) {
            return Utility.getInstance().WeightConverterOzToString(getWeight());
        }
        return Utility.emptyValue;
    }
    
    public String getAge(Stock stock){
        return getAge(stock, null);
    }
    public String getAge(Stock stock, LocalDate toDate){
        LocalDate ageToDate = getSortDate().toLocalDate();
        if(toDate==null) ageToDate = LocalDate.now();
        AgeBetween age = new AgeBetween(stock.getDoB(), toDate);
        return age.getAgeFormattedString();
    }

    @Override
    public String toString() {
        return "StockWeightHistory{" + "id=" + id + ", stockId=" + stockId + ", weight=" + weight + ", customDate=" + customDate + ", note=" + note + ", createdDate=" + createdDate + ", lastModifiedDate=" + lastModifiedDate + '}';
    }

    
}
