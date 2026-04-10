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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import ca.jusjoken.data.Utility.Gender;

/**
 *
 * @author birch
 */
public class StockDetailsFormLayout extends FormLayout{
    private Stock stockEntity;
    private Binder<Stock> binder;

    @PropertyId("external")
    private Checkbox fieldExternal = new Checkbox();
    @PropertyId("breeder")
    private Checkbox fieldBreeder = new Checkbox();
    @PropertyId("sex")
    private RadioButtonGroup<Gender> fieldGender = new RadioButtonGroup<>();
    @PropertyId("prefix")
    private TextField fieldPrefix = new TextField();
    @PropertyId("name")
    private TextField fieldName = new TextField();
    @PropertyId("tattoo")
    private TextField fieldTattoo = new TextField();
    @PropertyId("color")
    private TextField fieldColor = new TextField();
    @PropertyId("breed")
    private TextField fieldBreed = new TextField();
    @PropertyId("weight")
    private TextField fieldWeight = new TextField();

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
    
    @PropertyId("stockValue")
    private NumberField fieldValue = UIUtilities.getNumberField(); //TODO - needs to be a pickbox
    
    @PropertyId("acquired")
    private DatePicker fieldAquiredDate = new DatePicker();
    @PropertyId("doB")
    private DatePicker fieldBornDate = new DatePicker();

    @PropertyId("fosterLitterName")
    private TextField fieldFosterName = new TextField();

    @PropertyId("invoiceNumber")
    private TextField fieldInvoiceNumber = UIUtilities.getTextField();

    private TextField fieldSaleStatus = new TextField();

    public StockDetailsFormLayout(Stock stock) {
        // Keep natural height so parent Scroller can detect overflow and scroll.
        setHeight(null);
        setWidthFull();
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
        new ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use two columns, if the layout's width exceeds 320px
        new ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        removeAll();

        // Set all fields to readonly
        fieldExternal.setReadOnly(true);
        fieldBreeder.setReadOnly(true);
        fieldGender.setReadOnly(true);
        fieldPrefix.setReadOnly(true);
        fieldName.setReadOnly(true);
        fieldTattoo.setReadOnly(true);
        fieldColor.setReadOnly(true);
        fieldBreed.setReadOnly(true);
        fieldWeight.setReadOnly(true);
        fieldFatherName.setReadOnly(true);
        fieldMotherName.setReadOnly(true);
        fieldGenotype.setReadOnly(true);
        fieldLegs.setReadOnly(true);
        fieldChampNo.setReadOnly(true);
        fieldRegNo.setReadOnly(true);
        fieldStatus.setReadOnly(true);
        fieldValue.setReadOnly(true);
        fieldAquiredDate.setReadOnly(true);
        fieldBornDate.setReadOnly(true);
        fieldFosterName.setReadOnly(true);
        fieldInvoiceNumber.setReadOnly(true);
        fieldSaleStatus.setReadOnly(true);

        setFieldWidths();
        
        // Add fields in same order as StockEditor
        addFormItem(fieldExternal,"External");
        addFormItem(fieldPrefix,"Prefix");
        addFormItem(fieldName,"Name");
        addFormItem(fieldTattoo,"Tattoo/ID");
        addFormItem(fieldBreed,"Breed");
        addFormItem(fieldGender,"Gender");
        addFormItem(fieldBreeder,"Breeder");
        addFormItem(fieldColor,"Color");
        addFormItem(fieldWeight,"Weight");
        addFormItem(fieldFatherName,"Father");
        addFormItem(fieldMotherName,"Mother");
        addFormItem(fieldGenotype,"Genotype");
        addFormItem(fieldLegs,"Legs");
        addFormItem(fieldChampNo,"Championship Number");
        addFormItem(fieldRegNo,"Registration Number");
        addFormItem(fieldStatus,"Status");
        addFormItem(fieldAquiredDate,"Acquired");
        addFormItem(fieldBornDate,"Born");
        addFormItem(fieldFosterName,"Foster");
        addFormItem(fieldSaleStatus, "Sale Status");
        addFormItem(fieldValue, "Value");
        addFormItem(fieldInvoiceNumber, "Invoice #");
        
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

        fieldGender.setItems(Gender.MALE, Gender.FEMALE, Gender.NA);
        fieldGender.setRenderer(new TextRenderer<>(gender -> {
            if(gender.equals(Gender.MALE)) return stockEntity.getStockType().getMaleName();
            else if(gender.equals(Gender.FEMALE)) return stockEntity.getStockType().getFemaleName();
            return "NA";
        }));

        // Bind weight as string since fieldWeight is now a TextField
        binder.forField(fieldWeight)
                .bindReadOnly(stock -> stock.getWeightInLbsOzAsString());

        // Bind sale status
        binder.forField(fieldSaleStatus)
                .bindReadOnly(stock -> {
                    if (stock.getSaleStatus() == null || stock.getSaleStatus().toString().equals("NONE")) {
                        return "Not for sale";
                    }
                    return stock.getSaleStatus().getShortName();
                });
        
        binder.setBean(this.stockEntity);
        binder.bindInstanceFields(this);
        
    }

    private void setFieldWidths() {
        fieldExternal.setWidthFull();
        fieldBreeder.setWidthFull();
        fieldGender.setWidthFull();
        fieldPrefix.setWidthFull();
        fieldName.setWidthFull();
        fieldTattoo.setWidthFull();
        fieldColor.setWidthFull();
        fieldBreed.setWidthFull();
        fieldWeight.setWidthFull();
        fieldFatherName.setWidthFull();
        fieldMotherName.setWidthFull();
        fieldGenotype.setWidthFull();
        fieldLegs.setWidthFull();
        fieldChampNo.setWidthFull();
        fieldRegNo.setWidthFull();
        fieldStatus.setWidthFull();
        fieldValue.setWidthFull();
        fieldAquiredDate.setWidthFull();
        fieldBornDate.setWidthFull();
        fieldFosterName.setWidthFull();
        fieldInvoiceNumber.setWidthFull();
        fieldSaleStatus.setWidthFull();
    }
}
