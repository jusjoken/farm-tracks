/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.service;

import ca.jusjoken.data.entity.Litter;
import java.util.Comparator;

/**
 *
 * @author birch
 */
public class LitterComparator implements Comparator<Litter>{

    @Override
    public int compare(Litter t, Litter t1) {
        return t.getDoB().compareTo(t1.getDoB());
    }
    
}
