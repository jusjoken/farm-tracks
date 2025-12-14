/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.ColumnName;
import java.util.Comparator;

/**
 *
 * @author birch
 */
public class ColumnNameComparator implements Comparator<ColumnName>{

    @Override
    public int compare(ColumnName t, ColumnName t1) {
        return t.getDisplayName().compareTo(t1.getDisplayName());
    }
    
}
