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
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
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

    //default is EDIT - must be superUser to allow DELETE
    private DialogMode dialogMode = DialogMode.VIEW;

    public enum DisplayMode{
        LITTER_LIST, KIT_LIST, STOCK_DETAILS, PROFILE_IMAGE
    }
    //private DisplayMode displayMode = DisplayMode.PROFILE_IMAGE;

    private Boolean superUser = Boolean.FALSE;
    private Boolean validationEnabled = Boolean.FALSE;
    private Logger log = LoggerFactory.getLogger(DialogCommon.class);
    private Dialog dialog = new Dialog();
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

    private Checkbox fieldBreeder = new Checkbox();
    private RadioButtonGroupEx<Gender> fieldGender = new RadioButtonGroupEx<>();
    private TextField fieldPrefix = new TextField();
    private TextField fieldName = new TextField();
    private TextField fieldTattoo = new TextField();
    private TextField fieldColor = new TextField();
    private TextField fieldBreed = new TextField();
    private WeightInput fieldWeight = new WeightInput();
    
    private DatePicker fieldAquiredDate = new DatePicker();
    private DatePicker fieldBornDate = new DatePicker();
    private TextField fieldLegs = UIUtilities.getTextField();
    private TextField fieldChampNo = UIUtilities.getTextField();
    private TextField fieldRegNo = UIUtilities.getTextField();
    private TextField fieldFatherName = new TextField();
    private TextField fieldMotherName = new TextField();
    private ComboBox<Stock> fieldFather = new ComboBox();
    private ComboBox<Stock> fieldMother = new ComboBox();
    private TextField fieldGenotype = UIUtilities.getTextField();
    private TextField fieldCategory = UIUtilities.getTextField(); //TODO - needs to be a pickbox
    private TextField fieldStatus = UIUtilities.getTextField(); //TODO - needs to be a pickbox
    private DatePicker fieldStatusDate = new DatePicker();
    private TextField fieldFoster = UIUtilities.getTextField(); //TODO - needs to figure this out
    
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
                    dialogSave(currentDisplayMode);
                }
        );
        dialogOkButton.addClickShortcut(Key.ENTER);
        dialogOkButton.setEnabled(false);
        dialogOkButton.setDisableOnClick(true);

        dialogResetButton.addClickListener(
                event -> {
                    dialogOkButton.setEnabled(false);
                    setValues(this.stockEntity, currentDisplayMode);
                    dialogValidate(currentDisplayMode);
                    dialogUploadComponent.clearFileList();
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
        fieldFatherName.setWidthFull();
        fieldMotherName.setWidthFull();
        fieldCategory.setWidthFull();
        fieldStatus.setWidthFull();
        fieldStatusDate.setWidthFull();
        fieldFoster.setWidthFull();
        fieldAquiredDate.setWidthFull();
        fieldBornDate.setWidthFull();
        fieldChampNo.setWidthFull();
        fieldGenotype.setWidthFull();
        fieldLegs.setWidthFull();
        fieldRegNo.setWidthFull();

        //add change listeners for each editable field
        fieldBreeder.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldGender.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldPrefix.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldName.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldTattoo.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldColor.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldBreed.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldWeight.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldAquiredDate.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldBornDate.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldLegs.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldChampNo.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldRegNo.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldFather.addValueChangeListener(item -> {
            System.out.println("**Father value changed to:" + item.getValue());
            dialogValidate(currentDisplayMode);
        });
        fieldFather.addCustomValueSetListener(item -> {
            System.out.println("**Father custom changed to:" + item.getDetail());
            //TODO:: set the NEW field in Stock for FatherExternal and null FatherId
        });
        fieldMother.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldMother.addCustomValueSetListener(item -> {
            System.out.println("**Mother custom changed to:" + item.getDetail());
            //TODO:: set the NEW field in Stock for MotherExternal and null MotherId
        });
        fieldGenotype.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldCategory.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldStatus.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldStatusDate.addValueChangeListener(item -> dialogValidate(currentDisplayMode));
        fieldFoster.addValueChangeListener(item -> dialogValidate(currentDisplayMode));

    }

    private void dialogClose(){
//        if(customTaskConverted){
//            log.info("dialogClose: reloading stockEntity");
//            List<Stock> stockEntityList = stockService.findById(this.stockEntity.getId());
//            log.info("dialogClose: reloading stockEntity:" + stockEntityList);
//            if(stockEntityList!=null && stockEntityList.size()>0){
//                log.info("dialogClose: reseting to stockEntity:" + stockEntityList.get(0));
//                this.stockEntity = stockEntityList.get(0);
//            }
//        }
        dialog.close();
    }

    private void dialogSave(DisplayMode currentDisplayMode) {
        //save here
        //update stockEntity from fields
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            //this.stockEntity.setGlobalSubtotal(fieldGlobalSubTotal.getValue());
            //this.stockEntity.setGlobalTotalTaxes(fieldGlobalTaxes.getNumberField().getValue());
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            //write the profile image to file
            try (OutputStream outputStream = Files.newOutputStream(stockEntity.getProfileFileToBeSaved().toPath())) {
                outputStream.write(profileImageData);
            } catch (IOException e) {
                // Handle exception
                e.printStackTrace();
            }            
        }else{
            //this.stockEntity.setWebOrder(fieldWebOrder.getValue());
            //this.stockEntity.setReceiptTotal(fieldReceiptTotal.getValue());
        }

        stockService.save(this.stockEntity);
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
        System.out.println("dialogOpen: dialogMode:" + dialogMode);
        //set values and visibility for fields
        clearLists();
        dialogLayout.removeAll();
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            dialogLayout.add(showItem(this.stockEntity, currentDisplayMode, false, false));
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
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
            //nothing
        }

        customTaskConverted = Boolean.FALSE;

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialogValidate(currentDisplayMode);
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        //dialog.addClassNames("backdrop-blur-none");

        dialog.open();
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
                dialogValidate(currentDisplayMode);
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
                dialogValidate(currentDisplayMode);
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
          dialogValidate(currentDisplayMode);
        });

        fieldProfileUseCamera.setValue(false);
        
        Button getCropButton = new Button("Crop Image");

        getCropButton.addClickListener(e -> {
            openCropDialog(profileImageData, profileImageMimeType, currentDisplayMode);
            dialogValidate(currentDisplayMode);
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
        
        System.out.println("Scale: BEFORE" + newBi.getWidth() + "x" + newBi.getHeight());
        
        BufferedImage resizedBi = Scalr.resize(newBi, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, targetWidth, Scalr.OP_ANTIALIAS);   
        System.out.println("Scale: AFTER" + resizedBi.getWidth() + "x" + resizedBi.getHeight());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedBi, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        return bytes;
    } 
    
    private byte[] getRotatedImage(byte[] imageBytes) throws Exception {
        // convert byte[] back to a BufferedImage
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage newBi = ImageIO.read(is);
        
        System.out.println("Rotate: BEFORE:" + newBi.getWidth() + "x" + newBi.getHeight());
        newBi = Scalr.rotate(newBi, Scalr.Rotation.CW_90, Scalr.OP_ANTIALIAS);
        System.out.println("Rotate: AFTER:" + newBi.getWidth() + "x" + newBi.getHeight());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(newBi, "jpg", baos);
        byte[] bytes = baos.toByteArray();
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
          dialogValidate(currentDisplayMode);
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
    
    private void dialogValidate(DisplayMode currentDisplayMode) {
        //validate fields and enable OK button if valid
        if(validationEnabled && this.stockEntity!=null){
            hasChangedValues = Boolean.FALSE;
            if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
                validateAvatar(fieldProfileAvatar,avatarDiv,this.stockEntity.getProfileImage().toString());
            }else if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
                
                //validateField(fieldReceiptTotal,this.stockEntity.getReceiptTotal());
                //validateCheckbox(fieldFeesOnly,this.stockEntity.getFeesOnly());
            }else{
                //validateField(fieldReceiptTotal,this.stockEntity.getReceiptTotal());
                //validateCheckbox(fieldWebOrder,this.stockEntity.getWebOrder());
            }
            //do common fields here
//            validateListbox(fieldPaymentMethod,this.stockEntity.getPaymentMethod());
//            validateField(fieldDeliveryFee,this.stockEntity.getDeliveryFee());
//            validateField(fieldServiceFee,this.stockEntity.getServiceFee());
//            validateField(fieldServiceFeePercent,getServiceFeePercent());
//            validateField(fieldTotalSale.getNumberField(),this.stockEntity.getTotalSale());
//            validateField(fieldTip,this.stockEntity.getTip());
//            validateCheckbox(fieldTipIssue,this.stockEntity.getTipInNotesIssue());
//            if(customTaskConverted) hasChangedValues = Boolean.TRUE;
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

    private void validateAvatar(Avatar field, Div fieldDiv, String value){
        if(field.getName().equals(value)){
            fieldDiv.getStyle().set("box-shadow","none");
            fieldDiv.getStyle().set("border-width",UIUtilities.borderSizeSmall);
        }else{
            fieldDiv.getStyle().set("box-shadow",UIUtilities.boxShadowStyle);
            fieldDiv.getStyle().set("border-radius",UIUtilities.boxShadowStyleRadius);
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

    public FormLayout showItem(Stock stockEntity, DisplayMode currentDisplayMode){
        return showItem(stockEntity, currentDisplayMode, false, true);
    }

    public FormLayout showItem(Stock currentStock, DisplayMode currentDisplayMode, Boolean forceReadOnly, Boolean showHeader){
        validationEnabled = Boolean.FALSE;
        FormLayout stockFormLayout = new FormLayout();
        stockFormLayout.setWidthFull();
        stockFormLayout.setAutoResponsive(false);
        //stockFormLayout.setColumnWidth("8em");
        stockFormLayout.setExpandColumns(true);
        stockFormLayout.setExpandFields(true);
        stockFormLayout.setMaxColumns(3);
        stockFormLayout.setMinColumns(1);
        
        if(forceReadOnly){
            fieldFather.setReadOnly(true);
            fieldMother.setReadOnly(true);
            fieldFatherName.setReadOnly(true);
            fieldMotherName.setReadOnly(true);
            fieldCategory.setReadOnly(true);
            fieldStatus.setReadOnly(true);
            fieldStatusDate.setReadOnly(true);
            fieldFoster.setReadOnly(true);
            fieldAquiredDate.setReadOnly(true);
            fieldBornDate.setReadOnly(true);
            fieldChampNo.setReadOnly(true);
            fieldGenotype.setReadOnly(true);
            fieldLegs.setReadOnly(true);
            fieldRegNo.setReadOnly(true);
        }
        
        stockFormLayout.setResponsiveSteps(
        // Use one column by default
        new ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
        // Use two columns, if the layout's width exceeds 320px
        new ResponsiveStep("600px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        /*
        stockFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));
        stockFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("100px", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE));
        */
        //this.stockEntity = stockEntity;

        //configure the field layout
        //log.info("showItem:" + currentStock.getName() + ", displayMode:" + currentDisplayMode);
        stockFormLayout.removeAll();

        //add the header
        if(showHeader){
            stockFormLayout.add(getStockHeader(currentStock, Boolean.FALSE));
        }

        //common fields

        //fields by displayMode type
        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            if(dialogMode.equals(DialogMode.EDIT)){
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
            }
            if(dialogMode.equals(DialogMode.VIEW)){
                stockFormLayout.addFormItem(fieldFatherName,"Father");
                stockFormLayout.addFormItem(fieldMotherName,"Mother");
            }
            
            stockFormLayout.addFormItem(fieldGenotype,"Genotype");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldLegs,"Legs");
            stockFormLayout.addFormItem(fieldChampNo,"Championship Number");
            stockFormLayout.addFormItem(fieldRegNo,"Registration Number");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldStatus,"Status");
            stockFormLayout.addFormItem(fieldStatusDate,"Status Date");
            stockFormLayout.addFormItem(fieldCategory,"Category");
            stockFormLayout.getElement().appendChild(ElementFactory.createBr()); // row break
            stockFormLayout.addFormItem(fieldAquiredDate,"Aquired");
            stockFormLayout.addFormItem(fieldBornDate,"Born");
            stockFormLayout.addFormItem(fieldFoster,"Foster");
        }else if(currentDisplayMode.equals(DisplayMode.LITTER_LIST)){
            
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            stockFormLayout.add(avatarDiv);
        }

        //set values
        setValues(currentStock, currentDisplayMode);
        return stockFormLayout;
    }

    private void setValues(Stock currentStock, DisplayMode currentDisplayMode){
        validationEnabled = Boolean.FALSE;

        if(currentDisplayMode.equals(DisplayMode.STOCK_DETAILS)){
            if(dialogMode.equals(DialogMode.EDIT)){
                fieldName.setValue(currentStock.getName());
                fieldPrefix.setValue(currentStock.getPrefix());
                fieldTattoo.setValue(currentStock.getTattoo());
                fieldBreed.setValue(currentStock.getBreed());

                fieldGender.setRenderer(new TextRenderer<Gender>(gender -> {
                    if(gender.equals(Gender.MALE)) return currentStock.getStockType().getMaleName();
                    return currentStock.getStockType().getFemaleName();
                }));        
                
                fieldGender.setValue(currentStock.getSex());
                fieldBreeder.setValue(currentStock.isBreeder());
                fieldColor.setValue(currentStock.getColor());
                System.out.println("setValues: weight:" + currentStock.getWeight());
                fieldWeight.setValue(currentStock.getWeight());
                
                fieldFather.setItems(stockService.getFathers());
                fieldFather.setItemLabelGenerator(Stock::getDisplayName);
                fieldFather.setValue(stockService.findById(currentStock.getFatherId()));
                fieldMother.setItems(stockService.getMothers());
                fieldMother.setItemLabelGenerator(Stock::getDisplayName);
                fieldMother.setValue(stockService.findById(currentStock.getMotherId()));
            }
            if(dialogMode.equals(DialogMode.VIEW)){
                if(currentStock.getFatherId()!=null){
                    fieldFatherName.setValue(stockService.findById(currentStock.getFatherId()).getDisplayName());
                }
                if(currentStock.getMotherId()!=null){
                    fieldMotherName.setValue(stockService.findById(currentStock.getMotherId()).getDisplayName());
                }
}
            fieldAquiredDate.setValue(currentStock.getAcquired());
            //System.out.println("***Set Aquired::" + currentStock.getAcquired());
            
            fieldBornDate.setValue(currentStock.getDoB());
            fieldLegs.setValue(currentStock.getLegs());
            fieldChampNo.setValue(currentStock.getChampNo());
            fieldRegNo.setValue(currentStock.getRegNo());
            
            fieldGenotype.setValue(currentStock.getGenotype());
            fieldCategory.setValue(currentStock.getCategory());
            fieldStatus.setValue(currentStock.getStatus());
            //System.out.println("***Set Status::" + currentStock.getStatus());
            fieldStatusDate.setValue(currentStock.getStatusDate());
            fieldFoster.setValue("TODO");
        }else if(currentDisplayMode.equals(DisplayMode.PROFILE_IMAGE)){
            fieldProfileAvatar.setImageHandler(DownloadHandler.forFile(currentStock.getProfileFile()));
            fieldProfileAvatar.setName(currentStock.getProfileImage());
            profileImageData = getByteArrayFromImageFile(currentStock.getProfileFile().getAbsolutePath());
            profileImageMimeType = "image/*";
        }else{
            //Phonein
            //fieldWebOrder.setValue(this.stockEntity.getWebOrder());
            //fieldReceiptTotal.setValue(this.stockEntity.getReceiptTotal());
        }
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
        
        stockName.setImageHandler(DownloadHandler.forFile(item.getProfileFile()));

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

    public void addListener(ListRefreshNeededListener listener){
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded(){
        for (ListRefreshNeededListener listener: listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

//    public DisplayMode getDisplayMode() {
//        return displayMode;
//    }
//
//    public void setDisplayMode(DisplayMode displayMode) {
//        this.displayMode = displayMode;
//    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(String dialogTitle) {
        this.dialogTitle = dialogTitle;
    }
    
}
