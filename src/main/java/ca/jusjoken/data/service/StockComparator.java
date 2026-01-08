/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Stock;
import java.util.Comparator;

/**
 *
 * @author birch
 */
public class StockComparator implements Comparator<Stock>{

    @Override
    public int compare(Stock t, Stock t1) {
        return t.getDisplayName().compareTo(t1.getDisplayName());
    }
    
}
