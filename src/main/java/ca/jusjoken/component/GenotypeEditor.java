package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.entity.GenotypeSegment;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;

public class GenotypeEditor {
    @SuppressWarnings("unused")
    private Logger log = LoggerFactory.getLogger(GenotypeEditor.class);
    private Dialog dialog = new Dialog();
    private StockService stockService;
    private List<GenotypeSegment> genotypeSegments;
    private Stock stockEntity;
    private String dialogTitle = "";
    private VerticalLayout fieldsLayout;
    private GenotypeSegmentSelect firstSegmentSelect;

    private Button dialogOkButton = new Button("OK");
    private Button dialogCancelButton = new Button("Cancel");
    private Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    
    private VerticalLayout dialogLayout = new VerticalLayout();
    private boolean persistOnSave = true;
    private Runnable afterSaveAction;

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    public GenotypeEditor() {
        this.stockService = Registry.getBean(StockService.class);
        dialogConfigure();
    }

    private void dialogConfigure(){
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setMargin(false);
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
        dialogOkButton.addClickShortcut(Key.ENTER);
        dialogOkButton.setEnabled(false);
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


    }    
    
    public void dialogOpen(Stock stockEntity){
        dialogOpen(stockEntity, true, null);
    }

    public void dialogOpen(Stock stockEntity, boolean persistOnSave, Runnable afterSaveAction){
        this.stockEntity = stockEntity;
        this.persistOnSave = persistOnSave;
        this.afterSaveAction = afterSaveAction;
        this.genotypeSegments = stockEntity.getGenoSegments();
        
        dialogOkButton.setEnabled(false);
        dialogTitle = "Edit Genotype";

        dialogLayout.removeAll();

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialog.setDraggable(true);
        dialog.setResizable(true);
        
        //add the header
        dialogLayout.add(stockEntity.getStockHeader(Boolean.FALSE));
        dialogLayout.add(new Hr());

        fieldsLayout = UIUtilities.getVerticalLayout(false, true, false);
        fieldsLayout.removeAll();
        firstSegmentSelect = null;

        //add the needed fields

        for(GenotypeSegment segment: genotypeSegments){
            GenotypeSegmentSelect genotypeSelect = new GenotypeSegmentSelect();
            genotypeSelect.setItems(segment.getValues());
            genotypeSelect.setValue(segment);
            genotypeSelect.getSegment1().addValueChangeListener(event -> {
                //System.out.println("GenotypeEditor: valueChanged seg1:" + event.getValue());
                valuesChanged();
            });
            genotypeSelect.getSegment2().addValueChangeListener(event -> {
                //System.out.println("GenotypeEditor: valueChanged seg2:" + event.getValue());
                valuesChanged();
            });
            if (firstSegmentSelect == null) {
                firstSegmentSelect = genotypeSelect;
            }
            fieldsLayout.add(genotypeSelect);
        }

        //set values here
        
        fieldsLayout.add();
        dialogLayout.add(fieldsLayout);

        dialog.open();
        focusFirstEditableField();
    }

    private void focusFirstEditableField() {
        if (firstSegmentSelect != null) {
            firstSegmentSelect.getSegment1().focus();
        }
    }

    private void valuesChanged(){
        dialogOkButton.setEnabled(true);
    }
    
    private void dialogSave() {
        List<String> allGenoList = new ArrayList<>();
        //iterate over all segment components to get their values to build a new genotype csv string
        Iterator<Component> iterator = fieldsLayout.getChildren().iterator();
        while (iterator.hasNext()) {
            Component child = iterator.next();
            if(child instanceof GenotypeSegmentSelect){
                //System.out.println("Child:" + ((GenotypeSegmentSelect)child).getValue());
                GenotypeSegment segment = ((GenotypeSegmentSelect)child).getValue();
                allGenoList.add(segment.getValue1());
                allGenoList.add(segment.getValue2());
            }
            // Perform actions on each child
        }
        String newGenotype = String.join(",", allGenoList);
        //System.out.println("dialogSave: saving newGenotype:" + newGenotype);
        stockEntity.setGenotype(newGenotype);
        if (persistOnSave) {
            stockService.save(stockEntity);
            notifyRefreshNeeded();
        }
        if (afterSaveAction != null) {
            afterSaveAction.run();
        }
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
