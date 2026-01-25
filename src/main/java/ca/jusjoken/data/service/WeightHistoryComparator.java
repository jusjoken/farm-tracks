/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.StockWeightHistory;
import java.util.Comparator;

/**
 *
 * @author birch
 */
public class WeightHistoryComparator  implements Comparator<StockWeightHistory>{

    @Override
    public int compare(StockWeightHistory t, StockWeightHistory t1) {
        return t.getSortDate().compareTo(t1.getSortDate());
    }
    
}
