/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockWeightHistory;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockWeightHistoryService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author birch
 */
public class WeightEditor {
    public enum DialogMode{
        EDIT, CREATE
    }
    
    private DialogMode dialogMode = DialogMode.CREATE;
    
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(WeightEditor.class);
    private Dialog dialog = new Dialog();
    private StockWeightHistory weight;
    private Stock stockEntity;
    private String dialogTitle = "";

    private Button dialogOkButton = new Button("OK");
    private Button dialogCancelButton = new Button("Cancel");
    private Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    
    private WeightInput weightInput = new WeightInput();
    private TextArea notes = new TextArea();
    private DateTimePicker weightDateTime = new DateTimePicker();
    private Span ageAsOf = new Span();
    private VerticalLayout dialogLayout = new VerticalLayout();

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private StockWeightHistoryService weightService;

    public WeightEditor() {
        weightService = Registry.getBean(StockWeightHistoryService.class);
        dialogConfigure();
    }

    private void dialogConfigure(){
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");

        dialog.add(dialogLayout);
        dialogCloseButton.addClickListener((e) -> dialogClose());
        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.setCloseOnEsc(true);
        dialogCancelButton.addClickListener((e) -> dialogClose());
        dialogCancelButton.setAutofocus(true);
        dialogCancelButton.setEnabled(true);

        dialogOkButton.addClickListener(
                event -> {
                    dialogSave();
                }
        );
        //dialogOkButton.addClickShortcut(Key.ENTER);
        dialogOkButton.setEnabled(false);
        dialogOkButton.setDisableOnClick(true);
        dialogOkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(dialogCancelButton,dialogOkButton);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //one time configuration for any fields
        notes.setLabel("Optional notes:");
        notes.setWidthFull();
        weightInput.setLabel("Weight");
        weightInput.setWidthFull();
        weightInput.addValueChangeListener(event -> {
            System.out.println("dialogConfigure: weightInput value changed:" + event.getValue());
            if(event.getValue()>0) dialogOkButton.setEnabled(true);
            else dialogOkButton.setEnabled(false);
        });
        
        weightDateTime.setLabel("Optional date/time");
        weightDateTime.setWidthFull();
        weightDateTime.addValueChangeListener(event -> {
            updateAge();
        });
        
        //custom js code to force a default time of midnight if only the date is entered
        weightDateTime.getElement().executeJs("this.getElementsByTagName(\"vaadin-date-picker\")[0].addEventListener('change', function(){this.getElementsByTagName(\"vaadin-time-picker\")[0].value='00:00';}.bind(this));");
    }    
    
    public void dialogOpen(Stock stockEntity){
        StockWeightHistory newWeight = new StockWeightHistory();
        newWeight.setStockId(stockEntity.getId());
        dialogOpen(stockEntity, newWeight, DialogMode.CREATE);
    }
    
    public void dialogOpen(Stock stockEntity, StockWeightHistory weightEntity, DialogMode mode){
        this.stockEntity = stockEntity;
        this.weight = weightEntity;
        dialogMode = mode;
        
        dialogOkButton.setEnabled(false);
        if(dialogMode.equals(DialogMode.CREATE)){
            dialogTitle = "Create new weight";
        }else{ //edit
            dialogTitle = "Edit weight";
        }

        dialogLayout.removeAll();

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialog.setDraggable(true);
        dialog.setResizable(true);
        
        //add the header
        dialogLayout.add(stockEntity.getStockHeader(Boolean.FALSE));
        dialogLayout.add(new Hr());

        //add the needed fields
        VerticalLayout fieldsLayout = UIUtilities.getVerticalLayout(true, true, true);

        //set values here
        if(dialogMode.equals(DialogMode.CREATE)){
            notes.setValue("");
            weightInput.setValue(0);
            weightDateTime.setValue(null);
        }else{ //edit
            if(weightEntity.hasNote()){
                notes.setValue(weightEntity.getNote());
            }
            weightInput.setValue(weightEntity.getWeight());
            weightDateTime.setValue(weightEntity.getSortDate());
        }
        updateAge();
        
        fieldsLayout.add(weightInput, notes, weightDateTime, ageAsOf);
        dialogLayout.add(fieldsLayout);

        dialog.open();
    }
    
    private void updateAge(){
        System.out.println("updateAge: fieldVale:" + weightDateTime.getValue() + " stockAge:" + stockEntity.getAge().getAgeFormattedString());
        if(weightDateTime.getValue()==null){
            ageAsOf.setText("Age:" + stockEntity.getAge().getAgeFormattedString());
        }else{
            ageAsOf.setText("Age:" + weight.getAge(stockEntity, weightDateTime.getValue().toLocalDate()));
        }
    }

    private void dialogSave() {
        weight.setWeight(weightInput.getValue());
        if(!notes.isEmpty()) weight.setNote(notes.getValue());
        if(!weightDateTime.isEmpty()) weight.setCustomDate(weightDateTime.getValue());
        weightService.save(weight, stockEntity);
        notifyRefreshNeeded();
        dialogClose();
    }
    
    private void dialogClose(){
        dialog.close();
    }

    public void addListener(ListRefreshNeededListener listener){
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded(){
        for (ListRefreshNeededListener listener: listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }
    
}
