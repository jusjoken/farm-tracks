/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import ca.jusjoken.data.Utility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author birch
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class StockStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "stock_id", nullable = false)
    private Integer stockId;
    
    //statusName must be one from Utility list of valid status'
    @Column(name = "status_name", nullable = false)
    private String statusName;
    
    @Column(name = "original_status_date", nullable = true)
    private LocalDateTime originalStatusDate;

    @Column(name = "status_note", nullable = true)
    private String statusNote;
    
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
    public StockStatusHistory() {
    }

    public StockStatusHistory(Integer stockId, String statusName) {
        this.stockId = stockId;
        this.statusName = statusName;
    }

    public StockStatusHistory(Integer stockId, String statusName, LocalDateTime statusDate) {
        this.stockId = stockId;
        this.statusName = statusName;
        this.originalStatusDate = statusDate;
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

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public LocalDateTime getOriginalStatusDate() {
        return originalStatusDate;
    }

    public void setOriginalStatusDate(LocalDateTime originalStatusDate) {
        this.originalStatusDate = originalStatusDate;
    }

    public String getStatusNote() {
        return statusNote;
    }

    public void setStatusNote(String statusNote) {
        this.statusNote = statusNote;
    }
    
    public Boolean hasStatusNote(){
        if(statusNote==null || statusNote.isEmpty()) return Boolean.FALSE;
        else return Boolean.TRUE;
    }
    
    public LocalDateTime getSortDate(){
        if(getOriginalStatusDate()!=null) return getOriginalStatusDate();
        return getLastModifiedDate();
    }
    
    public String getFormattedStatus(){
        return getStatusName() + " - " + getSortDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm"));
    }

    @Override
    public String toString() {
        return "StockStatusHistory{" + "id=" + id + ", stockId=" + stockId + ", statusName=" + statusName + ", originalStatusDate=" + originalStatusDate + ", statusNote=" + statusNote + ", createdDate=" + createdDate + ", lastModifiedDate=" + lastModifiedDate + '}';
    }
    
    

}
