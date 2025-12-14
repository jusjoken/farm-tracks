/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;
import org.springframework.data.domain.Sort;

/**
 *
 * @author birch
 */
public class ColumnSort {

private String columnName = null;
private Sort.Direction columnSortDirection = Sort.Direction.ASC;

    public ColumnSort() {
    }

    public ColumnSort(String columnName, Sort.Direction columnSortDirection) {
        this.columnName = columnName;
        this.columnSortDirection = columnSortDirection;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Sort.Direction getColumnSortDirection() {
        return columnSortDirection;
    }

    public void setColumnSortDirection(Sort.Direction columnSortDirection) {
        this.columnSortDirection = columnSortDirection;
    }
    
    public Sort.Order getSortOrder(){
        if(columnName==null) return null;
        return new Sort.Order(columnSortDirection, columnName);
    }
    
    @Override
    public String toString() {
        return "ColumnSort{" + "columnName=" + columnName + ", columnSortDirection=" + columnSortDirection + '}';
    }

    
}
