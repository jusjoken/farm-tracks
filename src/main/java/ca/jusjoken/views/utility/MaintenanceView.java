/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.utility;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Import;
import ca.jusjoken.data.Utility;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 *
 * @author birch
 */
@PageTitle("Maintenance")
@Route("maintenance")
@Menu(order = 0, icon = LineAwesomeIconUrl.COGS_SOLID)
@PermitAll
@Uses(Icon.class)
public class MaintenanceView extends Div{
    private Import importUtility = new Import();
    private Button processButton = new Button("Process import");
    private Upload kitUpload;
    private Upload breederUpload;
    private Upload litterUpload;
    
    public MaintenanceView() {
        setSizeFull();
        addClassNames("maintenance-view");
        
        VerticalLayout layout = new VerticalLayout(createImportAllSection(), createIndividualImportSection());
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);

    }

    //this will be a controlled import that will remove all Everbreed data and reimport from breeders, kits and litters csv files
    private Details createImportAllSection() {
        VerticalLayout importItems = new VerticalLayout();
        importItems.setSizeFull();
        Details details = new Details("Full import from Everbreed", importItems);
        details.setOpened(true);

        processButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        processButton.setEnabled(false);
        processButton.setDisableOnClick(true);
        processButton.addClickListener(event -> {
            importUtility.processAllImportsFromEverbreed();
            kitUpload.clearFileList();
            breederUpload.clearFileList();
            litterUpload.clearFileList();
            processButton.setEnabled(false);
        });

        importItems.add(createImportLayout(Utility.ImportType.BREEDERS,Boolean.FALSE));
        importItems.add(createImportLayout(Utility.ImportType.KITS,Boolean.FALSE));
        importItems.add(createImportLayout(Utility.ImportType.LITTERS,Boolean.FALSE));
        importItems.add(processButton);
        return details;
    }

    private VerticalLayout createImportLayout(Utility.ImportType importType, Boolean autoProcess) {
        VerticalLayout importItems = new VerticalLayout();
        importItems.setSizeFull();

        FileBuffer fileBuffer = new FileBuffer();
        Upload upload = new Upload(fileBuffer);
        upload.setWidthFull();
        upload.setDropAllowed(false);
        upload.setAcceptedFileTypes("text/csv");
        
        String buttonName;
        if(importType.equals(importType.BREEDERS)){
            buttonName = "Select breeders file for import";
        }else if(importType.equals(importType.KITS)){
            buttonName = "Select kits file for import";
        }else{
            buttonName = "Select litters file for import";
        }
        Button uploadButton = new Button(buttonName);
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(uploadButton);
        
        // Disable the upload button after the file is selected
        // Re-enable the upload button after the file is cleared
        upload.getElement()
                .addEventListener("max-files-reached-changed", event -> {
                    boolean maxFilesReached = event.getEventData()
                            .getBoolean("event.detail.value");
                    uploadButton.setEnabled(!maxFilesReached);
                }).addEventData("event.detail.value");            

        upload.addStartedListener(event -> {
            if(autoProcess){
                importUtility.clearAllImportLists();
            }else{
                switch (importType) {
                    case BREEDERS:
                        importUtility.clearBreederList();
                        break;
                    case KITS:
                        importUtility.clearKitList();
                        break;
                    case LITTERS:
                        importUtility.clearLitterList();
                        break;
                    default:
                        break;
                }
            }
        });
        upload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            String filePath = savedFileData.getFile().getAbsolutePath();
            System.out.println("file:" + filePath);
            if(importType.equals(importType.BREEDERS)){
                breederUpload = upload;
                importUtility.importBreederFromEverbreed(filePath);
                if(importUtility.hasBreeders() && autoProcess){
                    importUtility.processBreedersFromEverbreed();
                }
            }else if(importType.equals(importType.KITS)){
                kitUpload = upload;
                importUtility.importKitFromEverbreed(filePath);
                if(importUtility.hasKits() && autoProcess){
                    importUtility.processKitsFromEverbreed();
                }
            }else if(importType.equals(importType.LITTERS)){
                litterUpload = upload;
                importUtility.importLitterFromEverbreed(filePath);
                if(importUtility.hasLitters()&& autoProcess){
                    importUtility.processLittersFromEverbreed();
                }
            }else{
                
            }
            if(!autoProcess && importUtility.hasBreeders() && importUtility.hasKits() && importUtility.hasLitters()){
                processButton.setEnabled(true);
            }
        });
        importItems.add(upload);
        return importItems;
    }

    private Details createIndividualImportSection() {
        VerticalLayout importItems = new VerticalLayout();
        importItems.setSizeFull();
        Details details = new Details("Individual import from Everbreed exports", importItems);
        details.setOpened(true);
        importItems.add(createImportLayout(Utility.ImportType.BREEDERS,Boolean.TRUE));
        importItems.add(createImportLayout(Utility.ImportType.KITS,Boolean.TRUE));
        importItems.add(createImportLayout(Utility.ImportType.LITTERS,Boolean.TRUE));
        
        return details;
    }
    
}
