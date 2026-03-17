/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.utility;

import ca.jusjoken.component.ProgressBarUpdateListener;
import ca.jusjoken.data.Import;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.ImportType;
import ca.jusjoken.data.service.BackendService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import jakarta.annotation.security.RolesAllowed;
import java.text.NumberFormat;

/**
 *
 * @author birch
 */
@PageTitle("Maintenance")
@Route("maintenance")
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class MaintenanceView extends Div implements ProgressBarUpdateListener{
    private final Import importUtility = new Import();
    private final Button processButton = new Button("Process import");
    private final ProgressBar progress = new ProgressBar();
    private final NativeLabel progressBarLabelText = new NativeLabel();
    private final Span progressBarLabelValue = new Span();

    private Upload kitUpload;
    private Upload breederUpload;
    private Upload litterUpload;
    private final VaadinService vService = VaadinService.getCurrent();
    private UI ui;
    
    public MaintenanceView() {
        addProgressBarUpdateListener();
        setSizeFull();
        addClassNames("maintenance-view");
        
        VerticalLayout layout = new VerticalLayout(createImportAllSection());
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);

    }

    // Add a method to set the progress bar update listener
    private void addProgressBarUpdateListener() {
        importUtility.addListener(this);
    }

    //this will be a controlled import that will remove all Everbreed data and reimport from breeders, kits and litters csv files
    private VerticalLayout createImportAllSection() {
        VerticalLayout importItems = new VerticalLayout();
        importItems.setSizeFull();
        importItems.setAlignItems(FlexComponent.Alignment.START);
        importItems.setJustifyContentMode(JustifyContentMode.START);
        BackendService backendService = new BackendService();

        processButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        processButton.setEnabled(false);
        processButton.setDisableOnClick(true);

        processButton.addClickListener(clickEvent -> {
            ui = clickEvent.getSource().getUI().orElseThrow();
            System.out.println("processButton: before calling long");
            progressStatusMessage("Processing imports from Everbreed...");
            
            vService.getExecutor().execute(() -> {
                backendService.longRunningTask(importUtility);
                
                ui.access(() -> {
                    progressStatusMessage("All imports completed.");
                    kitUpload.clearFileList();
                    breederUpload.clearFileList();
                    litterUpload.clearFileList();
                    processButton.setEnabled(false);
                });
            });
            
        });

        importItems.add(createImportLayout(Utility.ImportType.BREEDERS,Boolean.FALSE));
        importItems.add(createImportLayout(Utility.ImportType.KITS,Boolean.FALSE));
        importItems.add(createImportLayout(Utility.ImportType.LITTERS,Boolean.FALSE));
        
        HorizontalLayout progressBarLabel = new HorizontalLayout(progressBarLabelText, progressBarLabelValue);
        progressBarLabel.setJustifyContentMode(JustifyContentMode.BETWEEN);
        progressBarLabel.setWidthFull();
        importItems.add(progressBarLabel);
        progress.setWidthFull();
        importItems.add(progress);

        importItems.add(processButton);
        return importItems;
    }

    private VerticalLayout createImportLayout(Utility.ImportType importType, Boolean autoProcess) {
        VerticalLayout importItems = new VerticalLayout();
        importItems.setWidthFull();

        FileBuffer fileBuffer = new FileBuffer();


        Upload upload = new Upload(fileBuffer);
        upload.setWidthFull();
        upload.setDropAllowed(false);
        upload.setAcceptedFileTypes("text/csv");
        
        String buttonName;
        buttonName = switch (importType) {
            case BREEDERS -> "Select breeders file for import";
            case KITS -> "Select kits file for import";
            default -> "Select litters file for import";
        };
        Button uploadButton = new Button(buttonName);
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(uploadButton);
        
        // Disable the upload button after the file is selected
        // Re-enable the upload button after the file is cleared
        upload.getElement()
                .addEventListener("max-files-reached-changed", event -> {
                    boolean maxFilesReached = event.getEventData().get("event.detail.value").asBoolean();
                    uploadButton.setEnabled(!maxFilesReached);
                }).addEventData("event.detail.value");            

        upload.addStartedListener(event -> {
            if(autoProcess){
                importUtility.clearAllImportLists();
            }else{
                if (importUtility != null) {
                    switch (importType) {
                        case BREEDERS -> importUtility.clearBreederList();
                        case KITS -> importUtility.clearKitList();
                        case LITTERS -> importUtility.clearLitterList();
                    }
                } else {
                    // Handle the case when importUtility is null, such as logging an error or displaying a message to the user.
                    System.out.println("ImportUtility may not have been initialized");
                }
            }
        });
        upload.addSucceededListener(event -> {
            //clear any previous status
            progressBarLabelText.setText("");
            progressBarLabelValue.setText("");
            FileData savedFileData = fileBuffer.getFileData();
            String filePath = savedFileData.getFile().getAbsolutePath();
            System.out.println("file:" + filePath);
            switch (importType) {
                case BREEDERS -> {
                    breederUpload = upload;
                    importUtility.importBreederFromEverbreed(filePath);
                    if (importUtility.hasBreeders() && autoProcess) {
                        importUtility.processBreedersFromEverbreed();
                    }
                }
                case KITS -> {
                kitUpload = upload;
                    importUtility.importKitFromEverbreed(filePath);
                    if (importUtility.hasKits() && autoProcess) {
                        importUtility.processKitsFromEverbreed();
                    }
                }
                case LITTERS -> {
                    litterUpload = upload;
                    importUtility.importLitterFromEverbreed(filePath);
                    if (importUtility.hasLitters() && autoProcess) {
                        importUtility.processLittersFromEverbreed();
                    }
                }
            }
            if(!autoProcess && importUtility.hasBreeders() && importUtility.hasKits() && importUtility.hasLitters()){
                processButton.setEnabled(true);
            }
        });
        importItems.add(upload);
        return importItems;
    }

    @Override
    public void progressUpdate(Double value) {
        System.out.println("progressUpdate:" + value);
        ui.access(() -> {
            progress.setValue(value);
            Double percent = progress.getValue() / progress.getMax();
            NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMaximumFractionDigits(0);
            System.out.println("progressStatusMessage: percent:" + nf.format(percent));
            progressBarLabelValue.setText(nf.format(percent));
        });
    }

    @Override
    public void progressMax(Double value) {
        ui.access(() -> {
            progress.setMax(value);
            progress.setWidthFull();
        });
    }

    @Override
    public void progressStatusMessage(String value) {
        ui.access(() -> {
            progressBarLabelText.setText(value);
        });
    }
    
}
