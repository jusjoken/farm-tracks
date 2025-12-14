/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.Utility;
import java.util.Objects;

/**
 *
 * @author birch
 */
public class StockStatus {

private String name;
private Utility.ICONS icon;
private String longName;
private Integer sortOrder;
private Boolean stopsAgeCalculation;

    public StockStatus() {
    }

    public StockStatus(String name, Utility.ICONS icon, String longName, Boolean stopsAgeCalculation, Integer sortOrder) {
        this.name = name;
        this.icon = icon;
        this.longName = longName;
        this.stopsAgeCalculation = stopsAgeCalculation;
        this.sortOrder = sortOrder;
    }
    
    public StockStatus getDefault(){
        return new StockStatus("active", Utility.ICONS.STATUS_ACTIVE, "Active",Boolean.FALSE,1);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StockStatus other = (StockStatus) obj;
        return Objects.equals(this.name, other.name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Utility.ICONS getIcon() {
        return icon;
    }

    public void setIcon(Utility.ICONS icon) {
        this.icon = icon;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean stopsAgeCalculation(){
        if(this.stopsAgeCalculation==null) return Boolean.FALSE;
        return this.stopsAgeCalculation;
    }
    
    @Override
    public String toString() {
        return "StockStatus{" + "name=" + name + ", icon=" + icon + ", longName=" + longName + ", sortOrder=" + sortOrder + ", stopsAgeCalculation=" + stopsAgeCalculation + '}';
    }


    
}
