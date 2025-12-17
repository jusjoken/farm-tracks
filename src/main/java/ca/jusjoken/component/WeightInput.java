/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import ca.jusjoken.data.Utility;
import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;

/**
 *
 * @author birch
 */
public class WeightInput extends AbstractCompositeField<HorizontalLayout,WeightInput, Integer> {
    private NumberField pounds = new NumberField();
    private NumberField ounces = new NumberField();
    private Integer totalOunces = 0;

    public WeightInput() {
        super(null);
        
        getContent().add(pounds, ounces);
        // Optionally, add validation or formatting
        getContent().setPadding(false);
        getContent().setSpacing(true);
        getContent().setMargin(false);
        getContent().setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        pounds.setWidth("45%");
        ounces.setWidth("45%");
        pounds.addValueChangeListener(listener -> {
            calculateTotalOunces();
            setModelValue(totalOunces, false);
        });
        ounces.addValueChangeListener(listener -> {
            calculateTotalOunces();
            setModelValue(totalOunces, false);
        });
    }

    public int getTotalOunces() {
        return totalOunces;
    }
    
    private void setTotalOunces(Integer tOunces){
        totalOunces = tOunces;
        //set each of the fields values
        //System.out.println("setTotalOunces: totalOunces:" + tOunces);
        pounds.setValue(Utility.getInstance().WeightConverterOzToPounds(tOunces).doubleValue());
        pounds.setSuffixComponent(new Div("lbs"));
        //System.out.println("setTotalOunces: pounds:" + pounds.getValue());
        ounces.setValue(Utility.getInstance().WeightConverterOzToRemainingOunces(tOunces).doubleValue());
        ounces.setSuffixComponent(new Div("oz"));
        //System.out.println("setTotalOunces: ounces:" + ounces.getValue());
    }

    private void calculateTotalOunces() {
        Integer pValue = 0;
        Integer oValue = 0;
        if(!pounds.isEmpty()) pValue = pounds.getValue().intValue();
        if(!ounces.isEmpty()) oValue = ounces.getValue().intValue();
        totalOunces = Utility.getInstance().WeightConverterPoundsOuncesToOz(pValue, oValue);
        //System.out.println("calculateTotalOunces: totalOunces:" + totalOunces);
    }

    @Override
    protected void setPresentationValue(Integer tOunces) {
        if(tOunces==null){
            setTotalOunces(0);
        }else{
            setTotalOunces(tOunces);
        }
    }
    
    @Override
    public void setReadOnly(boolean readOnly) {
        pounds.setReadOnly(readOnly);
        ounces.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return pounds.isReadOnly();
    }  
    
    public void setWidthFull(){
        getContent().setWidthFull();
    }
    
}   