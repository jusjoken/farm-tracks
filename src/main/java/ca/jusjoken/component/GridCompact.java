/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

/**
 *
 * @author birch
 */
public class GridCompact<T> extends Grid<T> {

    public GridCompact() {
        addThemeVariants(GridVariant.LUMO_COMPACT);
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        getStyle().set("--vaadin-grid-cell-padding", "var(--lumo-space-xs)");
    }
    
    
}
