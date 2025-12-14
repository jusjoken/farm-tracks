/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/**
 *
 * @author birch
 */
public class ComponentConfirmEvent extends ComponentEvent<ConfirmDialog>  {
    
    public ComponentConfirmEvent(ConfirmDialog source, boolean fromClient) {
        super(source, fromClient);
    }
    
}
