/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.data.domain.Sort.Order;
import java.util.List;
import org.springframework.data.domain.Sort;


/**
 *
 * @author birch
 */
public class StockSort {
    private String name;
    private List<Order> sortOrderList = new ArrayList<>();

    public StockSort(String name) {
        this.name = name;
    }

    public StockSort(String name, Order order) {
        this.name = name;
        this.sortOrderList.add(order);
    }

    public StockSort(String name, Order... orders) {
        this.name = name;
        this.sortOrderList.addAll(Arrays.asList(orders));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.name);
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
        final StockSort other = (StockSort) obj;
        return Objects.equals(this.name, other.name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addOrder(Order order){
        this.sortOrderList.add(order);
    }

    public List<Order> getSortOrderList() {
        return sortOrderList;
    }
    
    public void setSortDirection(Sort.Direction direction){
        for( int i = 0; i < this.sortOrderList.size(); i++){
            Order item = this.sortOrderList.get(i);
            if(!item.getDirection().equals(direction)){
                this.sortOrderList.set(i, new Order(direction, item.getProperty()));
            }
        }
    }

    @Override
    public String toString() {
        return "StockSort{" + "name=" + name + ", sortOrderList=" + sortOrderList + '}';
    }
    
    
    
}
