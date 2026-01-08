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
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.ParentIntegerToStockConverter;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StatusHistoryConverter;
import ca.jusjoken.data.service.StockService;
import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.PropertyId;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;

public class DialogCommon {

    public enum DialogMode{
        EDIT, DELETE, VIEW
    }

    private Binder<Stock> binder;
    private static final String notFostered = "Not fostered";

    //default is EDIT - must be superUser to allow DELETE
    private DialogMode dialogMode = DialogMode.VIEW;

    public enum DisplayMode{
        LITTER_LIST, KIT_LIST, STOCK_DETAILS, PROFILE_IMAGE
    }
    //private DisplayMode displayMode = DisplayMode.PROFILE_IMAGE;

    private Logger log = LoggerFactory.getLogger(DialogCommon.class);
    private Dialog dialog = new Dialog();
    private Long taskID = 0L;
    private Stock stockEntity;
    private String dialogTitle = "";

    private List<Stock> stockList = new ArrayList<>();
    private List<Litter> litterList = new ArrayList<>();
    
    private Button dialogResetButton = new Button("Reset");
    private Button dialogOkButton = new Button("OK");
    private Button dialogCancelButton = new Button("Cancel");
    private Button dialogCloseButton = new Button(new Icon("lumo", "cross"));

    private Upload dialogUploadComponent;

    private static final String[] ACCEPTED_MIME_TYPES =
      {"image/gif", "image/png", "image/jpeg", "image/bmp", "image/webp"};
    private byte[] profileImageData = null;
    private String profileImageMimeType = null;

    //Fields defined here
    //global fields
    private Avatar fieldProfileAvatar = new Avatar();
    private Div avatarDiv = new Div(fieldProfileAvatar);    
    private ImageCrop fieldProfileImageCrop = new ImageCrop(fieldProfileAvatar.getImage());
    private Checkbox fieldProfileUseCamera = new Checkbox("Use camera if available");
    private Button dialogProfileImageRotateButton = new Button("Rotate");
    private Boolean profileAvatarHasChanges = Boolean.FALSE;


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
    private WeightInput fieldWeight = new WeightInput();
    
    @PropertyId("acquired")
    private DatePicker fieldAquiredDate = new DatePicker();
    @PropertyId("doB")
    private DatePicker fieldBornDate = new DatePicker();
    @PropertyId("legs")
    private TextField fieldLegs = UIUtilities.getTextField();
    @PropertyId("champNo")
    private TextField fieldChampNo = UIUtilities.getTextField();
    @PropertyId("regNo")
    private TextField fieldRegNo = UIUtilities.getTextField();

    @PropertyId("fatherId")
    private ComboBox<Stock> fieldFather = new ComboBox();
    @PropertyId("motherId")
    private ComboBox<Stock> fieldMother = new ComboBox();


    @PropertyId("genotype")
    private TextField fieldGenotype = UIUtilities.getTextField();
    @PropertyId("category")
    private TextField fieldCategory = UIUtilities.getTextField(); //TODO - needs to be a pickbox
    
    @PropertyId("status")
    private TextField fieldStatus = new TextField();

    @PropertyId("fosterLitter")
    private Select<Litter> fieldFoster = new Select();


    private TextArea fieldNotes = new TextArea();

    private VerticalLayout dialogLayout = new VerticalLayout();

    private Boolean hasChangedValues = Boolean.FALSE;
    private StockService stockService;
    private LitterService litterService;
    private DisplayMode openedDisplayMode = DisplayMode.STOCK_DETAILS;

    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    private String profileImagePath;
    
    public DialogCommon() {
        this(DisplayMode.STOCK_DETAILS);
    }

    public DialogCommon(DisplayMode currentDisplayMode) {
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
        profileImagePath = System.getenv("PROFILE_IMAGE_PATH");
        dialogConfigure(currentDisplayMode);
    }


    private void dialogConfigure(DisplayMode currentDisplayMode) {

        binder = new Binder<Stock>(Stock.class);
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

        dialogResetButton.setEnabled(false);
        dialogResetButton.addClickListener(
                event -> {
                    dialogReset(openedDisplayMode);
                }
        );

        HorizontalLayout footerLayout = new HorizontalLayout(dialogOkButton,dialogCancelButton,dialogResetButton);

        // Prevent click shortcut of the OK button from also triggering when another button is focused
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //one time configuration for any fields
        fieldGender.setItems(Gender.MALE, Gender.FEMALE);
        if(dialogMode.equals(DialogMode.EDIT)){
            fieldMother.setAllowCustomValue(true);
            fieldFather.setAllowCustomValue(true);
        }
        fieldFoster.setEmptySelectionAllowed(true);
        fieldFoster.setEmptySelectionCaption(notFostered);
        
        //form fields need width full to fill the column they are in
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
        fieldCategory.setWidthFull();
        fieldStatus.setWidthFull();
        fieldFoster.setWidthFull();
        fieldAquiredDate.setWidthFull();
        fieldBornDate.setWidthFull();
        fieldChampNo.setWidthFull();
        fieldGenotype.setWidthFull();
        fieldLegs.setWidthFull();
        fieldRegNo.setWidthFull();

        fieldFather.addCustomValueSetListener(item -> {
            //add the custom value to the list and set as value
            fieldFather.setItems(stockService.getFathers(item.getDetail(), stockEntity.getStockType()));
            fieldFather.setValue(stockService.getParentExt(item.getDetail(), stockEntity.getStockType()));
            System.out.println("**Father custom changed to:" + item.getDetail() + " getValue =" + item.getSource().getValue());
        });
        fieldMother.addCustomValueSetListener(item -> {
            //add the custom value to the list and set as value
            fieldMother.setItems(stockService.getMothers(item.getDetail(), stockEntity.getStockType()));
            fieldMother.setValue(stockService.getParentExt(item.getDetail(), stockEntity.getStockType()));
            System.out.println("**Mother custom changed to:" + item.getDetail() + " getValue =" + item.getSource().getValue());
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
        DisplayMode currentDisplayMode = openedDisplayMode;
        System.out.println("dialogSave: stockEntity start save:" + stockEntity);
        //save here
        
        //retrieve the stock entity from the database before saving so it is the same entity
        this.stockEntity = stockService.findById(stockEntity.getId());
        
        //update stockEntity from fields
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            //Handle user adding a parent manually
            if(fieldFather!=null && fieldFather.getValue()!=null && fieldFather.getValue().isTemp()){
                this.stockEntity.setFatherExtName(fieldFather.getValue().getDisplayName());
                this.stockEntity.setFatherId(null);
            }else{
                if(fieldFather.getValue()!=null){
                    this.stockEntity.setFatherId(fieldFather.getValue().getId());
                }
            }
            if(fieldMother!=null && fieldMother.getValue()!=null && fieldMother.getValue().isTemp()){
                this.stockEntity.setMotherExtName(fieldMother.getValue().getDisplayName());
                this.stockEntity.setMotherId(null);
            }else{
                if(fieldMother.getValue()!=null){
                    this.stockEntity.setMotherId(fieldMother.getValue().getId());
                }
            }
            //write all bound fields back to the entity
            try {
                binder.writeBean(stockEntity);
            } catch (ValidationException ex) {
                System.out.println("dialogSave: Validation Error writing bean stockEntity:" + ex);
            }

            System.out.println("dialogSave: stockEntity before save:" + stockEntity);
            stockService.save(this.stockEntity);
            System.out.println("dialogSave: stockEntity after save:" + stockEntity);
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            //write the profile image to file
            try (OutputStream outputStream = Files.newOutputStream(stockEntity.getProfileFileToBeSaved().toPath())) {
                outputStream.write(profileImageData);
                profileAvatarHasChanges = Boolean.FALSE;
                validateProfileAvatar();
            } catch (IOException e) {
                // Handle exception
                e.printStackTrace();
            }            
        }

        //refresh if needed
        log.info("dialogSave: notifying listeners");
        dialog.close();
        notifyRefreshNeeded();
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
        this.stockEntity = stockEntity;
        
        openedDisplayMode = currentDisplayMode;
        //set values and visibility for fields
        clearLists();
        dialogLayout.removeAll();
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            //binder.readBean(this.stockEntity);
            dialogLayout.add(showItem(this.stockEntity, currentDisplayMode));
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            profileAvatarHasChanges = Boolean.FALSE;
            validateProfileAvatar();
            
            //load stored image or if none load default image
            fieldProfileAvatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
            fieldProfileAvatar.setHeight("12em");
            fieldProfileAvatar.setWidth("12em");

            fieldProfileAvatar.setImageHandler(DownloadHandler.forFile(this.stockEntity.getProfileFile()));
            fieldProfileAvatar.setName(this.stockEntity.getProfileImage());
            profileImageData = getByteArrayFromImageFile(this.stockEntity.getProfileFile().getAbsolutePath());
            profileImageMimeType = "image/*";
            //dialogLayout.add(getStockHeader(stockEntity, false), avatarDiv, createImageUploadLayout());
            dialogLayout.add(showItem(this.stockEntity,currentDisplayMode), createImageUploadLayout(currentDisplayMode));
        }else{
            System.out.println("dialogOpen: dialogMode:" + dialogMode + " NOTHING");
            //nothing
        }

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialog.setDraggable(true);
        dialog.setResizable(true);
        //dialog.addClassNames("backdrop-blur-none");

        dialog.open();
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
    
    private VerticalLayout createImageUploadLayout(DisplayMode currentDisplayMode) {
        VerticalLayout uploadLayout = UIUtilities.getVerticalLayout();

        InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory(
            (metadata, data) -> {
                byte[] newData = null;
                try {
                    // Get other information about the file.
                    newData = getResizedImage(data, 600);
                } catch (Exception ex) {
                    System.getLogger(DialogCommon.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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
                System.getLogger(DialogCommon.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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
            openCropDialog(profileImageData, profileImageMimeType, currentDisplayMode);
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
        boolean result = ImageIO.write(newBi, "png", baos); //changed to png format as jpg was failing for images with same W as H
        //System.out.println("Rotate: wrtie result:" + result);
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
    
    private void openCropDialog(byte[] outputStream, String mimeType, DisplayMode currentDisplayMode) {
      // Set up image crop dialog
      Dialog cropDialog = new Dialog();
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

      HorizontalLayout buttonLayout = new HorizontalLayout(cropDialogCancelButton, cropButton);
      Div cropDialogLayout = new Div(fieldProfileImageCrop);
      cropDialogLayout.setSizeFull();
      buttonLayout.setWidthFull();
      buttonLayout.setJustifyContentMode(JustifyContentMode.END);
      cropDialog.add(cropDialogLayout);
      cropDialog.getFooter().add(buttonLayout);
      cropDialog.open();
    }  
    
    private byte[] getByteArrayFromImageFile(String filePath){
        //System.out.println("DialogCommon:getByteArrayFromImageFile:" + filePath);
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filePath));
        } catch (IOException ex) {
            System.getLogger(DialogCommon.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException ex) {
            System.getLogger(DialogCommon.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
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
    
    public DialogMode getDialogMode() {
        return dialogMode;
    }

    public void setDialogMode(DialogMode dialogMode) {
        this.dialogMode = dialogMode;
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
        
        stockFormLayout.setResponsiveSteps(
        // Use one column by default
        new ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
        // Use two columns, if the layout's width exceeds 320px
        new ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        stockFormLayout.removeAll();

        //add the header
        stockFormLayout.add(currentStock.getStockHeader(Boolean.FALSE));

        //fields by displayMode type
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            stockFormLayout.addFormItem(fieldName,"Name");
            stockFormLayout.addFormItem(fieldPrefix,"Prefix");
            stockFormLayout.addFormItem(fieldTattoo,"Tattoo");
            stockFormLayout.addFormItem(fieldBreed,"Breed");
            stockFormLayout.addFormItem(fieldGender,"Gender");
            stockFormLayout.addFormItem(fieldBreeder,"Breeder");
            stockFormLayout.addFormItem(fieldColor,"Color");
            stockFormLayout.addFormItem(fieldWeight,"Weight");
            stockFormLayout.addFormItem(fieldFather,"Father");
            stockFormLayout.addFormItem(fieldMother,"Mother");
            stockFormLayout.addFormItem(fieldGenotype,"Genotype");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldLegs,"Legs");
            stockFormLayout.addFormItem(fieldChampNo,"Championship Number");
            stockFormLayout.addFormItem(fieldRegNo,"Registration Number");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldStatus,"Status");
            stockFormLayout.addFormItem(fieldCategory,"Category");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldAquiredDate,"Aquired");
            stockFormLayout.addFormItem(fieldBornDate,"Born");
            if(dialogMode.equals(DialogMode.EDIT)){
                stockFormLayout.addFormItem(fieldFoster,"Foster");
            }
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            stockFormLayout.add(avatarDiv);
        }
        
        //set values
        setValues(currentStock, currentDisplayMode);
        return stockFormLayout;
    }

    private void setValues(Stock currentStock, DisplayMode currentDisplayMode){

        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            if(dialogMode.equals(DialogMode.EDIT)){
                fieldGender.setRenderer(new TextRenderer<Gender>(gender -> {
                    if(gender.equals(Gender.MALE)) return currentStock.getStockType().getMaleName();
                    return currentStock.getStockType().getFemaleName();
                }));        
                
                fieldFather.setItemLabelGenerator(Stock::getDisplayName);
                fieldMother.setItemLabelGenerator(Stock::getDisplayName);
                fieldFoster.setItems(litterService.getActiveLitters());
                fieldFoster.setItemLabelGenerator(litter -> {
                    if(litter == null) return notFostered;
                    return litter.getDisplayName();
                });

                //retrieve the stock entity from the database
                this.stockEntity = stockService.findById(currentStock.getId());

                fieldFather.setItems(stockService.getFathers(currentStock.getFatherExtName(),currentStock.getStockType()));
                binder.forField(fieldFather)
                        .withConverter(new ParentIntegerToStockConverter(stockEntity, Gender.MALE))
                        .bind(Stock::getFatherId, Stock::setFatherId);
                fieldMother.setItems(stockService.getMothers(currentStock.getMotherExtName(),currentStock.getStockType()));
                binder.forField(fieldMother)
                        .withConverter(new ParentIntegerToStockConverter(stockEntity, Gender.FEMALE))
                        .bind(Stock::getMotherId, Stock::setMotherId);

                binder.forField(fieldStatus)
                        .withConverter(new StatusHistoryConverter(this.stockEntity))
                        .bindReadOnly(Stock::getStatus);

                binder.bindInstanceFields(this);
                binder.readBean(this.stockEntity);

                binder.addStatusChangeListener(listener -> {
                    boolean isValid = listener.getBinder().isValid();
                    boolean hasChanges = listener.getBinder().hasChanges();
                    System.out.println("addStatusChangeListener: isValid:" + isValid + " hasChanges:" + hasChanges);

                    dialogOkButton.setEnabled(hasChanges && isValid);
                    dialogResetButton.setEnabled(hasChanges);
                });

            }
            
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
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
    
}
