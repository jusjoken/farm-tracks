/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.service.ParentIntegerToStringConverter;
import ca.jusjoken.data.service.StatusHistoryConverter;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.dom.ElementFactory;

/**
 *
 * @author birch
 */
public class StockDetailsFormLayout extends FormLayout{
    private Stock stockEntity;
    private Binder<Stock> binder;

    @PropertyId("fatherId")
    private TextField fieldFatherName = new TextField();
    @PropertyId("motherId")
    private TextField fieldMotherName = new TextField();

    @PropertyId("genotype")
    private TextField fieldGenotype = UIUtilities.getTextField();

    @PropertyId("legs")
    private TextField fieldLegs = UIUtilities.getTextField();
    @PropertyId("champNo")
    private TextField fieldChampNo = UIUtilities.getTextField();
    @PropertyId("regNo")
    private TextField fieldRegNo = UIUtilities.getTextField();

    @PropertyId("status")
    private TextField fieldStatus = new TextField();
    @PropertyId("category")
    private TextField fieldCategory = UIUtilities.getTextField(); //TODO - needs to be a pickbox

    @PropertyId("acquired")
    private DatePicker fieldAquiredDate = new DatePicker();
    @PropertyId("doB")
    private DatePicker fieldBornDate = new DatePicker();

    @PropertyId("fosterLitterName")
    private TextField fieldFosterName = new TextField();

    public StockDetailsFormLayout(Stock stock) {
        this.stockEntity = stock;
        binder = new Binder<Stock>(Stock.class);
        createForm();
    }
    
    public void createForm(){
        setWidthFull();
        setAutoResponsive(false);
        setExpandColumns(true);
        setExpandFields(true);
        setMaxColumns(3);
        setMinColumns(1);

        setResponsiveSteps(
        // Use one column by default
        new ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
        // Use two columns, if the layout's width exceeds 320px
        new ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        removeAll();

        fieldFatherName.setReadOnly(true);
        fieldMotherName.setReadOnly(true);
        fieldGenotype.setReadOnly(true);
        fieldLegs.setReadOnly(true);
        fieldChampNo.setReadOnly(true);
        fieldRegNo.setReadOnly(true);
        fieldStatus.setReadOnly(true);
        fieldCategory.setReadOnly(true);
        fieldAquiredDate.setReadOnly(true);
        fieldBornDate.setReadOnly(true);
        fieldFosterName.setReadOnly(true);
        
        addFormItem(fieldFatherName,"Father");
        addFormItem(fieldMotherName,"Mother");
        addFormItem(fieldGenotype,"Genotype");
        getElement().appendChild(ElementFactory.createBr()); // row break
        addFormItem(fieldLegs,"Legs");
        addFormItem(fieldChampNo,"Championship Number");
        addFormItem(fieldRegNo,"Registration Number");
        getElement().appendChild(ElementFactory.createBr()); // row break
        addFormItem(fieldStatus,"Status");
        addFormItem(fieldCategory,"Category");
        getElement().appendChild(ElementFactory.createBr()); // row break
        addFormItem(fieldAquiredDate,"Aquired");
        addFormItem(fieldBornDate,"Born");
        addFormItem(fieldFosterName,"Foster");
        
        //set values
        
        binder.forField(fieldFatherName)
                .withConverter(new ParentIntegerToStringConverter(stockEntity, Utility.Gender.MALE))
                .bindReadOnly(Stock::getFatherId);
        binder.forField(fieldMotherName)
                .withConverter(new ParentIntegerToStringConverter(stockEntity, Utility.Gender.FEMALE))
                .bindReadOnly(Stock::getMotherId);

        binder.forField(fieldStatus)
                .withConverter(new StatusHistoryConverter(this.stockEntity))
                .bindReadOnly(Stock::getStatus);
        
        binder.setBean(this.stockEntity);
        binder.bindInstanceFields(this);
        
    }

    
    
}
