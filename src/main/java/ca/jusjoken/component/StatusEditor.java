/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.entity.StockWeightHistory;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockWeightHistoryService;

/**
 *
 * @author birch
 */
public class StatusEditor {
    public enum DialogMode{
        EDIT, CREATE
    }
    
    private DialogMode dialogMode = DialogMode.CREATE;
    
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(StatusEditor.class);
    private StockStatusHistory statusEntity;
    private final Dialog dialog = new Dialog();
    private Boolean isStatusValid = Boolean.FALSE;
    private Boolean isCustom = Boolean.FALSE;
    private StockStatus stockStatus;
    private Stock stockEntity;
    private String dialogTitle = "";

    private final Button dialogOkButton = new Button("OK");
    private final Button dialogCancelButton = new Button("Cancel");
    private final Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    
    private final TextArea notes = new TextArea();
    private final DateTimePicker statusDateTime = new DateTimePicker();
    private final Select<StockStatus> statusSelect = new Select<>();
    private final NativeLabel promptLabel = new NativeLabel();
    private final VerticalLayout dialogLayout = new VerticalLayout();
    private final Span ageAsOf = new Span();
    private final WeightInput preButcherWeight = new WeightInput();
    private final WeightInput butcheredWeight = new WeightInput();
    private final NumberField butcheredValue = UIUtilities.getNumberField("Optional value", Boolean.FALSE, "$");
    private Runnable afterSaveAction;

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private StockStatusHistoryService statusService;
    private StockWeightHistoryService weightService;
   
    public StatusEditor() {
        statusService = Registry.getBean(StockStatusHistoryService.class);
        weightService = Registry.getBean(StockWeightHistoryService.class);
        dialogConfigure();
    }

    private void dialogConfigure(){
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "270px").set("max-width", "100%");

        dialog.add(dialogLayout);
        dialogCloseButton.addClickListener((e) -> dialogClose());
        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.setCloseOnEsc(true);
        dialogCancelButton.addClickListener((e) -> dialogClose());
        dialogCancelButton.setEnabled(true);

        dialogOkButton.addClickListener(
                event -> {
                    dialogSave();
                }
        );
        //dialogOkButton.addClickShortcut(Key.ENTER);
        dialogOkButton.setEnabled(true);
        dialogOkButton.setDisableOnClick(true);
        dialogOkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(dialogOkButton, dialogCancelButton);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //one time configuration for any fields
        promptLabel.addClassName(LumoUtility.TextColor.ERROR);
        notes.setLabel("Optional notes:");
        notes.setWidthFull();
        notes.setAutoselect(true);
        statusDateTime.setLabel("Optional status date/time");
        statusDateTime.setWidthFull();
        statusDateTime.addValueChangeListener(event -> {
            updateAge();
        });
        //custom js code to force a default time of midnight if only the date is entered
        statusDateTime.getElement().executeJs("this.getElementsByTagName(\"vaadin-date-picker\")[0].addEventListener('change', function(){this.getElementsByTagName(\"vaadin-time-picker\")[0].value='00:00';}.bind(this));");

        statusSelect.setLabel("Status");
        statusSelect.setWidthFull();
        statusSelect.setItemLabelGenerator(StockStatus::getLongName);
        statusSelect.addValueChangeListener(event -> {
            if (dialogMode == DialogMode.CREATE) {
                applyStatusSelection(event.getValue(), true);
            }
        });
        
        butcheredWeight.setWidthFull();
        butcheredWeight.setLabel("Butchered");
        butcheredWeight.setHelperText("Optional");
        preButcherWeight.setWidthFull();
        preButcherWeight.setLabel("Pre-butcher");
        preButcherWeight.setHelperText("Optional");
        butcheredValue.setWidthFull();
        butcheredValue.setAutoselect(true);

        
    }    
    
    public void dialogOpen(Stock stockEntity, String stockStatusToEdit){
        dialogOpen(stockEntity, stockStatusToEdit, null);
    }

    public void dialogOpen(Stock stockEntity){
        dialogOpen(stockEntity, (Runnable) null);
    }

    public void dialogOpen(Stock stockEntity, Runnable afterSaveAction){
        StockStatusHistory newStatus = new StockStatusHistory();
        newStatus.setStockId(stockEntity.getId());
        dialogOpen(stockEntity, null, newStatus, DialogMode.CREATE, afterSaveAction);
    }

    public void dialogOpen(Stock stockEntity, String stockStatusToEdit, Runnable afterSaveAction){
        StockStatusHistory newStatus = new StockStatusHistory();
        newStatus.setStockId(stockEntity.getId());
        newStatus.setStatusName(stockStatusToEdit);
        dialogOpen(stockEntity, stockStatusToEdit, newStatus, DialogMode.CREATE, afterSaveAction);
        
    }
    public void dialogOpen(Stock stockEntity, String stockStatusToEdit, StockStatusHistory statusEntity, DialogMode mode){
        dialogOpen(stockEntity, stockStatusToEdit, statusEntity, mode, null);
    }

    public void dialogOpen(Stock stockEntity, String stockStatusToEdit, StockStatusHistory statusEntity, DialogMode mode, Runnable afterSaveAction){
        this.stockEntity = stockEntity;
        this.statusEntity = statusEntity;
        dialogMode = mode;
        this.afterSaveAction = afterSaveAction;

        StockStatus preselectedStatus = null;
        if (stockStatusToEdit != null && Utility.getInstance().hasStockStatus(stockStatusToEdit)) {
            preselectedStatus = Utility.getInstance().getStockStatus(stockStatusToEdit);
        }

        dialogOkButton.setEnabled(false);
        
        dialogLayout.removeAll();
        dialogTitle = dialogMode.equals(DialogMode.EDIT) ? "Edit Status" : "Change Status";
        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialog.setDraggable(true);
        dialog.setResizable(true);
        
        //add the header
        dialogLayout.add(stockEntity.getStockHeader(Boolean.FALSE));
        dialogLayout.add(new Hr());

        if(dialogMode.equals(DialogMode.CREATE)){
            notes.setValue("");
            statusDateTime.setValue(null);

            if (preselectedStatus != null) {
                applyStatusSelection(preselectedStatus, false);
            } else {
                var statuses = Utility.getInstance().getStockStatusList(false);
                statusSelect.setItems(statuses);
                dialogLayout.add(statusSelect);
                stockStatus = null;
                isStatusValid = Boolean.FALSE;
                isCustom = Boolean.FALSE;
                promptLabel.setText("Select a status to continue.");
                VerticalLayout statusHintLayout = UIUtilities.getVerticalLayout(false, true, false);
                statusHintLayout.add(promptLabel);
                dialogLayout.add(statusHintLayout);
            }
        }else{ //edit
            if (preselectedStatus == null) {
                dialogClose();
                return;
            }
            applyStatusSelection(preselectedStatus, false);
            if(statusEntity.hasNote()){
                notes.setValue(statusEntity.getNote());
            }
            statusDateTime.setValue(statusEntity.getSortDate());
            if(isCustom){
                butcheredValue.setValue(stockEntity.getStockValue());
                //weights are not displayed on edit so make sure they have no value
                preButcherWeight.setValue(null);
                butcheredWeight.setValue(null);
            }
        }
        updateAge();

        dialog.open();
        focusFirstEditableField();
        
    }

    private void applyStatusSelection(StockStatus selectedStatus, boolean resetCreateValues) {
        stockStatus = selectedStatus;
        isStatusValid = stockStatus != null;
        if (!isStatusValid) {
            dialogOkButton.setEnabled(false);
            return;
        }

        statusEntity.setStatusName(stockStatus.getName());
        isCustom = stockStatus.getPrompt() == null;
        if (isCustom) {
            promptLabel.setText("");
        } else {
            promptLabel.setText(String.format(stockStatus.getPrompt(), stockEntity.getDisplayName()));
        }

        if(dialogMode.equals(DialogMode.CREATE)){
            dialogTitle = stockStatus.getActionName();
        }else{
            dialogTitle = "Edit " + stockStatus.getActionName();
        }
        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);

        rebuildStatusFields();

        if (dialogMode.equals(DialogMode.CREATE) && resetCreateValues && isCustom) {
            preButcherWeight.setValue(stockEntity.getWeight());
            butcheredWeight.setValue(null);
            butcheredValue.setValue(stockEntity.getStockValue());
        }

        dialogOkButton.setEnabled(true);
        updateAge();
    }

    private void rebuildStatusFields() {
        VerticalLayout promptLayout = UIUtilities.getVerticalLayout(false, true, false);
        if (isCustom) {
            if(dialogMode.equals(DialogMode.CREATE)){
                promptLayout.add(preButcherWeight, butcheredWeight, butcheredValue);
            }else{
                promptLayout.add(butcheredValue);
            }
        } else {
            promptLayout.add(promptLabel);
        }
        promptLayout.add(notes, statusDateTime, ageAsOf);

        // Remove existing content below optional status picker and header.
        while (dialogLayout.getComponentCount() > (dialogMode.equals(DialogMode.CREATE) ? 3 : 2)) {
            dialogLayout.remove(dialogLayout.getComponentAt(dialogLayout.getComponentCount() - 1));
        }
        dialogLayout.add(promptLayout);
    }

    private void focusFirstEditableField() {
        if (dialogMode.equals(DialogMode.CREATE) && !isStatusValid) {
            statusSelect.focus();
            return;
        }
        if (isCustom && dialogMode.equals(DialogMode.CREATE)) {
            preButcherWeight.focus();
            preButcherWeight.selectAll();
            return;
        }
        if (isCustom && dialogMode.equals(DialogMode.EDIT)) {
            butcheredValue.focus();
            selectNumberFieldValue(butcheredValue);
            return;
        }
        notes.focus();
    }

    private void selectNumberFieldValue(NumberField field) {
        field.getElement().executeJs(
            "const f=this; requestAnimationFrame(() => { const i=f.inputElement; if (i) { i.select(); } });"
        );
    }

    private void updateAge(){
        System.out.println("updateAge: fieldVale:" + statusDateTime.getValue() + " stockAge:" + stockEntity.getAge().getAgeFormattedString());
        if(statusDateTime.getValue()==null){
            ageAsOf.setText("Age:" + stockEntity.getAge().getAgeFormattedString());
        }else{
            ageAsOf.setText("Age:" + statusEntity.getAge(stockEntity, statusDateTime.getValue().toLocalDate()));
        }
    }

    private void dialogSave() {
        if (!isStatusValid || stockStatus == null) {
            return;
        }
        if(!notes.isEmpty()) statusEntity.setNote(notes.getValue());
        if(!statusDateTime.isEmpty()) statusEntity.setCustomDate(statusDateTime.getValue());
        if(isCustom){
            if(!butcheredValue.isEmpty()) stockEntity.setStockValue(butcheredValue.getValue());
            //create a weight for each filled in optional weight
            if(!preButcherWeight.isEmpty() && preButcherWeight.getValue()>0){
                //do not ave this if it's the current value as no need having 2 records
                if(!preButcherWeight.getValue().equals(stockEntity.getWeight())){
                    StockWeightHistory newWeight = new StockWeightHistory(stockEntity.getId(), preButcherWeight.getValue());
                    if(!statusDateTime.isEmpty()) newWeight.setCustomDate(statusDateTime.getValue());
                    weightService.save(newWeight, stockEntity);
                }
            }
            if(!butcheredWeight.isEmpty() && butcheredWeight.getValue()>0){
                StockWeightHistory newWeight = new StockWeightHistory(stockEntity.getId(), butcheredWeight.getValue());
                if(!statusDateTime.isEmpty()) newWeight.setCustomDate(statusDateTime.getValue());
                weightService.save(newWeight, stockEntity);
            }
        }
        statusService.save(statusEntity, stockEntity, Boolean.FALSE);
        if (afterSaveAction != null) {
            afterSaveAction.run();
        }
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
