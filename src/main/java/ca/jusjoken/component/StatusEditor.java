/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author birch
 */
public class StatusEditor {
    private Logger log = LoggerFactory.getLogger(StatusEditor.class);
    private Collection<StockStatus> statusTypes = Utility.getInstance().getStockStatusList(Boolean.FALSE);
    private String stockStatusToEdit;
    private Dialog dialog = new Dialog();
    private Boolean isStatusValid = Boolean.FALSE;
    private Boolean isCustom = Boolean.FALSE;
    private StockStatus stockStatus;
    private Stock stockEntity;
    private String dialogTitle = "";

    private Button dialogOkButton = new Button("OK");
    private Button dialogCancelButton = new Button("Cancel");
    private Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    
    private TextArea notes = new TextArea();
    private NativeLabel promptLabel = new NativeLabel();
    private VerticalLayout dialogLayout = new VerticalLayout();

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private StockStatusHistoryService statusService;
   
    public StatusEditor() {
        statusService = Registry.getBean(StockStatusHistoryService.class);
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
        dialogOkButton.setEnabled(true);
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
        promptLabel.addClassName(LumoUtility.TextColor.ERROR);
        notes.setLabel("Optional notes:");
        notes.setWidthFull();
        
        
    }    
    
    public void dialogOpen(Stock stockEntity, String stockStatusToEdit){
        this.stockEntity = stockEntity;
        this.stockStatusToEdit = stockStatusToEdit;
        //check if the passed status is a valid one for edit
        isStatusValid = Utility.getInstance().hasStockStatus(stockStatusToEdit);
        if(isStatusValid) stockStatus = Utility.getInstance().getStockStatus(stockStatusToEdit);
        if(stockStatus.getPrompt()==null){
            isCustom = Boolean.TRUE;
        } 
        else {
            isCustom = Boolean.FALSE;
            promptLabel.setText(String.format(stockStatus.getPrompt(),stockEntity.getDisplayName()));
        }
        
        System.out.println("dialogOpen: name:" + stockEntity.getDisplayName() + " isCustom:" + isCustom + " status:" + stockStatusToEdit + " prompt:" + promptLabel.getText());
        dialogTitle = stockStatus.getActionName();

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
        if(isCustom){ //add custom fields based on specific status
            dialogLayout.add(new Span("!! NOT YET IMPLEMENTED !!"));
            //likely need to use showItem here for more complex forms
            //dialogLayout.add(showItem());
        }else{ //general form using prompt
            VerticalLayout promptLayout = UIUtilities.getVerticalLayout(true, true, true);
            promptLayout.add(promptLabel, notes);
            dialogLayout.add(promptLayout);
        }

        dialog.open();
        
    }

    public FormLayout showItem(){
        FormLayout stockFormLayout = new FormLayout();
        stockFormLayout.setWidthFull();
        stockFormLayout.setAutoResponsive(false);
        stockFormLayout.setExpandColumns(true);
        stockFormLayout.setExpandFields(true);
        stockFormLayout.setMaxColumns(3);
        stockFormLayout.setMinColumns(1);

        stockFormLayout.setResponsiveSteps(
        // Use one column by default
        new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
        // Use two columns, if the layout's width exceeds 320px
        new FormLayout.ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new FormLayout.ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        stockFormLayout.removeAll();

        //add all the appropriate fields and set their values
        //TODO:: for complex forms

        return stockFormLayout;
    }
    
    private void dialogSave() {
        StockStatusHistory newStatus = new StockStatusHistory(stockEntity.getId(), stockStatusToEdit);
        if(!notes.isEmpty()) newStatus.setStatusNote(notes.getValue());
        statusService.save(newStatus, stockEntity, Boolean.FALSE);
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
