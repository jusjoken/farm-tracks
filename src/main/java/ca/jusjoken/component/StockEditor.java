/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.component;

/**
 *
 * @author birch
 */
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.Utility.StockSaleStatus;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.entity.StockWeightHistory;
import ca.jusjoken.data.service.AppSettingsService;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.ParentIntegerToStockConverter;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StatusHistoryConverter;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockWeightHistoryService;

@Tag("dialog-common")
public class StockEditor extends Component{

    public enum DialogMode{
        EDIT, DELETE, VIEW
    }

    private Binder<Stock> binder;
    private Registration nameChangedReg;
    private Registration tattooChangedReg;
    private static final String NOT_FOSTERED = "Not fostered";

    public enum DisplayMode{
        LITTER_LIST, KIT_LIST, STOCK_DETAILS, PROFILE_IMAGE
    }
    //private DisplayMode displayMode = DisplayMode.PROFILE_IMAGE;

    private final Logger log = LoggerFactory.getLogger(StockEditor.class);
    private final Dialog dialog = new Dialog();
    private Stock stockEntity;
    private String dialogTitle = "";
    private Boolean isNewStock = Boolean.FALSE;
    private StockSaleStatus openedSaleStatus = StockSaleStatus.NONE;
    
    private Integer returnId = null;
    private Stock returnStock;

    private final List<Stock> stockList = new ArrayList<>();
    private final List<Litter> litterList = new ArrayList<>();
    
    private final Button dialogResetButton = new Button("Reset");
    private final Button dialogOkButton = new Button("OK");
    private final Button dialogCancelButton = new Button("Cancel");
    private final Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    private final Button fieldGenotypeEditButton = new Button(new Icon(Utility.ICONS.ACTION_PEDIGREE.getIconSource()));
    private final Button fieldStatusEditButton = new Button(new Icon(Utility.ICONS.ACTION_EDIT.getIconSource()));
    private final Button fieldSaleStatusEditButton = new Button(new Icon(Utility.ICONS.ACTION_EDIT.getIconSource()));
    private final HorizontalLayout fieldGenotypeLayout = new HorizontalLayout();
    private final HorizontalLayout fieldStatusLayout = new HorizontalLayout();
    private final HorizontalLayout fieldSaleStatusLayout = new HorizontalLayout();

    private Upload dialogUploadComponent;

    private static final String[] ACCEPTED_MIME_TYPES =
      {"image/gif", "image/png", "image/jpeg", "image/bmp", "image/webp"};
    private byte[] profileImageData = null;
    private String profileImageMimeType = null;

    //Fields defined here
    //global fields
    private final Avatar fieldProfileAvatar = new Avatar();
    private final Div avatarDiv = new Div(fieldProfileAvatar);    
    private ImageCrop fieldProfileImageCrop = new ImageCrop(fieldProfileAvatar.getImage());
    private final Checkbox fieldProfileUseCamera = new Checkbox("Use camera if available");
    private final Button dialogProfileImageRotateButton = new Button("Rotate");
    private Boolean profileAvatarHasChanges = Boolean.FALSE;


    @PropertyId("external")
    private final Checkbox fieldExternal = new Checkbox();
    @PropertyId("breeder")
    private final Checkbox fieldBreeder = new Checkbox();
    @PropertyId("sex")
    private final RadioButtonGroup<Gender> fieldGender = new RadioButtonGroup<>();
    @PropertyId("prefix")
    private final TextField fieldPrefix = new TextField();
    @PropertyId("name")
    private final TextField fieldName = new TextField();
    @PropertyId("tattoo")
    private final TextField fieldTattoo = new TextField();
    @PropertyId("color")
    private final TextField fieldColor = new TextField();
    @PropertyId("breed")
    private final TextField fieldBreed = new TextField();
    @PropertyId("weight")
    private final WeightInput fieldWeight = new WeightInput();
    
    @PropertyId("acquired")
    private final DatePicker fieldAquiredDate = new DatePicker();
    @PropertyId("doB")
    private final DatePicker fieldBornDate = new DatePicker();
    @PropertyId("legs")
    private final TextField fieldLegs = UIUtilities.getTextField();
    @PropertyId("champNo")
    private final TextField fieldChampNo = UIUtilities.getTextField();
    @PropertyId("regNo")
    private final TextField fieldRegNo = UIUtilities.getTextField();

    @PropertyId("fatherId")
    private final ComboBox<Stock> fieldFather = new ComboBox<>();
    @PropertyId("motherId")
    private final ComboBox<Stock> fieldMother = new ComboBox<>();


    @PropertyId("genotype")
    private final TextField fieldGenotype = UIUtilities.getTextField();
    
    @PropertyId("status")
    private final TextField fieldStatus = new TextField();

    @PropertyId("fosterLitter")
    private final Select<Litter> fieldFoster = new Select<>();

    @PropertyId("notes")
    private final TextArea fieldNotes = new TextArea();

    @PropertyId("stockValue")
    private final NumberField fieldValue = UIUtilities.getNumberField("",Boolean.FALSE,"$");

    @PropertyId("invoiceNumber")
    private final TextField fieldInvoiceNumber = UIUtilities.getTextField();

    private final TextField fieldSaleStatus = new TextField();

    //FormItems that may be hidden
    FormItem fieldFosterFormItem;
    FormItem fieldStatusFormItem;
    FormItem fieldAquiredFormItem;
    FormItem fieldSaleStatusFormItem;
    FormItem fieldValueFormItem;
    FormItem fieldInvoiceFormItem;

    private final VerticalLayout dialogLayout = new VerticalLayout();

    private StockService stockService;
    private LitterService litterService;
    private AppSettingsService appSettingsService;
    private StockStatusHistoryService statusService;
    private StockWeightHistoryService weightService;
    private GenotypeEditor genotypeEditor;
    private StatusEditor statusEditor;
    private DisplayMode openedDisplayMode = DisplayMode.STOCK_DETAILS;
    private boolean lockGenderSelection = false;

    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    //private String profileImagePath;
    
    public StockEditor() {
        this(DisplayMode.STOCK_DETAILS);
    }

    public StockEditor(DisplayMode currentDisplayMode) {
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.appSettingsService = Registry.getBean(AppSettingsService.class);
        this.statusService = Registry.getBean(StockStatusHistoryService.class);
        this.weightService = Registry.getBean(StockWeightHistoryService.class);
        //profileImagePath = System.getenv("PATH_TO_PROFILE_IMAGE");
        dialogConfigure();
    }


    private void dialogConfigure() {

        //configure the dialog internal layout for the form
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        UIUtilities.applyDialogWidth(dialog, dialogLayout, UIUtilities.DialogWidthPreset.XLARGE);

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

        dialogResetButton.setEnabled(false);
        dialogResetButton.addClickListener(
                event -> {
                    dialogReset(openedDisplayMode);
                }
        );

        HorizontalLayout footerLayout = new HorizontalLayout(dialogOkButton,dialogCancelButton,dialogResetButton);
        UIUtilities.applyResponsiveDialogFooter(footerLayout);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //one time configuration for any fields
        fieldGender.setItems(Gender.MALE, Gender.FEMALE, Gender.NA);
        fieldMother.setAllowCustomValue(true);
        fieldFather.setAllowCustomValue(true);
        fieldFoster.setEmptySelectionAllowed(true);
        fieldFoster.setEmptySelectionCaption(NOT_FOSTERED);
        fieldExternal.setLabel("not on farm");
        
        //form fields need width full to fill the column they are in
        fieldExternal.setWidthFull();
        fieldBreeder.setWidthFull();
        fieldGender.setWidthFull();
        fieldPrefix.setWidthFull();
        fieldName.setWidthFull();
        fieldTattoo.setWidthFull();
        fieldColor.setWidthFull();
        fieldBreed.setWidthFull();
        fieldWeight.setWidthFull();
        fieldFather.setWidthFull();
        fieldMother.setWidthFull();
        fieldStatus.setWidthFull();
        fieldFoster.setWidthFull();
        fieldAquiredDate.setWidthFull();
        fieldBornDate.setWidthFull();
        fieldChampNo.setWidthFull();
        fieldGenotype.setWidthFull();
        fieldLegs.setWidthFull();
        fieldRegNo.setWidthFull();
        fieldNotes.setWidthFull();
        fieldValue.setWidthFull();
        fieldInvoiceNumber.setWidthFull();
        fieldSaleStatus.setWidthFull();

        fieldSaleStatus.setReadOnly(true);

        fieldValue.setLabel("");
        fieldInvoiceNumber.setLabel("");
        fieldValue.removeThemeVariants(TextFieldVariant.LUMO_SMALL);
        fieldInvoiceNumber.removeThemeVariants(TextFieldVariant.LUMO_SMALL);

        fieldSaleStatusEditButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        fieldSaleStatusEditButton.getElement().setAttribute("aria-label", "Edit sale status");
        fieldSaleStatusEditButton.setTooltipText("Edit sale status");
        fieldSaleStatusEditButton.addClickListener(event -> openSaleStatusEditor());

        fieldSaleStatusLayout.setWidthFull();
        fieldSaleStatusLayout.setSpacing(true);
        fieldSaleStatusLayout.setPadding(false);
        fieldSaleStatusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        fieldSaleStatusLayout.add(fieldSaleStatus, fieldSaleStatusEditButton);
        fieldSaleStatusLayout.expand(fieldSaleStatus);
        fieldSaleStatus.getStyle().set("min-width", "0");
        fieldSaleStatus.getStyle().set("flex", "1 1 auto");
        fieldSaleStatusEditButton.getStyle().set("flex-shrink", "0");

        fieldPrefix.setAutoselect(true);
        fieldName.setAutoselect(true);
        fieldTattoo.setAutoselect(true);
        fieldColor.setAutoselect(true);
        fieldBreed.setAutoselect(true);
        fieldLegs.setAutoselect(true);
        fieldChampNo.setAutoselect(true);
        fieldRegNo.setAutoselect(true);
        fieldGenotype.setAutoselect(true);
        fieldStatus.setAutoselect(true);
        fieldNotes.setAutoselect(true);
        fieldValue.setAutoselect(true);
        fieldInvoiceNumber.setAutoselect(true);

        fieldGenotypeEditButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        fieldGenotypeEditButton.getElement().setAttribute("aria-label", "Edit genotype");
        fieldGenotypeEditButton.setTooltipText("Edit genotype");
        fieldGenotypeEditButton.addClickListener(event -> openGenotypeEditor());

        fieldGenotypeLayout.setWidthFull();
        fieldGenotypeLayout.setSpacing(true);
        fieldGenotypeLayout.setPadding(false);
        fieldGenotypeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        fieldGenotypeLayout.add(fieldGenotype, fieldGenotypeEditButton);
        fieldGenotypeLayout.expand(fieldGenotype);
        fieldGenotype.setWidth(null);
        fieldGenotype.getStyle().set("min-width", "0");
        fieldGenotype.getStyle().set("flex", "1 1 auto");
        fieldGenotypeEditButton.getStyle().set("flex-shrink", "0");

        fieldStatusEditButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        fieldStatusEditButton.getElement().setAttribute("aria-label", "Edit status");
        fieldStatusEditButton.setTooltipText("Edit status");
        fieldStatusEditButton.addClickListener(event -> openStatusEditor());

        fieldStatusLayout.setWidthFull();
        fieldStatusLayout.setSpacing(true);
        fieldStatusLayout.setPadding(false);
        fieldStatusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        fieldStatusLayout.add(fieldStatus, fieldStatusEditButton);
        fieldStatusLayout.expand(fieldStatus);
        fieldStatus.setWidth(null);
        fieldStatus.getStyle().set("min-width", "0");
        fieldStatus.getStyle().set("flex", "1 1 auto");
        fieldStatusEditButton.getStyle().set("flex-shrink", "0");

        fieldFather.addCustomValueSetListener(item -> {
            Stock externalFather = stockService.getOrCreateExternalParent(item.getDetail(), stockEntity.getStockType(), Gender.MALE);
            fieldFather.setItems(stockService.getFathers(null, stockEntity.getStockType()));
            fieldFather.setValue(externalFather);
            System.out.println("**Father custom changed to:" + item.getDetail() + " set parent =" + externalFather);
        });
        fieldMother.addCustomValueSetListener(item -> {
            Stock externalMother = stockService.getOrCreateExternalParent(item.getDetail(), stockEntity.getStockType(), Gender.FEMALE);
            fieldMother.setItems(stockService.getMothers(null, stockEntity.getStockType()));
            fieldMother.setValue(externalMother);
            System.out.println("**Mother custom changed to:" + item.getDetail() + " set parent =" + externalMother);
        });
        
        fieldExternal.addValueChangeListener(event -> {
            boolean showNonExternalFields = !event.getValue();
            toggleHiddenFormFields(showNonExternalFields);
            toggleSaleFormFields(showNonExternalFields);
        });

    }
    
    private void dialogReset(DisplayMode currentDisplayMode){
        System.out.println("dialogReset: currentDisplayMode:" + currentDisplayMode);
        dialogOkButton.setEnabled(false);
        dialogResetButton.setEnabled(false);
        setValues(this.stockEntity, currentDisplayMode);
        if(dialogUploadComponent!=null) dialogUploadComponent.clearFileList();
    }

    private void dialogClose(){
        dialog.close();
    }

    private void dialogSave() {
        returnId = null;
        returnStock = null;
        DisplayMode currentDisplayMode = openedDisplayMode;
        //System.out.println("dialogSave: stockEntity start save:" + stockEntity);
        //save here
        
        //retrieve the stock entity from the database before saving so it is the same entity
        if(this.stockEntity.getId()!=null) this.stockEntity = stockService.findById(stockEntity.getId());
        
        //update stockEntity from fields
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            saveSelectedParent(fieldFather == null ? null : fieldFather.getValue(), Gender.MALE);
            saveSelectedParent(fieldMother == null ? null : fieldMother.getValue(), Gender.FEMALE);
            //write all bound fields back to the entity
            try {
                binder.writeBean(stockEntity);
            } catch (ValidationException ex) {
                System.out.println("dialogSave: Validation Error writing bean stockEntity:" + ex);
            }

            //System.out.println("dialogSave: stockEntity before save:" + stockEntity);
            stockService.save(this.stockEntity);

            //handle NEW stock items
            if(isNewStock){
                if(this.stockEntity.getExternal()){
                    //use archived status for external stock
                    statusService.save(new StockStatusHistory(this.stockEntity.getId(),"archived",LocalDateTime.now()),this.stockEntity,Boolean.FALSE);
                    
                }else{
                    //use active status
                    statusService.save(new StockStatusHistory(this.stockEntity.getId(),"active",LocalDateTime.now()),this.stockEntity,Boolean.FALSE);
                }
                if(this.stockEntity.getWeight()>0){
                    //save a new weight history item
                    weightService.save(new StockWeightHistory(this.stockEntity.getId(),this.stockEntity.getWeight(),LocalDateTime.now()),this.stockEntity);
                }
            }

            saveStatusHistoryForSaleStatusChange();

            returnId = this.stockEntity.getId();
            returnStock = this.stockEntity;
            //System.out.println("dialogSave: stockEntity after save:" + stockEntity);
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            //write the profile image to file
            try (OutputStream outputStream = Files.newOutputStream(stockEntity.getProfileFileToBeSaved().toPath())) {
                outputStream.write(profileImageData);
                profileAvatarHasChanges = Boolean.FALSE;
                validateProfileAvatar();
            } catch (IOException e) {
                // Handle exception
                log.info("Error saving the avatar image:" + e);
            }            
        }

        //refresh if needed
        log.info("dialogSave: notifying listeners");
        dialog.close();
        notifyRefreshNeeded();
    }

    private void saveSelectedParent(Stock selectedParent, Gender parentGender) {
        if (selectedParent == null) {
            if (parentGender.equals(Gender.MALE)) {
                this.stockEntity.setFatherId(null);
                this.stockEntity.setFatherExtName(null);
            } else if (parentGender.equals(Gender.FEMALE)) {
                this.stockEntity.setMotherId(null);
                this.stockEntity.setMotherExtName(null);
            }
            return;
        }

        Stock parentToLink = selectedParent;
        if (selectedParent.isTemp()) {
            parentToLink = stockService.getOrCreateExternalParent(selectedParent.getDisplayName(), this.stockEntity.getStockType(), parentGender);
        }

        if (parentGender.equals(Gender.MALE)) {
            this.stockEntity.setFatherId(parentToLink == null ? null : parentToLink.getId());
            this.stockEntity.setFatherExtName(null);
        } else if (parentGender.equals(Gender.FEMALE)) {
            this.stockEntity.setMotherId(parentToLink == null ? null : parentToLink.getId());
            this.stockEntity.setMotherExtName(null);
        }
    }

    public void dialogOpen(Integer stockID, DisplayMode currentDisplayMode){
        Stock stockEntityItem = stockService.findById(stockID);
        if(stockEntityItem!=null){
            dialogOpen(stockEntityItem, currentDisplayMode);
        }else{
            log.info("StockEditDialog: dialogOpen: failed to find stock with Id:" + stockID);
        }
    }
    public void dialogOpen(Stock stockEntity, DisplayMode currentDisplayMode){
        dialogOpen(stockEntity, currentDisplayMode, false);
    }

    public void dialogOpen(Stock stockEntity, DisplayMode currentDisplayMode, boolean lockGenderSelection){
        binder = new Binder<>(Stock.class);
        this.lockGenderSelection = lockGenderSelection;
        this.stockEntity = stockEntity;
        openedSaleStatus = stockEntity.getSaleStatus() == null ? StockSaleStatus.NONE : stockEntity.getSaleStatus();
        
        if(this.stockEntity.getId()==null){
            isNewStock = Boolean.TRUE;
            applyDefaultFarmPrefixForNewStock();
        }else isNewStock = Boolean.FALSE;
        
        openedDisplayMode = currentDisplayMode;
        if (currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)) {
            UIUtilities.applyDialogWidth(dialog, dialogLayout, UIUtilities.DialogWidthPreset.COMPACT);
        } else {
            UIUtilities.applyDialogWidth(dialog, dialogLayout, UIUtilities.DialogWidthPreset.XLARGE);
        }
        //set values and visibility for fields
        clearLists();
        dialogLayout.removeAll();
        switch (currentDisplayMode) {
            case STOCK_DETAILS -> //binder.readBean(this.stockEntity);
                dialogLayout.add(showItem(this.stockEntity, currentDisplayMode));
            case PROFILE_IMAGE -> {
                profileAvatarHasChanges = Boolean.FALSE;
                validateProfileAvatar();
                //load stored image or if none load default image
                fieldProfileAvatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
                fieldProfileAvatar.setHeight("12em");
                fieldProfileAvatar.setWidth("12em");
                if(this.stockEntity.getProfileFile()!=null){
                    fieldProfileAvatar.setImageHandler(DownloadHandler.forFile(this.stockEntity.getProfileFile()));
                    fieldProfileAvatar.setName(this.stockEntity.getProfileImage());
                    profileImageData = getByteArrayFromImageFile(this.stockEntity.getProfileFile().getAbsolutePath());
                    profileImageMimeType = "image/*";
                }
                dialogLayout.add(showItem(this.stockEntity,currentDisplayMode), createImageUploadLayout());
            }
            default -> {
            }
        }
        //nothing

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        UIUtilities.applyDialogDraggableForViewport(dialog);
        dialog.setResizable(true);
        //dialog.addClassNames("backdrop-blur-none");

        dialog.open();
        focusFirstEditableField(currentDisplayMode);
    }

    private void saveStatusHistoryForSaleStatusChange() {
        statusService.saveStatusHistoryForSaleStatusChange(stockEntity, openedSaleStatus);
    }

    private void applyDefaultFarmPrefixForNewStock() {
        if (stockEntity == null) {
            return;
        }

        // External entries are not farm-owned stock, so default prefix should be blank.
        if (Boolean.TRUE.equals(stockEntity.getExternal())) {
            stockEntity.setPrefix("");
            return;
        }

        if (stockEntity.getPrefix() != null && !stockEntity.getPrefix().trim().isEmpty()) {
            return;
        }

        String farmPrefix = appSettingsService.getAppSettings().getFarmPrefix();
        if (farmPrefix != null && !farmPrefix.trim().isEmpty()) {
            stockEntity.setPrefix(farmPrefix.trim());
        }
    }

    private void focusFirstEditableField(DisplayMode currentDisplayMode) {
        if (currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)) {
            if (fieldPrefix.isVisible() && fieldPrefix.isEnabled() && !fieldPrefix.isReadOnly()) {
                fieldPrefix.focus();
                return;
            }
            if (fieldName.isVisible() && fieldName.isEnabled() && !fieldName.isReadOnly()) {
                fieldName.focus();
            }
        }
    }
    
    private void validateProfileAvatar(){
        if(profileAvatarHasChanges){
            dialogResetButton.setEnabled(true);
            dialogOkButton.setEnabled(true);
        }else{
            dialogResetButton.setEnabled(false);
            dialogOkButton.setEnabled(false);
        }
        
    }
    
    private VerticalLayout createImageUploadLayout() {
        VerticalLayout uploadLayout = UIUtilities.getVerticalLayout();

        InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory(
            (metadata, data) -> {
                byte[] newData = null;
                try {
                    // Get other information about the file.
                    newData = getResizedImage(data, 600);
                } catch (Exception ex) {
                    System.getLogger(StockEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
                
                profileImageData = newData;
                profileImageMimeType = metadata.contentType();
                String src = getImageAsBase64(newData, metadata.contentType());
                //System.out.println("***Upload file: Base64:" + src);
                fieldProfileAvatar.setImage(src);
                fieldProfileAvatar.setName(metadata.fileName());
                profileAvatarHasChanges = Boolean.TRUE;
                validateProfileAvatar();
            });

        dialogUploadComponent = new Upload(inMemoryHandler);  
        dialogUploadComponent.setMaxFiles(1);
        dialogUploadComponent.setMaxFileSize(1024 * 1024 * 10);
        dialogUploadComponent.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);
        Button uploadButton = new Button(getUploadButtonName(fieldProfileUseCamera.getValue()));
        dialogUploadComponent.setUploadButton(uploadButton);
        
        fieldProfileUseCamera.addValueChangeListener(e -> {
            if(e.getValue()){
                dialogUploadComponent.getElement().setAttribute("capture", "environment");
            }else{
                dialogUploadComponent.getElement().removeAttribute("capture");
            }
            uploadButton.setText(getUploadButtonName(e.getValue()));
        });

        if(fieldProfileUseCamera.getValue()){
            dialogUploadComponent.getElement().setAttribute("capture", "environment");
            uploadButton.setText(getUploadButtonName(fieldProfileUseCamera.getValue()));
        }
        
        dialogProfileImageRotateButton.addClickListener(click -> {
            try {
                profileImageData = getRotatedImage(profileImageData);
                String src = getImageAsBase64(profileImageData, profileImageMimeType);
                fieldProfileAvatar.setImage(src);
                fieldProfileAvatar.setName(fieldProfileAvatar.getName() + "rcw-");
                profileAvatarHasChanges = Boolean.TRUE;
                validateProfileAvatar();
            } catch (Exception ex) {
                System.getLogger(StockEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        });

        //error handler
        dialogUploadComponent.addFileRejectedListener(event -> {
          String errorMessage = event.getErrorMessage();
          Notification notification =
              Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
          notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
          profileAvatarHasChanges = Boolean.FALSE;
          validateProfileAvatar();
        });

        fieldProfileUseCamera.setValue(false);
        
        Button getCropButton = new Button("Crop Image");

        getCropButton.addClickListener(e -> {
            openCropDialog(profileImageData, profileImageMimeType);
            //profileAvatarHasChanges = Boolean.TRUE;
        });
        
        HorizontalLayout cropAndRotate = UIUtilities.getHorizontalLayout(true,true,false);
        cropAndRotate.add(getCropButton,dialogProfileImageRotateButton);

        uploadLayout.add(dialogUploadComponent, fieldProfileUseCamera, cropAndRotate);
        return uploadLayout;
    }   
    
    private byte[] getResizedImage(byte[] imageBytes, int targetWidth) throws Exception {
        // convert byte[] back to a BufferedImage
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage newBi = ImageIO.read(is);
        
        //System.out.println("Scale: BEFORE" + newBi.getWidth() + "x" + newBi.getHeight());
        
        BufferedImage resizedBi = Scalr.resize(newBi, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth, Scalr.OP_ANTIALIAS);   
        //System.out.println("Scale: AFTER" + resizedBi.getWidth() + "x" + resizedBi.getHeight());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedBi, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        return bytes;
    } 
    
    private byte[] getRotatedImage(byte[] imageBytes) throws Exception {
        // convert byte[] back to a BufferedImage
        //System.out.println("Rotate: imageBytes size:" + imageBytes.length);
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage newBi = ImageIO.read(is);
        //System.out.println("Rotate: newBi BEFORE: width:" + newBi.getWidth() + " height:" + newBi.getHeight());
        
        //System.out.println("Rotate: BEFORE:" + newBi.getWidth() + "x" + newBi.getHeight());
        newBi = Scalr.rotate(newBi, Scalr.Rotation.CW_90, Scalr.OP_ANTIALIAS);
        //System.out.println("Rotate: AFTER:" + newBi.getWidth() + "x" + newBi.getHeight());
        //System.out.println("Rotate: newBi AFTER: width:" + newBi.getWidth() + " height:" + newBi.getHeight());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(newBi, "png", baos); //changed to png format as jpg was failing for images with same W as H
        byte[] bytes = baos.toByteArray();
        //System.out.println("Rotate: bytes size:" + bytes.length);
        return bytes;
    } 
    
    private String getUploadButtonName(Boolean useCamera){
        if(useCamera){
            return "Capture";
        }else{
            return "Upload";
        }
    }
    
    private void openCropDialog(byte[] outputStream, String mimeType) {
      // Set up image crop dialog
      Dialog cropDialog = new Dialog();
            cropDialog.setCloseOnEsc(true);
      cropDialog.setCloseOnOutsideClick(false);
      cropDialog.setMaxHeight("100%");
      cropDialog.setMaxWidth(cropDialog.getHeight());

      Button cropButton = new Button("Crop image");
      Button cropDialogCancelButton = new Button("Cancel");
      cropDialogCancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

      String src = getImageAsBase64(outputStream, mimeType);
      fieldProfileImageCrop = new ImageCrop(src);
      fieldProfileImageCrop.setAspect(1.0);
      fieldProfileImageCrop.setCircularCrop(true);
      fieldProfileImageCrop.setCrop(new Crop("%", 25, 25, 50, 50)); // centered crop
      fieldProfileImageCrop.setKeepSelection(true);

      cropDialogCancelButton.addClickListener(c -> {
          cropDialog.close();
      });

      cropButton.addClickListener(event -> {
          fieldProfileAvatar.setImage(fieldProfileImageCrop.getCroppedImageDataUri());
          fieldProfileAvatar.setName(fieldProfileImageCrop.getImageSrc());
          profileImageData = fieldProfileImageCrop.getCroppedImageBase64();
          profileAvatarHasChanges = Boolean.TRUE;
          validateProfileAvatar();
          cropDialog.close();
      });

    HorizontalLayout buttonLayout = new HorizontalLayout(cropButton, cropDialogCancelButton);
      Div cropDialogLayout = new Div(fieldProfileImageCrop);
      cropDialogLayout.setSizeFull();
      buttonLayout.setWidthFull();
      buttonLayout.setJustifyContentMode(JustifyContentMode.END);
      cropDialog.add(cropDialogLayout);
      cropDialog.getFooter().add(buttonLayout);
      cropDialog.open();
    }  
    
    private byte[] getByteArrayFromImageFile(String filePath){
        //System.out.println("StockEditor:getByteArrayFromImageFile:" + filePath);
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException ex) {
            System.getLogger(StockEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException ex) {
            System.getLogger(StockEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        byte[] imageBytes = baos.toByteArray();   
        return imageBytes;
    }
    
    private String getImageAsBase64(byte[] src, String mimeType) {
    return src != null ? "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(src)
        : null;
    }    

    private void clearLists(){
        stockList.clear();
        litterList.clear();
    }
    
    public FormLayout showItem(Stock currentStock, DisplayMode currentDisplayMode){
        FormLayout stockFormLayout = new FormLayout();
        stockFormLayout.setWidthFull();
        stockFormLayout.setAutoResponsive(false);
        stockFormLayout.setExpandColumns(true);
        stockFormLayout.setExpandFields(true);
        stockFormLayout.setMaxColumns(3);
        stockFormLayout.setMinColumns(1);

        //fields that are always ReadOnly
        fieldStatus.setReadOnly(true);
        fieldGenotype.setReadOnly(true);
        
        if(isNewStock){
            fieldWeight.setReadOnly(false);
        }else{
            fieldWeight.setReadOnly(true); 
        }
        
        stockFormLayout.setResponsiveSteps(
        // Use one column by default
        new ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use two columns, if the layout's width exceeds 320px
        new ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        stockFormLayout.removeAll();

        //add the header
        stockFormLayout.add(currentStock.getStockHeader(Boolean.FALSE));

        //fields by displayMode type
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            stockFormLayout.addFormItem(fieldExternal,"External");
            stockFormLayout.addFormItem(fieldPrefix,"Prefix");
            stockFormLayout.addFormItem(fieldName,"Name");
            stockFormLayout.addFormItem(fieldTattoo,"Tattoo/ID");
            stockFormLayout.addFormItem(fieldBreed,"Breed");
            stockFormLayout.addFormItem(fieldGender,"Gender");
            stockFormLayout.addFormItem(fieldBreeder,"Breeder");
            stockFormLayout.addFormItem(fieldColor,"Color");
            stockFormLayout.addFormItem(fieldWeight,"Weight");
            stockFormLayout.addFormItem(fieldFather,"Father");
            stockFormLayout.addFormItem(fieldMother,"Mother");
            stockFormLayout.addFormItem(fieldGenotypeLayout,"Genotype");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldLegs,"Legs");
            stockFormLayout.addFormItem(fieldChampNo,"Championship Number");
            stockFormLayout.addFormItem(fieldRegNo,"Registration Number");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            fieldStatusFormItem = stockFormLayout.addFormItem(fieldStatusLayout,"Status");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            fieldAquiredFormItem = stockFormLayout.addFormItem(fieldAquiredDate,"Aquired");
            stockFormLayout.addFormItem(fieldBornDate,"Born");
            fieldFosterFormItem = stockFormLayout.addFormItem(fieldFoster,"Foster");
            fieldSaleStatusFormItem = stockFormLayout.addFormItem(fieldSaleStatusLayout, "Sale Status");
            fieldValueFormItem = stockFormLayout.addFormItem(fieldValue, "Value");
            fieldInvoiceFormItem = stockFormLayout.addFormItem(fieldInvoiceNumber, "Invoice #");
            stockFormLayout.addFormItem(fieldNotes,"Notes");
            
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            stockFormLayout.add(avatarDiv);
        }
        
        //set values
        setValues(currentStock, currentDisplayMode);
        return stockFormLayout;
    }
    
    private void toggleHiddenFormFields(Boolean show){
        if(fieldStatusFormItem != null) fieldStatusFormItem.setVisible(show);
        if(fieldFosterFormItem != null) fieldFosterFormItem.setVisible(show);
        if(fieldAquiredFormItem != null) fieldAquiredFormItem.setVisible(show);
    }

    private void toggleSaleFormFields(Boolean show){
        if(fieldSaleStatusFormItem != null) fieldSaleStatusFormItem.setVisible(show);
        if(fieldValueFormItem != null) fieldValueFormItem.setVisible(show);
        if(fieldInvoiceFormItem != null) fieldInvoiceFormItem.setVisible(show);
    }

    private void setValues(Stock currentStock, DisplayMode currentDisplayMode){

        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            
            fieldGenotype.setTooltipText(currentStock.getGenotype());

            fieldGender.setRenderer(new TextRenderer<>(gender -> {
                if(gender.equals(Gender.MALE)) return currentStock.getStockType().getMaleName();
                else if(gender.equals(Gender.FEMALE)) return currentStock.getStockType().getFemaleName();
                return "NA";
            }));        

            fieldFather.setItemLabelGenerator(Stock::getDisplayName);
            fieldMother.setItemLabelGenerator(Stock::getDisplayName);
            fieldFoster.setItems(litterService.getActiveLitters(currentStock.getStockType()));
            fieldFoster.setItemLabelGenerator(litter -> {
                if(litter == null) return NOT_FOSTERED;
                return litter.getDisplayName();
            });

            //retrieve the stock entity from the database
            if(currentStock.getId()==null){
                this.stockEntity = currentStock;
            }else{
                this.stockEntity = stockService.findById(currentStock.getId());
            }

                boolean lockExternalParentFieldsForThisDialog = lockGenderSelection
                    && Boolean.TRUE.equals(this.stockEntity.getExternal());
                fieldGender.setReadOnly(lockExternalParentFieldsForThisDialog);
                fieldExternal.setReadOnly(lockExternalParentFieldsForThisDialog);
                fieldBreeder.setReadOnly(lockExternalParentFieldsForThisDialog);

            boolean hasGenotypeConfig = this.stockEntity.getStockType() != null
                    && !this.stockEntity.getStockType().getGenotypes().isEmpty();
            fieldGenotypeEditButton.setEnabled(hasGenotypeConfig);
            fieldGenotypeEditButton.setVisible(hasGenotypeConfig);

                boolean canEditStatus = this.stockEntity.getId() != null;
                fieldStatusEditButton.setEnabled(canEditStatus);
                fieldStatusEditButton.setVisible(canEditStatus);
                fieldSaleStatusEditButton.setEnabled(canEditStatus);
                fieldSaleStatusEditButton.setVisible(canEditStatus);

            System.out.println("setValues: currentStock: fatherid:" + currentStock.getFatherId());
            System.out.println("setValues: stockEntity: fatherid:" + stockEntity.getFatherId());

            fieldFather.setItems(stockService.getFathers(currentStock.getFatherExtName(),currentStock.getStockType()));
            binder.forField(fieldFather)
                    .withConverter(new ParentIntegerToStockConverter(stockEntity, Gender.MALE))
                    //.withDefaultValidator(false)
                    .bind(Stock::getFatherId, Stock::setFatherId);
            fieldMother.setItems(stockService.getMothers(currentStock.getMotherExtName(),currentStock.getStockType()));
            binder.forField(fieldMother)
                    .withConverter(new ParentIntegerToStockConverter(stockEntity, Gender.FEMALE))
                    //.withDefaultValidator(false)
                    .bind(Stock::getMotherId, Stock::setMotherId);

            binder.forField(fieldStatus)
                    .withConverter(new StatusHistoryConverter(this.stockEntity))
                    .bindReadOnly(Stock::getStatus);

            Binder.Binding<Stock, String> nameBinding = binder.forField(fieldName)
                    .withValidator(
                            name -> name.length() + fieldTattoo.getValue().length() > 0,
                            "Either Name or ID field must be filled in.")
                    .bind(Stock::getName, Stock::setName);

            if (tattooChangedReg != null) tattooChangedReg.remove();
            tattooChangedReg = fieldTattoo.addValueChangeListener(event -> nameBinding.validate());

            Binder.Binding<Stock, String> tattooBinding = binder.forField(fieldTattoo)
                    .withValidator(
                            tattoo -> tattoo.length() + fieldName.getValue().length() > 0,
                            "Either Name or ID field must be filled in.")
                    .bind(Stock::getTattoo, Stock::setTattoo);

            if (nameChangedReg != null) nameChangedReg.remove();
            nameChangedReg = fieldName.addValueChangeListener(event -> tattooBinding.validate());

            binder.bindInstanceFields(this);
            binder.readBean(this.stockEntity);

            boolean showNonExternalFields = !Boolean.TRUE.equals(this.stockEntity.getExternal());
            toggleHiddenFormFields(showNonExternalFields);
            toggleSaleFormFields(showNonExternalFields);

            syncStatusFieldsFromStock();
            binder.validate(); //force validation so warnings are displayed on form load

            binder.addStatusChangeListener(listener -> {
                boolean isValid = !listener.hasValidationErrors();
                boolean hasChanges = listener.getBinder().hasChanges();
                System.out.println("addStatusChangeListener: isValid:" + isValid + " hasChanges:" + hasChanges);
                dialogOkButton.setEnabled(hasChanges && isValid);
                dialogResetButton.setEnabled(hasChanges);
            });

        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            if(currentStock.getProfileFile()==null){
                fieldProfileAvatar.setImage("images/default-profile.png");
                fieldProfileAvatar.setName("default-profile.png");
                profileImageData = null;
                profileImageMimeType = null;
                profileAvatarHasChanges = Boolean.FALSE;
                validateProfileAvatar();
                return;
            }

            fieldProfileAvatar.setImageHandler(DownloadHandler.forFile(currentStock.getProfileFile()));
            fieldProfileAvatar.setName(currentStock.getProfileImage());
            profileImageData = getByteArrayFromImageFile(currentStock.getProfileFile().getAbsolutePath());
            profileImageMimeType = "image/*";
            profileAvatarHasChanges = Boolean.FALSE;
            validateProfileAvatar();
        }

    }

    public void addListener(ListRefreshNeededListener listener){
        listRefreshNeededListeners.add(listener);
    }

    private GenotypeEditor getGenotypeEditor() {
        if (genotypeEditor == null) {
            genotypeEditor = new GenotypeEditor();
        }
        return genotypeEditor;
    }

    private StatusEditor getStatusEditor() {
        if (statusEditor == null) {
            statusEditor = new StatusEditor();
        }
        return statusEditor;
    }

    private void openGenotypeEditor() {
        if (stockEntity == null || stockEntity.getStockType() == null || stockEntity.getStockType().getGenotypes().isEmpty()) {
            return;
        }
        getGenotypeEditor().dialogOpen(stockEntity, false, this::syncGenotypeFieldFromStock);
    }

    private void syncGenotypeFieldFromStock() {
        if (stockEntity == null) {
            return;
        }
        String genotypeValue = stockEntity.getGenotype() == null ? "" : stockEntity.getGenotype();
        fieldGenotype.setValue(genotypeValue);
        fieldGenotype.setTooltipText(genotypeValue);
    }

    private void openStatusEditor() {
        if (stockEntity == null || stockEntity.getId() == null) {
            return;
        }
        getStatusEditor().dialogOpen(stockEntity, this::syncStatusFieldsFromStock);
    }

    private void openSaleStatusEditor() {
        if (stockEntity == null || stockEntity.getId() == null) {
            return;
        }
        List<String> saleStatuses = new ArrayList<>(List.of("listed", "deposit", "sold"));
        if (stockEntity.getSaleStatus() != StockSaleStatus.NONE) {
            saleStatuses.add("active");
        }
        getStatusEditor().dialogOpen(stockEntity, saleStatuses, this::syncStatusFieldsFromStock);
    }

    private void syncStatusFieldsFromStock() {
        if (stockEntity == null) {
            return;
        }

        String statusName = stockEntity.getEffectiveStatusKey();
        String statusDisplay = statusName;
        if (statusName != null && Utility.getInstance().hasStockStatus(statusName)) {
            StockStatus status = Utility.getInstance().getStockStatus(statusName);
            statusDisplay = status != null ? status.getLongName() : statusName;
        }
        fieldStatus.setValue(statusDisplay == null ? "" : statusDisplay);

        fieldValue.setValue(stockEntity.getStockValue());
        fieldInvoiceNumber.setValue(stockEntity.getInvoiceNumber());
        String saleStatusDisplay = stockEntity.getSaleStatus() == StockSaleStatus.NONE
            ? "Not for sale"
            : stockEntity.getSaleStatus().getShortName();
        fieldSaleStatus.setValue(saleStatusDisplay);
    }

    private void notifyRefreshNeeded(){
        for (ListRefreshNeededListener listener: listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public Integer getReturnId() {
        return returnId;
    }

    public void setReturnId(Integer returnId) {
        this.returnId = returnId;
    }

    public Stock getReturnStock() {
        return returnStock;
    }

    public void setReturnStock(Stock returnStock) {
        this.returnStock = returnStock;
    }
    
    public Dialog getDialog(){
        return dialog;
    }


    
}
