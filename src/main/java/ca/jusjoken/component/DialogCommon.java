/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

/**
 *
 * @author birch
 */
import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DialogCommon {

    public enum DialogMode{
        EDIT, DELETE
    }

    //default is EDIT - must be superUser to allow DELETE
    private DialogMode dialogMode = DialogMode.EDIT;

    public enum DisplayMode{
        LITTER_LIST, KIT_LIST, STOCK_DETAILS
    }
    private DisplayMode displayMode = DisplayMode.STOCK_DETAILS;

    private Boolean superUser = Boolean.FALSE;
    private Boolean validationEnabled = Boolean.FALSE;
    private Logger log = LoggerFactory.getLogger(DialogCommon.class);
    private Dialog dialog = new Dialog();
    private Dialog dialogAdv = new Dialog();
    private Long taskID = 0L;
    private Stock stockEntity;
    private String dialogTitle = "";
    
    private List<Stock> stockList = new ArrayList<>();
    private List<Litter> litterList = new ArrayList<>();
    
    private Boolean customTaskConverted = Boolean.FALSE;
    private Button dialogResetButton = new Button("Reset");
    private Button dialogOkButton = new Button("OK");
    private Button dialogCancelButton = new Button("Cancel");
    private Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    private Button dialogConvertCustom = new Button(new Icon("vaadin","exchange"));

    private Button dialogAdvOkButton = new Button("OK");
    private Button dialogAdvCancelButton = new Button("Cancel");
    private Button dialogAdvCloseButton = new Button(new Icon("lumo", "cross"));


    //Fields defined here
    //global fields
    private NumberField fieldGlobalSubTotal = UIUtilities.getNumberField(Boolean.FALSE);
    private ButtonNumberField fieldGlobalTaxes = UIUtilities.getButtonNumberField("",Boolean.FALSE,"$");

    private Checkbox fieldWebOrder = new Checkbox();
    private Checkbox fieldFeesOnly = new Checkbox();

    private NumberField fieldPaidToVendor = UIUtilities.getNumberField(Boolean.FALSE);
    private NumberField fieldReceiptTotal = UIUtilities.getNumberField(Boolean.FALSE);
    private Select<String> fieldPaymentMethod = new Select<>();
    private NumberField fieldDeliveryFee = UIUtilities.getNumberField(Boolean.FALSE);
    private NumberField fieldServiceFeePercent = UIUtilities.getNumberField("",Boolean.FALSE,"%");
    private NumberField fieldServiceFee = UIUtilities.getNumberField(Boolean.FALSE);
    private ButtonNumberField fieldTotalSale = UIUtilities.getButtonNumberField("",Boolean.FALSE,"$");
    private NumberField fieldTip = UIUtilities.getNumberField(Boolean.FALSE);
    private NumberField fieldTotalWithTip = UIUtilities.getNumberField("",true,"$");
    private Checkbox fieldTipIssue = new Checkbox();
    private TextArea fieldNotes = new TextArea();

    private VerticalLayout dialogLayout = new VerticalLayout();
    private VerticalLayout dialogAdvLayout = new VerticalLayout();

    //adv dialog fields
    private enum AdvDialogMode {
        Global, Form
    }
    private AdvDialogMode advDialogMode = AdvDialogMode.Global;
    private Select<AdvDialogMode> advFieldConvertType = new Select<>();

    //private ComboBox<Restaurant> advFieldRestaurant = new ComboBox<>("Restaurant");
    private TextField advFieldOrderId = UIUtilities.getTextField("Order Id");

    private Boolean hasChangedValues = Boolean.FALSE;
    private StockService stockService;
    private LitterService litterService;

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    public DialogCommon() {
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
        dialogConfigure();
        dialogAdvConfigure();
    }


    private void dialogConfigure() {

        //configure the dialog internal layout for the form
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");

        dialog.add(dialogLayout);
        dialogCloseButton.addClickListener((e) -> dialogClose());
        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.setCloseOnEsc(true);
        dialogCancelButton.addClickListener((e) -> dialogClose());

        dialogOkButton.addClickListener(
                event -> {
                    dialogSave();
                }
        );
        dialogOkButton.addClickShortcut(Key.ENTER);
        dialogOkButton.setEnabled(false);
        dialogOkButton.setDisableOnClick(true);

        dialogResetButton.addClickListener(
                event -> {
                    dialogOkButton.setEnabled(false);
                    setValues();
                    dialogValidate();
                }
        );

        dialogConvertCustom.setVisible(false);
        dialogConvertCustom.addClickListener(
                event -> {
                    dialogAdvOpen();
                    dialogOkButton.setEnabled(true);
                }
        );

        HorizontalLayout footerLayout = new HorizontalLayout(dialogOkButton,dialogCancelButton,dialogResetButton, dialogConvertCustom);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //one time configuration for any fields
        //fieldPaymentMethod.setItems(Config.getInstance().getPaymentMethods());
        fieldPaymentMethod.setReadOnly(false);
        fieldPaymentMethod.setEmptySelectionAllowed(false);
        fieldPaymentMethod.setRequiredIndicatorVisible(true);
        fieldPaymentMethod.setWidth("150px");
        fieldPaymentMethod.setPlaceholder("Select payment method");
        fieldNotes.setReadOnly(true);
        fieldNotes.addThemeVariants(TextAreaVariant.LUMO_SMALL);

        fieldGlobalTaxes.setButtonIcon(new Icon("vaadin", "cogs"));
        fieldTotalSale.setButtonIcon(new Icon("vaadin","calc"));

        fieldPaidToVendor.addValueChangeListener(item -> dialogValidate());
        fieldReceiptTotal.addValueChangeListener(item -> dialogValidate());
        fieldGlobalSubTotal.addValueChangeListener(item -> dialogValidate());
        fieldGlobalTaxes.addValueChangeListener(item -> dialogValidate());
        fieldGlobalTaxes.addClickListener(
                event -> {
                    dialogValidate();
                }
        );
        fieldWebOrder.addValueChangeListener(item -> dialogValidate());
        fieldFeesOnly.addValueChangeListener(item -> dialogValidate());
        fieldPaymentMethod.addValueChangeListener(item -> dialogValidate());
        fieldDeliveryFee.addValueChangeListener(item -> dialogValidate());
        fieldServiceFeePercent.addValueChangeListener(item -> dialogValidate());
        fieldServiceFee.addValueChangeListener(item -> dialogValidate());
        fieldTotalSale.addValueChangeListener(item -> dialogValidate());
        fieldTotalSale.addClickListener(
                event -> {
                    dialogValidate();
                }
        );
        fieldTip.addValueChangeListener(item -> dialogValidate());
        fieldTipIssue.addValueChangeListener(item -> dialogValidate());

    }

    private void dialogClose(){
        if(customTaskConverted){
            log.info("dialogClose: reloading stockEntity");
            List<Stock> stockEntityList = stockService.findById(this.stockEntity.getId());
            log.info("dialogClose: reloading stockEntity:" + stockEntityList);
            if(stockEntityList!=null && stockEntityList.size()>0){
                log.info("dialogClose: reseting to stockEntity:" + stockEntityList.get(0));
                this.stockEntity = stockEntityList.get(0);
            }
        }
        dialog.close();
    }

    private void dialogSave() {
        //save here
        //update stockEntity from fields
        /*
        if(displayMode.equals(DisplayMode.GLOBAL)){
            this.stockEntity.setGlobalSubtotal(fieldGlobalSubTotal.getValue());
            this.stockEntity.setGlobalTotalTaxes(fieldGlobalTaxes.getNumberField().getValue());
            if(posGlobal){
                this.stockEntity.setPaidToVendor(fieldPaidToVendor.getValue());
            }
        }else if(displayMode.equals(DisplayMode.CUSTOM)){
            this.stockEntity.setFeesOnly(fieldFeesOnly.getValue());
            this.stockEntity.setReceiptTotal(fieldReceiptTotal.getValue());
        }else{
            this.stockEntity.setWebOrder(fieldWebOrder.getValue());
            this.stockEntity.setReceiptTotal(fieldReceiptTotal.getValue());
        }
        this.stockEntity.setPaymentMethod(fieldPaymentMethod.getValue());
        this.stockEntity.setDeliveryFee(fieldDeliveryFee.getValue());
        this.stockEntity.setServiceFeePercent(fieldServiceFeePercent.getValue());
        this.stockEntity.setServiceFee(fieldServiceFee.getValue());
        this.stockEntity.setTotalSale(fieldTotalSale.getNumberField().getValue());
        this.stockEntity.setTip(fieldTip.getValue());
        this.stockEntity.setTipInNotesIssue(fieldTipIssue.getValue());

        taskDetailRepository.save(this.stockEntity);
        //refresh if needed
        */
        log.info("dialogSave: notifying listeners");
        dialog.close();
        notifyRefreshNeeded();
    }

    public void dialogOpen(Long stockID){
        List<Stock> stockEntityList = stockService.findById(stockID);
        if(stockEntityList!=null && stockEntityList.size()>0){
            dialogOpen(stockEntityList.get(0));
        }else{
            log.info("TaskEditDialog: dialogOpen: failed to find task with jobId:" + stockID);
        }
    }
    public void dialogOpen(Stock stockEntity){
        this.stockEntity = stockEntity;
        //set values and visibility for fields
        clearLists();
        dialogLayout.removeAll();
        if(displayMode.equals(DisplayMode.LITTER_LIST)){
            litterList = litterService.getLitters(this.stockEntity);
            //show list of litter items
            VirtualList<Litter> list = new VirtualList<>();
            list.setItems(litterList);
            list.setRenderer(litterCardRenderer);
            list.setWidthFull();
            dialogLayout.add(list);
        }else if(displayMode.equals(DisplayMode.KIT_LIST)){
            stockList = stockService.getKitsForParent(this.stockEntity);
            //show list of kit items
            VirtualList<Stock> list = new VirtualList<>();
            list.setItems(stockList);
            list.setRenderer(kitCardRenderer);
            list.setWidthFull();
            dialogLayout.add(getStockHeader(stockEntity, false), list);
            
        }else{
            //viewer for stock fields - READONLY
            dialogLayout.add(showItem(this.stockEntity));
        }
        
        

        customTaskConverted = Boolean.FALSE;

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialogValidate();
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        //dialog.addClassNames("backdrop-blur-none");

        dialog.open();


    }
    
    private void clearLists(){
        stockList.clear();
        litterList.clear();
    }
    
    private ComponentRenderer<Component, Litter> litterCardRenderer = new ComponentRenderer<>(
            litter -> {
                return createMiniLitterPanel(litter);
            });    

    private ComponentRenderer<Component, Stock> kitCardRenderer = new ComponentRenderer<>(
            stock -> {
                return createMiniKitCard(stock);
            });    

    private Layout createMiniLitterPanel(Litter litter){
        Layout box = new Layout();
        box.setFlexDirection(Layout.FlexDirection.COLUMN);
        box.addClassNames(FontSize.XSMALL);
        box.addClassNames(FontWeight.LIGHT);
        box.setWidth("134px");
        box.setHeight("62px");
        box.addClassNames(Margin.XSMALL, Padding.XSMALL);
        Layout headerRow = new Layout(new Span(litter.getName()));
        headerRow.setAlignItems(Layout.AlignItems.BASELINE);
        return box;
    }
    
    private Layout createMiniKitPanel(Stock kit){
        Layout box = new Layout();
        box.setFlexDirection(Layout.FlexDirection.COLUMN);
        box.addClassNames(FontSize.XSMALL);
        box.addClassNames(FontWeight.LIGHT);
        box.setWidth("134px");
        box.setHeight("62px");
        box.addClassNames(Margin.XSMALL, Padding.XSMALL);
        Layout headerRow = new Layout(new Span(kit.getDisplayName()));
        headerRow.setAlignItems(Layout.AlignItems.BASELINE);
        /*
        Component actionMenu = createDefaultActions();
        actionMenu.getStyle().set("margin-left", "auto");
        actionMenu.addClassNames(LumoUtility.Padding.NONE, LumoUtility.Margin.Top.NONE,LumoUtility.Margin.Bottom.NONE);
        headerRow.add(actionMenu);
        */
        box.add(headerRow);
        box.add(new Span(kit.getColor()));
        box.add(new Html(kit.getWeightInLbsOz()));

        UIUtilities.setBorders(box, null, false);
        box.addClassNames(Border.ALL,BorderRadius.MEDIUM, BoxShadow.SMALL);
        return box;
    }

    private Card createMiniKitCard(Stock kit){
        Card card = new Card();
        //card.setTitle(new Span(kit.getDisplayName()));
        //card.setSubtitle(new Span(kit.getColor()));
        Layout headerRow = new Layout(new Span(kit.getDisplayName()));
        headerRow.setAlignItems(Layout.AlignItems.BASELINE);
        card.setHeader(headerRow);
        Component actionMenu = UIUtilities.createDefaultActions();
        actionMenu.getStyle().set("margin-left", "auto");
        actionMenu.addClassNames(Padding.NONE, Margin.Top.NONE,Margin.Bottom.NONE);
        card.setHeaderSuffix(actionMenu);
        
        card.add(new Html(kit.getWeightInLbsOz()));
        card.addThemeVariants(CardVariant.LUMO_ELEVATED,CardVariant.LUMO_HORIZONTAL,CardVariant.LUMO_OUTLINED);
        card.addClassNames(Margin.SMALL);
        //card.setWidth("134px");
        //card.setHeight("62px");
        return card;
    }
    
    private void dialogValidate() {
        //validate fields and enable OK button if valid
        if(validationEnabled && this.stockEntity!=null){
            hasChangedValues = Boolean.FALSE;
            /*
            if(displayMode.equals(DisplayMode.GLOBAL)){
                log.info("dialogValidate: globalSubtotal:" + this.stockEntity.getGlobalSubtotal() + " field:" + fieldGlobalSubTotal.getValue());
                validateField(fieldGlobalSubTotal,this.stockEntity.getGlobalSubtotal());
                validateField(fieldGlobalTaxes.getNumberField(),this.stockEntity.getGlobalTotalTaxes());
                if(posGlobal){
                    validateField(fieldPaidToVendor,this.stockEntity.getPaidToVendor());
                }
            }else if(displayMode.equals(DisplayMode.CUSTOM)){
                validateField(fieldReceiptTotal,this.stockEntity.getReceiptTotal());
                validateCheckbox(fieldFeesOnly,this.stockEntity.getFeesOnly());
            }else{
                validateField(fieldReceiptTotal,this.stockEntity.getReceiptTotal());
                validateCheckbox(fieldWebOrder,this.stockEntity.getWebOrder());
            }
            //do common fields here
            validateListbox(fieldPaymentMethod,this.stockEntity.getPaymentMethod());
            validateField(fieldDeliveryFee,this.stockEntity.getDeliveryFee());
            validateField(fieldServiceFee,this.stockEntity.getServiceFee());
            validateField(fieldServiceFeePercent,getServiceFeePercent());
            validateField(fieldTotalSale.getNumberField(),this.stockEntity.getTotalSale());
            validateField(fieldTip,this.stockEntity.getTip());
            validateCheckbox(fieldTipIssue,this.stockEntity.getTipInNotesIssue());
            if(customTaskConverted) hasChangedValues = Boolean.TRUE;
            */
        }
        if(hasChangedValues){
            dialogOkButton.setEnabled(true);
            dialogResetButton.setEnabled(true);
        }else{
            dialogOkButton.setEnabled(false);
            dialogResetButton.setEnabled(false);
        }

    }

    private void validateField(NumberField field, Double value){
        if(value==null && field.getValue()==null){
            field.getStyle().set("box-shadow","none");
        }else if(value==null && field.getValue()!=null){
            field.getStyle().set("box-shadow",UIUtilities.boxShadowStyle);
            field.getStyle().set("border-radius",UIUtilities.boxShadowStyleRadius);
            hasChangedValues = Boolean.TRUE;
        }else if(field.getValue().equals(value)){
            field.getStyle().set("box-shadow","none");
        }else{
            field.getStyle().set("box-shadow",UIUtilities.boxShadowStyle);
            field.getStyle().set("border-radius",UIUtilities.boxShadowStyleRadius);
            hasChangedValues = Boolean.TRUE;
        }
    }

    private void validateCheckbox(Checkbox field, Boolean value){
        if(field.getValue().equals(value)){
            field.getStyle().set("box-shadow","none");
        }else{
            field.getStyle().set("box-shadow",UIUtilities.boxShadowStyle);
            field.getStyle().set("border-radius",UIUtilities.boxShadowStyleRadius);
            hasChangedValues = Boolean.TRUE;
        }
    }

    private void validateListbox(Select field, String value){
        log.info("validateListbox: fieldValue:" + field.getValue() + " value:" + value);
        if(field.getValue()==null || field.getValue().equals(value)){
            log.info("validateListbox: matched");
            field.getStyle().set("box-shadow","none");
        }else{
            log.info("validateListbox: NOT matched");
            field.getStyle().set("box-shadow",UIUtilities.boxShadowStyle);
            field.getStyle().set("border-radius",UIUtilities.boxShadowStyleRadius);
            hasChangedValues = Boolean.TRUE;
        }
    }


    public DialogMode getDialogMode() {
        return dialogMode;
    }

    public void setDialogMode(DialogMode dialogMode) {
        this.dialogMode = dialogMode;
    }

    public FormLayout showItem(Stock stockEntity){
        validationEnabled = Boolean.FALSE;
        FormLayout taskFormLayout = new FormLayout();
        taskFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));
        /*
        taskFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("100px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));

         */

        this.stockEntity = stockEntity;

        //determine display mode
        /*
        if(this.stockEntity.getCreatedBy().equals(43L)){
            displayMode = DisplayMode.GLOBAL;
            //determine if this restaurant uses pos payment like Smiley's
            List<Restaurant> restaurants = restaurantRepository.findEffectiveByRestaurantId(this.stockEntity.getRestaurantId(),this.stockEntity.getCreationDate().toLocalDate());
            if(restaurants!=null && restaurants.size()>0){
                posGlobal = restaurants.get(0).getPosGlobal();
            }
        }else if(this.stockEntity.getRestaurantId().equals(0L)){ //custom
            displayMode = DisplayMode.CUSTOM;
        }else{
            displayMode = DisplayMode.PHONEIN;
        }
        */
        //configure the field layout
        log.info("showTask: display mode set to:" + displayMode);
        taskFormLayout.removeAll();

        //add the header
        taskFormLayout.add(getStockHeader(this.stockEntity, Boolean.FALSE));

        //common fields

        //fields by displayMode type
        if(this.displayMode.equals(DisplayMode.STOCK_DETAILS)){
            taskFormLayout.addFormItem(fieldGlobalSubTotal,"Global Subtotal");
            taskFormLayout.addFormItem(fieldGlobalTaxes,"Global Taxes");
            taskFormLayout.addFormItem(fieldPaymentMethod,"Payment");
            taskFormLayout.addFormItem(fieldDeliveryFee,"Delivery Fee");
            taskFormLayout.addFormItem(fieldServiceFeePercent,"Service Fee(%)");
            taskFormLayout.addFormItem(fieldServiceFee,"Service Fee($)");
            taskFormLayout.addFormItem(fieldTotalSale,"Total sale");
            taskFormLayout.addFormItem(fieldTip,"Tip");
            taskFormLayout.addFormItem(fieldTotalWithTip,"Total with tip");
            //TODO:: only add if superUser
            taskFormLayout.addFormItem(fieldTipIssue,"Tip issue");
            taskFormLayout.addFormItem(fieldNotes, "Notes");
            dialogConvertCustom.setVisible(false);
        }else if(this.displayMode.equals(DisplayMode.LITTER_LIST)){
            taskFormLayout.addFormItem(fieldReceiptTotal,"Receipt total");
            taskFormLayout.addFormItem(fieldFeesOnly,"Fees only");
            taskFormLayout.addFormItem(fieldPaymentMethod,"Payment");
            taskFormLayout.addFormItem(fieldDeliveryFee,"Delivery Fee");
            taskFormLayout.addFormItem(fieldServiceFeePercent,"Service Fee(%)");
            taskFormLayout.addFormItem(fieldServiceFee,"Service Fee($)");
            taskFormLayout.addFormItem(fieldTotalSale,"Total sale");
            taskFormLayout.addFormItem(fieldTip,"Tip");
            taskFormLayout.addFormItem(fieldTotalWithTip,"Total with tip");
            //TODO:: only add if superUser
            taskFormLayout.addFormItem(fieldTipIssue,"Tip issue");
            taskFormLayout.addFormItem(fieldNotes, "Notes");
            dialogConvertCustom.setVisible(true);
        }else if(this.displayMode.equals(DisplayMode.KIT_LIST)){
            taskFormLayout.addFormItem(fieldReceiptTotal,"Receipt total");
            taskFormLayout.addFormItem(fieldWebOrder,"Web order");
            taskFormLayout.addFormItem(fieldPaymentMethod,"Payment");
            taskFormLayout.addFormItem(fieldDeliveryFee,"Delivery Fee");
            taskFormLayout.addFormItem(fieldServiceFeePercent,"Service Fee(%)");
            taskFormLayout.addFormItem(fieldServiceFee,"Service Fee($)");
            taskFormLayout.addFormItem(fieldTotalSale,"Total sale");
            taskFormLayout.addFormItem(fieldTip,"Tip");
            taskFormLayout.addFormItem(fieldTotalWithTip,"Total with tip");
            //TODO:: only add if superUser
            taskFormLayout.addFormItem(fieldTipIssue,"Tip issue");
            taskFormLayout.addFormItem(fieldNotes, "Notes");
            dialogConvertCustom.setVisible(false);
        }

        //set values
        setValues();
        return taskFormLayout;
    }

    private void setValues(){
        validationEnabled = Boolean.FALSE;
        /*
        if(Config.getInstance().getPaymentMethods().contains(this.stockEntity.getPaymentMethod())){
            fieldPaymentMethod.setValue(this.stockEntity.getPaymentMethod());
        }else{
            fieldPaymentMethod.clear();
        }
        fieldDeliveryFee.setValue(this.stockEntity.getDeliveryFee());
        fieldServiceFeePercent.setValue(getServiceFeePercent());
        fieldServiceFee.setValue(this.stockEntity.getServiceFee());
        fieldTotalSale.setValue(this.stockEntity.getTotalSale());
        fieldTip.setValue(this.stockEntity.getTip());
        fieldTotalWithTip.setValue(Utility.getInstance().round(this.stockEntity.getTip() + this.stockEntity.getTotalSale(),2));
        fieldTipIssue.setValue(this.stockEntity.getTipInNotesIssue());
        if(this.stockEntity.getNotes()!=null){
            fieldNotes.setValue(this.stockEntity.getNotes());
        }

        if(this.displayMode.equals(DisplayMode.GLOBAL)){
            fieldGlobalSubTotal.setValue(this.stockEntity.getGlobalSubtotal());
            fieldGlobalTaxes.setValue(this.stockEntity.getGlobalTotalTaxes());
            if(posGlobal){
                fieldPaidToVendor.setValue(this.stockEntity.getPaidToVendor());
            }
        }else if(this.displayMode.equals(DisplayMode.CUSTOM)){
            fieldFeesOnly.setValue(this.stockEntity.getFeesOnly());
            fieldReceiptTotal.setValue(this.stockEntity.getReceiptTotal());
        }else{
            //Phonein
            fieldWebOrder.setValue(this.stockEntity.getWebOrder());
            fieldReceiptTotal.setValue(this.stockEntity.getReceiptTotal());
        }
        */
        validationEnabled = Boolean.TRUE;

    }

    public HorizontalLayout getStockHeader(Stock item, Boolean fullHeader){
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        //Image img = new Image(item.getDefaultImageSource(), item.getName());
        Avatar stockName = new Avatar();
        stockName.setName(item.getName());
        stockName.setColorIndex(5);
        stockName.addThemeVariants(AvatarVariant.LUMO_LARGE);
        stockName.setImage(item.getDefaultImageSource());

        VerticalLayout columnAvatars = new VerticalLayout(stockName);

        /*
        SvgIcon genderIcon = item.getGenderIcon();
        if(genderIcon!=null){
            columnAvatars.add(genderIcon);
        }
        */
        //Avatar genderType = new Avatar();
        //genderType.setName(item.getSex().getShortName());
        //genderType.setColorIndex(Utility.getInstance().);
        //genderType.addThemeVariants(AvatarVariant.LUMO_XSMALL);

        columnAvatars.setPadding(false);
        columnAvatars.setSpacing(true);
        columnAvatars.setWidth("75px");
        columnAvatars.setAlignItems(FlexComponent.Alignment.CENTER);
        columnAvatars.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout columnInfo = new VerticalLayout();
        columnInfo.setPadding(false);
        columnInfo.setSpacing(false);

        columnInfo.add(item.getHeader());
        /*
        String customerNameString = formatCustomerName(item);
        if(customerNameString!=null){
            Span customerName = new Span(customerNameString);
            customerName.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            columnInfo.add(customerName);
        }
        */

        if(fullHeader){
            Span customerAddress = new Span(item.getWeightInLbsOz());
            customerAddress.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            columnInfo.add(customerAddress);
        }
        String extraInfo = "";
        if(!item.getTattoo().isEmpty()){
            extraInfo = "(" + item.getTattoo() + ")";
        }
        if(!item.getBreed().isEmpty()){
            extraInfo += "(" + item.getBreed() + ")";
        }
        if(!item.getColor().isEmpty()){
            extraInfo += "(" + item.getColor() + ")";
        }
        
        Span xInfo = new Span(extraInfo);
        xInfo.getStyle()
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        columnInfo.add(xInfo);

        if(fullHeader){
            /*
            Span paymentInfo;
            if(item.getPaymentMethod()==null){
                paymentInfo = new Span("Total:" + item.getTotalSale() + " Tip:" + item.getTip());
            }else{
                paymentInfo = new Span("Total:" + item.getTotalSale() + " Tip:" + item.getTip() + " - " + item.getPaymentMethod());
            }
            paymentInfo.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");
            columnInfo.add(paymentInfo);
            */
        }

        row.add(columnAvatars, columnInfo);
        row.getStyle().set("line-height", "var(--lumo-line-height-m)");
        return row;
    }

    private Integer getColorIndex(String taskTypeName){
        if(taskTypeName.equals("Global")) return 0;
        if(taskTypeName.equals("Form")) return 1;
        else return 2;
    }

    private String formatIds(String jobId, String orderId, String refNumber){
        String idString = "Jobid:" + jobId ;
        if(orderId!=null && !orderId.isEmpty()){
            idString = idString + " Orderid:" + orderId.trim();
        }
        if(refNumber!=null && !refNumber.isEmpty()){
            idString = idString + " Ref:" + refNumber.trim();
        }
        return idString;
    }

    private String formatCustomerName(Stock item){
        if(item.getDisplayName()==null || item.getName().isEmpty()){
            return null;
        }else{
            return item.getDisplayName();
        }
    }

    private String getStatusStyle(Long jobStatus){
        String statusString = "badge";
        if(jobStatus.equals(0L)) return "badge";
        if(jobStatus.equals(1L)) return "badge";
        if(jobStatus.equals(2L)) return "badge success";
        if(jobStatus.equals(3L)) return "badge error";
        if(jobStatus.equals(4L)) return "badge";
        //if(jobStatus.equals(5L)) return "";
        if(jobStatus.equals(6L)) return "badge contrast";
        if(jobStatus.equals(7L)) return "badge";
        if(jobStatus.equals(8L)) return "badge error";
        if(jobStatus.equals(9L)) return "badge error";
        if(jobStatus.equals(10L)) return "badge error";
        return "badge error";
    }

    private void dialogAdvConfigure() {
        dialogAdv.getElement().setAttribute("aria-label", "Convert Custom Task");

        //configure the dialog internal layout for the form
        dialogAdvLayout.setSpacing(false);
        dialogAdvLayout.setPadding(false);
        dialogAdvLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogAdvLayout.getStyle().set("width", "400px").set("max-width", "100%");

        dialogAdv.add(dialogAdvLayout);
        dialogAdv.setHeaderTitle("Convert Custom Task");

        dialogAdvCloseButton.addClickListener((e) -> dialogAdv.close());
        dialogAdvCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialogAdv.getHeader().add(dialogAdvCloseButton);
        dialogAdv.setCloseOnEsc(true);
        dialogAdvCancelButton.addClickListener((e) -> dialogAdv.close());

        dialogAdvOkButton.addClickListener(
                event -> {
                    dialogAdvSave();
                }
        );
        dialogAdvOkButton.addClickShortcut(Key.ENTER);
        dialogAdvOkButton.setEnabled(false);
        dialogAdvOkButton.setDisableOnClick(true);

        HorizontalLayout footerLayoutAdv = new HorizontalLayout(dialogAdvOkButton,dialogAdvCancelButton);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayoutAdv, () -> {}, Key.ENTER)
                .listenOn(footerLayoutAdv);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialogAdv.getFooter().add(footerLayoutAdv);

        //one time configuration for any fields
        List<AdvDialogMode> convertTypeList = Stream.of(AdvDialogMode.values()).collect(Collectors.toList());
        advFieldConvertType.setItems(convertTypeList);
        advFieldConvertType.setPlaceholder("Select conversion type");
        advFieldConvertType.addValueChangeListener(item -> {
            dialogAdvValidate();
        });

        /*
        advFieldRestaurant.setItems(restaurantRepository.findDistinctNonExpiredRestaurants());
        advFieldRestaurant.setItemLabelGenerator(Restaurant::getName);
        advFieldRestaurant.setReadOnly(false);
        advFieldRestaurant.setPlaceholder("Select restaurant");
        advFieldRestaurant.addValueChangeListener(item -> {
            dialogAdvValidate();
        });

        advFieldOrderId.addValueChangeListener(item -> {
            dialogAdvValidate();
        });
        */

    }

    private void dialogAdvValidate() {

        if(advFieldConvertType.getValue()==null){
            advFieldOrderId.setEnabled(false);
        }else if(advFieldConvertType.getValue().equals(AdvDialogMode.Global)){
            advFieldOrderId.setEnabled(true);
        }else{
            advFieldOrderId.setEnabled(false);
        }

        /*
        if(advFieldRestaurant.getValue().getRestaurantId().equals(0L)){
            dialogAdvOkButton.setEnabled(false);
        }else if(advFieldConvertType.getValue().equals(AdvDialogMode.Global)){
            //Global validation here which includes a valid orderid
            if(advFieldOrderId.getValue()==null || advFieldOrderId.getValue().isEmpty() || advFieldOrderId.getValue().equals("0")){
                dialogAdvOkButton.setEnabled(false);
            }else{
                OrderDetailRepository orderDetailRepository = Registry.getBean(OrderDetailRepository.class);
                Long orderId = Long.valueOf(advFieldOrderId.getValue());
                if(orderId==null){
                    dialogAdvOkButton.setEnabled(false);
                }else{
                    OrderDetail orderDetail = orderDetailRepository.findOrderDetailByOrderId(orderId);
                    if(orderDetail==null){
                        dialogAdvOkButton.setEnabled(false);
                    }else{
                        dialogAdvOkButton.setEnabled(true);
                    }
                }
            }
        }else{  //form does not need order id
            dialogAdvOkButton.setEnabled(true);
        }
        */
    }

    private void dialogAdvSave() {

        /*
        if(advFieldConvertType.getValue().equals(AdvDialogMode.Global)){
            this.stockEntity.setTemplateId("Order_Details");
            this.stockEntity.setCreatedBy(43L);
            this.stockEntity.setOrderId(advFieldOrderId.getValue());
            this.stockEntity.setLongOrderId(Long.valueOf(advFieldOrderId.getValue()));
            OrderDetailRepository orderDetailRepository = Registry.getBean(OrderDetailRepository.class);
            OrderDetail orderDetail = orderDetailRepository.findOrderDetailByOrderId(this.stockEntity.getLongOrderId());
            this.stockEntity.updateGlobalData(orderDetail);

        }else{
            this.stockEntity.setCreatedBy(3L);
            this.stockEntity.setOrderId(advFieldRestaurant.getValue().getFormId().toString());
            this.stockEntity.setLongOrderId(advFieldRestaurant.getValue().getFormId());
        }
        this.stockEntity.setPosPayment(false);
        this.stockEntity.setRestaurantId(advFieldRestaurant.getValue().getRestaurantId());
        this.stockEntity.setRestaurantName(advFieldRestaurant.getValue().getName());
        this.stockEntity.updateCalculatedFields();

        //update fields needed for Global Tasks
        customTaskConverted = Boolean.TRUE;
        */
        
        //dialogOkButton.setEnabled(false);
        dialogLayout.removeAll();
        dialogLayout.add(showItem(this.stockEntity));
        dialogValidate();
        dialogAdv.close();

    }

    private void dialogAdvOpen(){
        customTaskConverted = Boolean.FALSE;
        dialogAdvLayout.removeAll();
        /*
        dialogAdvLayout.add(advFieldConvertType, advFieldRestaurant,advFieldOrderId);

        //set values
        List<Restaurant> restaurants = restaurantRepository.findEffectiveByRestaurantId(this.stockEntity.getRestaurantId(), LocalDate.now());
        Restaurant restaurant = restaurants.get(0);
        advFieldRestaurant.setValue(restaurant);

        advFieldConvertType.setValue(AdvDialogMode.Global);

        if(this.stockEntity.getOrderId()!=null){
            advFieldOrderId.setValue(this.stockEntity.getOrderId());
        }
        */

        dialogAdv.open();

    }

    public void addListener(ListRefreshNeededListener listener){
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded(){
        for (ListRefreshNeededListener listener: listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }
    
}
