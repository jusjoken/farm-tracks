/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableSupplier;

/**
 *
 * @author birch
 */
public class LazyComponent extends Div {
    public LazyComponent(
            SerializableSupplier<? extends Component> supplier) {
        addAttachListener(e -> {
            if (getElement().getChildCount() == 0) {
                add(supplier.get());
            }
        });
    }
}