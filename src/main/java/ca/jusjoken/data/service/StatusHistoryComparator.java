/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockStatusHistory;
import java.util.Comparator;

/**
 *
 * @author birch
 */
public class StatusHistoryComparator implements Comparator<StockStatusHistory>{

    @Override
    public int compare(StockStatusHistory t, StockStatusHistory t1) {
        return t.getSortDate().compareTo(t1.getSortDate());
    }
    
}
