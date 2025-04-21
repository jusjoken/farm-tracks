/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.Comparator;

/**
 *
 * @author birch
 */
public class StockStatusComparator implements Comparator<StockStatus> {
    @Override
    public int compare(StockStatus a, StockStatus b) {
        return a.getSortOrder() - b.getSortOrder();
    }
}