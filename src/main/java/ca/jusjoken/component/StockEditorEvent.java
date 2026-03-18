/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import com.vaadin.flow.component.ComponentEvent;

/**
 *
 * @author birch
 */
public class StockEditorEvent  extends ComponentEvent<StockEditor>{
    
    public StockEditorEvent(StockEditor source, boolean fromClient) {
        super(source, fromClient);
    }
    
}
