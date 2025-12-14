/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data;

/**
 *
 * @author birch
 */
public class ColumnName {
    private String displayName;
    private String columnName;

    public ColumnName(String displayName, String columnName) {
        this.displayName = displayName;
        this.columnName = columnName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String toString() {
        return "ColumnName{" + "displayName=" + displayName + ", columnName=" + columnName + '}';
    }
    
    
}
