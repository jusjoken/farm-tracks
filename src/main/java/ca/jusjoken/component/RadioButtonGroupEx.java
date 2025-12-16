/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import ca.jusjoken.theme.RadioButtonTheme;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.dataview.RadioButtonGroupListDataView;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 *
 * @author birch
 * @param <T>
 */
public class RadioButtonGroupEx<T> extends RadioButtonGroup {

    public RadioButtonGroupEx() {
        this.addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Padding.XSMALL, LumoUtility.Padding.Top.MEDIUM);
        this.addThemeNames(RadioButtonTheme.EQUAL_WIDTH, RadioButtonTheme.PRIMARY, RadioButtonTheme.TOGGLE);
    }

    @Override
    public RadioButtonGroupListDataView setItems(ListDataProvider dataProvider) {
        RadioButtonGroupListDataView items = super.setItems(dataProvider);
        this.getChildren().forEach(component -> {
            component.getElement().getThemeList().add(RadioButtonTheme.PRIMARY);
            component.getElement().getThemeList().add(RadioButtonTheme.TOGGLE);
        });
        return items; 
    }

    @Override
    public void setLabel(String label) {
        this.setAriaLabel(label);
        super.setLabel(label);
    }

}
