/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.stock;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.Badge;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.WeightInput;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.Generation;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.views.MainLayout;
import com.spire.doc.AutoFitBehaviorType;
import com.spire.doc.CellWidthType;
import com.spire.doc.Document;
import com.spire.doc.DocumentObject;
import com.spire.doc.FileFormat;
import com.spire.doc.PreferredWidth;
import com.spire.doc.Section;
import com.spire.doc.Table;
import com.spire.doc.TableCell;
import com.spire.doc.TableRow;
import com.spire.doc.documents.BookmarksNavigator;
import com.spire.doc.documents.BorderStyle;
import com.spire.doc.documents.DocumentObjectType;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ParagraphStyle;
import com.spire.doc.documents.TableRowHeightType;
import com.spire.doc.documents.TextBodyPart;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.fields.TextRange;
import com.vaadin.componentfactory.pdfviewer.PdfViewer;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author birch
 */
@Route(value = "pedigree-editor", layout = MainLayout.class)
@PermitAll
public class StockPedigreeEditor extends Main implements ListRefreshNeededListener, HasDynamicTitle, HasUrlParameter<String>   {
    private final StockService stockService;
    private Stock stock;
    private StockType viewStockType;
    private Stock selectedParent;
    private Boolean hasStock;
    private String name = Utility.emptyValue;
    private List<Generation> genList;
    private TreeData<Generation> treeData = new TreeData<>();
    private TreeGrid<Generation> tree = new TreeGrid();
    private MasterDetailLayout mdLayout = new MasterDetailLayout();
    private File appPath = new File(System.getProperty("user.dir"));
    private File outputDir = new File(appPath,"output");
    private File pdfFile = null;
    private String pdfFileName = "";
    @Value("classpath:Pedigree_Template.docx")
    private Resource resourcePedigreeTemplate;
    @Value("classpath:Pedigree_Template_portrait.docx")
    private Resource resourcePedigreeTemplatePortrait;
    @Value("classpath:/META-INF/resources/images/gender_male.png")
    private Resource resourceGenderMale;
    @Value("classpath:/META-INF/resources/images/gender_female.png")
    private Resource resourceGenderFemale;
    private Logger log = LoggerFactory.getLogger(StockPedigreeEditor.class);
    //all include options
    private Checkbox includeDob = new Checkbox();
    private Checkbox includeColor = new Checkbox();
    private Checkbox includeBreed = new Checkbox();
    private Checkbox includeWeight = new Checkbox();
    private Checkbox includeId = new Checkbox();
    private Checkbox includeLegs = new Checkbox();
    private Checkbox includeRegNo = new Checkbox();
    private Checkbox includeChampNo = new Checkbox();

    private List<Integer> topLevelGenerations = new ArrayList<>();
    private ParagraphStyle topLevelDetailsStyle;
    private ParagraphStyle allLevelDetailsStyle;
    private Integer rowCounter = 0;
    private Integer colCounter = 0;
    private Integer tableRows = 4;

    public StockPedigreeEditor(StockService stockService) {
        this.stockService = stockService;
        this.resourcePedigreeTemplate = new ClassPathResource("Pedigree_Template.docx");
        this.resourcePedigreeTemplatePortrait = new ClassPathResource("Pedigree_Template_portrait.docx");
        this.resourceGenderMale = new ClassPathResource("/META-INF/resources/images/gender_male.png");
        this.resourceGenderFemale = new ClassPathResource("/META-INF/resources/images/gender_female.png");
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Height.FULL, LumoUtility.Overflow.HIDDEN);
        createTopLevelList();
        //NOTE: setParameter is called NEXT and is used to create the view layout etc.
    }
    
    private void createTopLevelList(){
        //these are the levels that require smaller fonts due to space limitations
        topLevelGenerations.add(3);
        topLevelGenerations.add(4);
        topLevelGenerations.add(6);
        topLevelGenerations.add(7);
        topLevelGenerations.add(10);
        topLevelGenerations.add(11);
        topLevelGenerations.add(13);
        topLevelGenerations.add(14);
        
    }

    private Component createContent() {
        System.out.println("StockPedigreeEditor createContent: hasStock:" + hasStock);
        mdLayout.setSizeFull();
        //mdLayout.setMasterMinSize("600px");
        mdLayout.setDetailSize("450px");
        if(stock!=null && hasStock){
            mdLayout.setMaster(createPedigree());
        }else{ //blank view
            mdLayout.setMaster(new Span("No stock"));
            mdLayout.setDetail(null);
        }
        return mdLayout;
    }
    
    private Component createPedigree(){
        VerticalLayout layout = UIUtilities.getVerticalLayout(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
        layout.setSizeFull();

        tree.setSelectionMode(Grid.SelectionMode.SINGLE);
        
        tree.asSingleSelect().addValueChangeListener(event -> {
            Generation selected = event.getValue();
            if(selected != null){
                mdLayout.setDetail(createItemEditor(selected));
            }else{
                mdLayout.setDetail(null);
            }
        });
        
        TreeDataProvider<Generation> treeDataProvider = new TreeDataProvider<>(treeData, HierarchicalDataProvider.HierarchyFormat.FLATTENED);
        tree.setDataProvider(treeDataProvider);
        layout.add(tree);
        
        genList = loadGenerations();
        
        //tree design
        tree.addComponentHierarchyColumn(gen -> {
            return new Div("");
        }).setHeader("").setWidth("100px").setFlexGrow(0);
        tree.addComponentColumn(Generation::getHeader).setHeader("Name").setWidth("400px").setFlexGrow(1);
        tree.setHeightFull();
        tree.addCollapseListener(listener -> {
            //the following forces all nodes to be expanded all the time and diables collapse
            tree.expand(genList);
        });
        treeDataProvider.refreshAll();
        tree.expand(genList);

        return layout;
    }
    
    private Component createItemEditor(Generation item) {
        VerticalLayout editorLayout = UIUtilities.getVerticalLayout(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        VerticalLayout editor = UIUtilities.getVerticalLayout(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        editor.setSizeFull();
        Scroller scroller = new Scroller(editor);
        //scroller.setMaxHeight("500px");
        scroller.setSizeFull();
        editorLayout.setSizeFull();
        editorLayout.setFlexGrow(1, scroller);
        
        //add breadcrumbs to show path to current selection
        HorizontalLayout badges = UIUtilities.getHorizontalLayout(false, true, false);
        for(Generation badgeItem: item.getBreadcrumbs()){
            Badge newBadge = new Badge(badgeItem.getName());
            UIUtilities.setBorders(newBadge, badgeItem.getStock(), UIUtilities.BorderSize.XSMALL);
            newBadge.addClickListener(click -> {
                tree.select(badgeItem);
            });
            badges.add(newBadge);
        }
        editorLayout.add(badges);
        editorLayout.add(scroller);
        
        //show parent selection for the child record
        Stock childStock = null;
        String parentTypeAction = "";
        Boolean showParentSelect = Boolean.FALSE;
        Boolean showDetails = Boolean.FALSE;
        Boolean showDetailsReadOnly = Boolean.FALSE;
        
        if(!item.getStock().isTemp()){
            if(item.getChild()==null){
                //First root item selected
                editor.add(createDetailsLayout(item, true));
            }else{
                //Existing stock OTHER THAN ROOT - Can CHANGE parent to another existing
                childStock = item.getChild().getStock();
                parentTypeAction = "Update existing";
                editor.add(createParentSelect(item, childStock, parentTypeAction));
                editor.add(createDetailsLayout(item, true));
            }
        }else{
            //TEMP item
            parentTypeAction = "Select existing";
            childStock = item.getChild().getStock();
            if(childStock.isTemp()){
                //previous level should be completed first
                Span noOptions = new Span("Previous pedigree level must be completed first!");
                editor.add(noOptions);
            }else{
                //allow parent selection OR entry
                
                editor.add(createParentSelect(item, childStock, parentTypeAction));
                editor.add(createDetailsLayout(item, false));
            }
        }
        System.out.println("createItemEditor: showParentSelect:" + showParentSelect + " showDetails:" + showDetails + " showDetailsRO:" + showDetailsReadOnly);
        
        HorizontalLayout footer = UIUtilities.getHorizontalLayout(true, true, false);
        //close button
        Button closeButton = new Button("Close");
        closeButton.addClickListener(click -> {
            mdLayout.setDetail(null);
        
        });
        footer.add(closeButton);

        //pdf button
        Button pdfButton = new Button("PDF");
        pdfButton.addClickListener(click -> {
            createPedigreePDF(item);
        });
        footer.add(pdfButton);

        //report button
        /*
        Button reportButton = new Button("Report");
        reportButton.addClickListener(click -> {
            createPedigreeReport();
        });
        editor.add(reportButton);
        */

        //save button
        Button saveButton = new Button("Save");
        saveButton.addClickListener(click -> {
            System.out.println("createItemEditor: SAVE: item name" + item.getChild().getStock().getDisplayName() + " selected parent:" + selectedParent.getDisplayName() + " Gender:" + item.getSex());
            saveParent(item.getChild().getStock(), selectedParent, item.getSex());

        });
        footer.add(saveButton);

        //PDF output settings
        FormLayout settingsForm = new FormLayout();

        settingsForm.setWidthFull();
        settingsForm.setAutoResponsive(false);
        settingsForm.setExpandColumns(true);
        settingsForm.setExpandFields(true);
        settingsForm.setMaxColumns(2);
        settingsForm.setMinColumns(1);

        settingsForm.setResponsiveSteps(
        // Use one column by default
        new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE),
        // Use two columns, if the layout's width exceeds 320px
        new FormLayout.ResponsiveStep("320px", 2,FormLayout.ResponsiveStep.LabelsPosition.TOP),
        // Use three columns, if the layout's width exceeds 500px
        new FormLayout.ResponsiveStep("900px", 3,FormLayout.ResponsiveStep.LabelsPosition.TOP));        
        settingsForm.removeAll();

        settingsForm.add("Include the following optional fields:");
        
        Details pdfSettings = new Details("PDF Settings",settingsForm);

        addIncludeField(settingsForm, includeId, "includeTattoo", "ID");
        addIncludeField(settingsForm, includeDob, "includeDob", "DOB");
        addIncludeField(settingsForm, includeColor, "includeColour", "Colour");
        addIncludeField(settingsForm, includeBreed, "includeBreed", "Breed");
        addIncludeField(settingsForm, includeWeight, "includeWeight", "Weight");
        addIncludeField(settingsForm, includeLegs, "includeLegs", "Legs");
        addIncludeField(settingsForm, includeRegNo, "includeRegNo", "RegNo");
        addIncludeField(settingsForm, includeChampNo, "includeChampNo", "ChampNo");

        editorLayout.add(pdfSettings);
        editorLayout.add(footer);
        return editorLayout;
    }
    
    private void addIncludeField(FormLayout settingsForm, Checkbox includeBox, String includeId, String label){
        includeBox.setId(includeId);
        includeBox.addValueChangeListener(event -> {
            saveLocalSetting(event.getSource().getId().get(), event.getValue());
        });
        settingsForm.addFormItem(includeBox, label);
        setLocalSetting(includeBox);
    }
    
    private void saveLocalSetting(String id, Boolean value){
        WebStorage.setItem(id, value.toString());
        //System.out.println("saveLocalSetting: id:" + id + " value:" + value);
    }
    
    private void setLocalSetting(Checkbox checkbox){
        WebStorage.getItem(checkbox.getId().get(), callback -> {
            //System.out.println("setLocalSetting: id:" + checkbox.getId() + " value:" + callback + " getBoolean:" + Boolean.valueOf(callback));
            if(callback!=null) checkbox.setValue(Boolean.valueOf(callback));
        });
        
    }
    
    private void saveParent(Stock stockToSave, Stock newParent, Gender sex){
        if(stockToSave==null || newParent==null){
            System.out.println("saveParent: Could not change parent as stock or parent was NULL: stock:" + stockToSave + " parent:" + newParent);
        }else{
            if(sex.equals(Gender.MALE)){
                stockToSave.setFatherId(newParent.getId());
            }else{
                stockToSave.setMotherId(newParent.getId());
            }
            stockService.save(stockToSave);
        }
    }
    
    private Select<Stock> createParentSelect(Generation item, Stock childStock, String parentTypeAction){
        String parentType = "";
        Select<Stock> parentSelect = new Select();
        parentSelect.setWidthFull();
        //parentSelect.setItemLabelGenerator(Stock::getDisplayName);
        parentSelect.setItemLabelGenerator(labelItem -> {
            String label = labelItem.getDisplayName();
            if(labelItem.getFatherId()!=null) label = label + " (" + stockService.findById(labelItem.getFatherId()).getDisplayName() + ")";
            if(labelItem.getMotherId()!=null) label = label + " (" + stockService.findById(labelItem.getMotherId()).getDisplayName() + ")";
            return label;
        });
        if(item.getSex().equals(Gender.MALE)){
            parentSelect.setItems(stockService.getFathers(childStock.getFatherExtName(),childStock.getStockType()));
            parentType = "Father";
        }else{
            parentSelect.setItems(stockService.getMothers(childStock.getMotherExtName(),childStock.getStockType()));
            parentType = "Mother";
        }
        parentSelect.setLabel(parentTypeAction + " " + parentType + " for:" + childStock.getDisplayName());
        parentSelect.setValue(item.getStock());
        System.out.println("createItemEditor: setValue to:" + childStock);
        parentSelect.addValueChangeListener(selection -> {
            System.out.println("createItemEditor: addValueChangeListener:" + selection.getValue());
            selectedParent = selection.getValue();
        });
        return parentSelect;
    }
    
    private FormLayout createDetailsLayout(Generation item, Boolean showDetailsReadOnly){
        FormLayout formLayout = new FormLayout();
        TextField prefixField = new TextField();
        formLayout.addFormItem(prefixField, "Prefix");
        prefixField.setValue(item.getPrefix());
        TextField nameField = new TextField();
        formLayout.addFormItem(nameField, "Name");
        nameField.setValue(item.getName());
        TextField tattooField = new TextField();
        formLayout.addFormItem(tattooField, "ID");
        tattooField.setValue(item.getTattoo());
        DatePicker doBField = new DatePicker();
        formLayout.addFormItem(doBField, "DOB");
        doBField.setValue(item.getDoB());
        TextField colorField = new TextField();
        formLayout.addFormItem(colorField, "Color");
        colorField.setValue(item.getColor());
        WeightInput weightField = new WeightInput();
        formLayout.addFormItem(weightField, "Weight");
        weightField.setValue(item.getWeight());
        TextField legsField = new TextField();
        formLayout.addFormItem(legsField, "Legs");
        legsField.setValue(item.getLegs());
        TextField champNoField = new TextField();
        formLayout.addFormItem(champNoField, "ChampNo");
        champNoField.setValue(item.getChampNo());
        TextField regNoField = new TextField();
        formLayout.addFormItem(regNoField, "RegNo");
        regNoField.setValue(item.getRegNo());

        //set width on all formFields
        for(Component formItem: formLayout.getChildren().toList()){
            for(Component formItemField: formItem.getChildren().toList()){
                if(formItemField instanceof HasSize){
                    ((HasSize) formItemField).setWidthFull();
                    if(showDetailsReadOnly){
                        if(formItemField instanceof AbstractField){
                            ((AbstractField) formItemField).setReadOnly(true);
                        }
                    }
                }
            }
        }
        return formLayout;
    }
    
    private List<Generation> loadGenerations(){
        List<Generation> list = new ArrayList<>();
        Generation B = new Generation(stock, 1);
        treeData.addRootItems(B);
        B.addBreadcrumb(B);
        createGeneration(B, list);
        return list;
    }
    
    private Generation createGeneration(Generation gen, List<Generation> list){
        //get parent stock from child passed in 
        Stock stockF;
        Stock stockM;
        if(gen.getStock()!=null){ //get from parent
            stockF = stockService.getFather(gen.getStock());
            if(stockF==null){
                stockF = createGenericStock(Utility.Gender.MALE);
            }
            stockM = stockService.getMother(gen.getStock());
            if(stockM==null){
                stockM = createGenericStock(Utility.Gender.FEMALE);
            }
        }else{
            stockF = createGenericStock(Utility.Gender.MALE);
            stockM = createGenericStock(Utility.Gender.FEMALE);
        }
        Generation genF = new Generation(stockF, gen.getLevel()+1);
        Generation genM = new Generation(stockM, gen.getLevel()+1);
        gen.setFather(genF);
        gen.setMother(genM);
        list.add(gen);
        if(gen.getLevel()<4){
            treeData.addItem(gen, genF);
            genF.getBreadcrumbs().clear();
            genF.getBreadcrumbs().addAll(gen.getBreadcrumbs());
            genF.addBreadcrumb(genF);
            genF.setChild(gen);
            createGeneration(genF, list);
            treeData.addItem(gen, genM);
            genM.getBreadcrumbs().clear();
            genM.getBreadcrumbs().addAll(gen.getBreadcrumbs());
            genM.addBreadcrumb(genM);
            genM.setChild(gen);
            createGeneration(genM, list);
        }
        return gen;
    }
    
    private Stock createGenericStock(Utility.Gender sex){
        Stock genStock = new Stock(Utility.emptyValue ,Boolean.TRUE, viewStockType);
        genStock.setSex(sex);
        return genStock;
    }
    
    private void createPedigreePDF(Generation baseItem){
        pdfFileName = "Pedigree_" + baseItem.getName() + ".pdf";
        
        try {
            Files.createDirectories(outputDir.toPath());
            System.out.println("Directory created: " + outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }        
        
        File outputFile = new File(outputDir,pdfFileName);
        pdfFile = outputFile;
        try {
            
            InputStream in = resourcePedigreeTemplate.getInputStream();
            InputStream inP = resourcePedigreeTemplatePortrait.getInputStream();
            Document document = new Document();
            document.loadFromStream(in, FileFormat.Doc);
            //create a reduced font size style for name for top level
            ParagraphStyle topLevelNameStyleMale = new ParagraphStyle(document);
            ParagraphStyle topLevelNameStyleFemale = new ParagraphStyle(document);
            ParagraphStyle allLevelNameStyleMale = new ParagraphStyle(document);
            ParagraphStyle allLevelNameStyleFemale = new ParagraphStyle(document);
            ParagraphStyle farmNameStyle = new ParagraphStyle(document);
            ParagraphStyle farmAddressStyle = new ParagraphStyle(document);
            ParagraphStyle farmEmailStyle = new ParagraphStyle(document);
            topLevelDetailsStyle = new ParagraphStyle(document);
            allLevelDetailsStyle = new ParagraphStyle(document);
            topLevelNameStyleMale.setName("TopLevelNameMale");
            topLevelNameStyleFemale.setName("TopLevelNameFemale");
            allLevelNameStyleMale.setName("AllLevelNameMale");
            allLevelNameStyleFemale.setName("AllLevelNameFemale");
            topLevelDetailsStyle.setName("TopLevelDetails");
            allLevelDetailsStyle.setName("AllLevelDetails");
            allLevelNameStyleMale.getCharacterFormat().setFontSize(12);
            allLevelNameStyleFemale.getCharacterFormat().setFontSize(12);
            allLevelDetailsStyle.getCharacterFormat().setFontSize(10);
            
            //change font size for TOP level based on number of include fields
            Integer includeCount = getCountIncludeFields();
            float topLevelNameSize = 7;
            float topLevelDetailsSize = 6;
            float col1Width = 87;
            float col2Width = 87;
            tableRows = 4;
            switch(includeCount){
                case 0:
                    topLevelNameSize = 12;
                    topLevelDetailsSize = 10;
                    break;
                case 1:
                    topLevelNameSize = 12;
                    topLevelDetailsSize = 10;
                    col1Width = 174;
                    col2Width = 0;
                    tableRows = 1;
                    break;
                case 2:
                    topLevelNameSize = 12;
                    topLevelDetailsSize = 10;
                    col1Width = 174;
                    col2Width = 0;
                    tableRows = 2;
                    break;
                case 3:
                    topLevelNameSize = 12;
                    topLevelDetailsSize = 10;
                    col1Width = 174;
                    col2Width = 0;
                    tableRows = 3;
                    break;
                case 4:
                    topLevelNameSize = 11;
                    topLevelDetailsSize = 9;
                    col1Width = 174;
                    col2Width = 0;
                    tableRows = 4;
                    break;
                case 5:
                    topLevelNameSize = 11;
                    topLevelDetailsSize = 9;
                    break;
                case 6:
                    topLevelNameSize = 11;
                    topLevelDetailsSize = 9;
                    break;
                case 7:
                    topLevelNameSize = 11;
                    topLevelDetailsSize = 9;
                    break;
                default:
                    topLevelNameSize = 11;//7
                    topLevelDetailsSize = 9;//6
                    break;
            }
            topLevelNameStyleMale.getCharacterFormat().setFontSize(topLevelNameSize);
            topLevelNameStyleFemale.getCharacterFormat().setFontSize(topLevelNameSize);
            topLevelDetailsStyle.getCharacterFormat().setFontSize(topLevelDetailsSize);
            
            
            topLevelNameStyleMale.getCharacterFormat().setTextColor(Color.decode(UIUtilities.borderColorMale));
            topLevelNameStyleFemale.getCharacterFormat().setTextColor(Color.decode(UIUtilities.borderColorFemale));
            allLevelNameStyleMale.getCharacterFormat().setTextColor(Color.decode(UIUtilities.borderColorMale));
            allLevelNameStyleFemale.getCharacterFormat().setTextColor(Color.decode(UIUtilities.borderColorFemale));
            farmNameStyle.getCharacterFormat().setFontSize(12);
            farmAddressStyle.getCharacterFormat().setFontSize(10);
            farmEmailStyle.getCharacterFormat().setFontSize(10);
            document.getStyles().add(topLevelNameStyleMale);
            document.getStyles().add(topLevelNameStyleFemale);
            document.getStyles().add(topLevelDetailsStyle);
            document.getStyles().add(allLevelDetailsStyle);
            document.getStyles().add(farmNameStyle);
            document.getStyles().add(farmAddressStyle);
            document.getStyles().add(farmEmailStyle);
            BookmarksNavigator bn = new BookmarksNavigator(document);
            
            //replace the main level gender icon
            replaceGenderImage(document, baseItem.getSex());

            //output the Farm name and address
            //TODO: get this info from the database
            String farmName = "Breza Homestead & Rabbitry";
            String farmAddressLine1 = "RR5 Site 3 Box 21";
            String farmAddressLine2 = "Rimbey Alberta T0C 2J0";
            String farmEmail = "equidances@hotmail.ca";
            bn.moveToBookmark("farm");
            Paragraph paraFarm = getParagraph(document);
            TextRange farmNameTr = paraFarm.appendText(farmName);
            farmNameTr.getCharacterFormat().setBold(true);
            paraFarm.applyStyle(farmNameStyle);
            Paragraph paraAdd1 = getParagraph(document);
            paraAdd1.appendText(farmAddressLine1);
            paraAdd1.applyStyle(farmAddressStyle);
            Paragraph paraAdd2 = getParagraph(document);
            paraAdd2.appendText(farmAddressLine2);
            paraAdd2.applyStyle(farmAddressStyle);
            Paragraph paraEmail = getParagraph(document);
            TextRange farmEmailTr = paraEmail.appendText(farmEmail);
            farmEmailTr.getCharacterFormat().setItalic(true);
            paraEmail.applyStyle(farmEmailStyle);
            TextBodyPart farmBp = new TextBodyPart(document);
            farmBp.getBodyItems().add(paraFarm);
            farmBp.getBodyItems().add(paraAdd1);
            farmBp.getBodyItems().add(paraAdd2);
            farmBp.getBodyItems().add(paraEmail);
            
            bn.replaceBookmarkContent(farmBp,false);
            
            Integer counter = 0;
            for(Generation item: genList){
                bn.moveToBookmark("gen_" + counter.toString());
                
                //outputContent(bn, "BEFORE");
                
                String nameField = "";
                if(item.getPrefix()!=null && !item.getPrefix().isEmpty()){
                    nameField = item.getPrefix() + " ";
                }
                nameField = nameField + item.getName();
                System.out.println("createPedigreePDF: processing:" + counter + " name:" + nameField);
                Paragraph paraName = new Paragraph(document);
                paraName.getFormat().setBeforeAutoSpacing(false);
                paraName.getFormat().setAfterAutoSpacing(false);
                paraName.getFormat().setBeforeSpacing(0);
                paraName.getFormat().setAfterSpacing(0);

                paraName.appendText(nameField);

                if(item.getSex().equals(Gender.MALE)){
                    if(topLevelGenerations.contains(counter)){
                        paraName.applyStyle(topLevelNameStyleMale);
                    }else{
                        paraName.applyStyle(allLevelNameStyleMale);
                    }
                }else{
                    if(topLevelGenerations.contains(counter)){
                        paraName.applyStyle(topLevelNameStyleFemale);
                    }else{
                        paraName.applyStyle(allLevelNameStyleFemale);
                    }
                }
                
                TextBodyPart bp = new TextBodyPart(document);
                //add the name
                bp.getBodyItems().add(paraName);

                //TODO: need to add genotype
                //add detail to table if toplevel otherwise add directly to bodypart
                if(topLevelGenerations.contains(counter)){
                    Table table = new Table(document, true);
                    table.resetCells(tableRows, 2);  //TODO: change rows dependent on numer of includes
                    bp.getBodyItems().add(table);
                    try {
                        table.setColumnWidth(0, col1Width, CellWidthType.Point);
                        table.setColumnWidth(1, col2Width, CellWidthType.Point);
                    } catch (Exception ex) {
                        System.getLogger(StockPedigreeEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                    }
                    table.getTableFormat().getBorders().setBorderType(BorderStyle.None);
                    rowCounter = 0;
                    colCounter = 0;
                    addParagraphToTable(document, table, "ID", item.getTattoo(), includeId);
                    if(item.getDoB()!=null){
                        addParagraphToTable(document, table, "DOB:", item.getDoB().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),includeDob);
                    }
                    addParagraphToTable(document, table, "Colour:", item.getColor(),includeColor);
                    addParagraphToTable(document, table, "Breed:", item.getBreed(),includeBreed);
                    addParagraphToTable(document, table, "Weight:", item.getWeightInLbsOz(),includeWeight);
                    addParagraphToTable(document, table, "Legs:", item.getLegs(),includeLegs);
                    addParagraphToTable(document, table, "ChampNo:", item.getChampNo(),includeChampNo);
                    addParagraphToTable(document, table, "RegNo:", item.getRegNo(),includeRegNo);
                    
                    for(int w = 0; w < table.getRows().getCount(); w++){
                        //table.getRows().get(w).setHeight(10f);
                        //table.getRows().get(w).setHeightType(TableRowHeightType.Exactly);
                        //table.getRows().get(w).setHeight(2f);
                        //table.get(w, 0).setCellWidth(100f, CellWidthType.Point);
                        //table.get(w, 1).setCellWidth(100f, CellWidthType.Point);
                    }
                    
                    table.autoFit(AutoFitBehaviorType.Fixed_Column_Widths);
                }else{
                    //add each detail if valid and included
                    addParagraph(document, counter, bp, "ID:", item.getTattoo(), includeId);
                    if(item.getDoB()!=null){
                        addParagraph(document, counter, bp, "DOB:", item.getDoB().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),includeDob);
                    }
                    addParagraph(document, counter, bp, "Colour:", item.getColor(),includeColor);
                    addParagraph(document, counter, bp, "Breed:", item.getBreed(),includeBreed);
                    addParagraph(document, counter, bp, "Weight:", item.getWeightInLbsOz(),includeWeight);
                    addParagraph(document, counter, bp, "Legs:", item.getLegs(),includeLegs);
                    addParagraph(document, counter, bp, "ChampNo:", item.getChampNo(),includeChampNo);
                    addParagraph(document, counter, bp, "RegNo:", item.getRegNo(),includeRegNo);
                }

                
                
                bn.replaceBookmarkContent(bp,false);
                
                counter++;


            }
            
            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            document.saveToStream(pdfStream, FileFormat.PDF);
            showPedigreePDF(pdfStream, baseItem.getName());
            
        } catch (IOException e) {
            log.info("createPedigreePDF: FAILED:" + " ERROR:" + e.toString());
            e.printStackTrace();
        }
        
    }
    
    private Integer getCountIncludeFields(){
        Integer count = 0;
        if(includeBreed.getValue()) count++;
        if(includeChampNo.getValue()) count++;
        if(includeColor.getValue()) count++;
        if(includeDob.getValue()) count++;
        if(includeId.getValue()) count++;
        if(includeLegs.getValue()) count++;
        if(includeRegNo.getValue()) count++;
        if(includeWeight.getValue()) count++;
        return count;
    }
    
    private void replaceGenderImage(Document document, Gender sex){
       ArrayList<DocumentObject> pictures = new ArrayList();

        // Iterate through all section
        for (Section section : (Iterable<Section>) document.getSections()) {
            // Iterate through all tables in the section
            for (Table table : (Iterable<Table>) section.getTables()) {
                // Iterate through all rows in the table
                for (TableRow row : (Iterable<TableRow>) table.getRows()) {
                    // Iterate through all cells in the row
                    for (TableCell cell : (Iterable<TableCell>) row.getCells()) {
                        // Iterate through all paragraphs in the cell
                        for (Paragraph para : (Iterable<Paragraph>) cell.getParagraphs()) {
                            // Check each child object in the paragraph
                            for (DocumentObject obj : (Iterable<DocumentObject>) para.getChildObjects()) {
                                //Find the images and add them to the list
                                if (obj.getDocumentObjectType() == DocumentObjectType.Picture) {
                                    pictures.add(obj);
                                }
                            }
                        }
                    }
                }
            }
        }
        //Replace the sixth picture in the list with a new image
        Integer imageToReplace = 6;
        if(pictures.size()>imageToReplace){
            DocPicture picture = (DocPicture)pictures.get(imageToReplace) ;
            InputStream inGenderFemale;
            InputStream inGenderMale;
            try {
                if(sex.equals(Gender.MALE)){
                    inGenderMale = resourceGenderMale.getInputStream();
                    picture.loadImage(inGenderMale);
                }else{
                    inGenderFemale = resourceGenderFemale.getInputStream();
                    picture.loadImage(inGenderFemale);
                }
                picture.setWidth(14);
                picture.setHeight(14);
            } catch (IOException ex) {
                System.getLogger(StockPedigreeEditor.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
        
    }
    
    private Paragraph getParagraph(Document document){
        Paragraph para = new Paragraph(document);
        para.getFormat().setBeforeAutoSpacing(false);
        para.getFormat().setAfterAutoSpacing(false);
        para.getFormat().setBeforeSpacing(0);
        para.getFormat().setAfterSpacing(0);
        return para;
    }
    
    private void addParagraph(Document document, Integer counter, TextBodyPart bp, String itemLabel, String itemValue){
        addParagraph(document, counter, bp, itemLabel, itemValue, null);
    }
    
    private void addParagraph(Document document, Integer counter, TextBodyPart bp, String itemLabel, String itemValue, Checkbox itemInclude){
        
        if(itemInclude==null || (itemInclude!=null && itemInclude.getValue())){
            Paragraph para = new Paragraph(document);
            para.setText(itemLabel + ":" + itemValue);
            bp.getBodyItems().add(para);
            //para.getFormat().setAfterSpacing(0);
            //para.getFormat().setBeforeSpacing(0);

            if(topLevelGenerations.contains(counter)){
                para.applyStyle(topLevelDetailsStyle);
            }else{
                para.applyStyle(allLevelDetailsStyle);
            }
        }
        
    }
    
    private void addParagraphToTable(Document document, Table table, String itemLabel, String itemValue, Checkbox itemInclude){
        System.out.println("addParagraphToTable: row:" + rowCounter + " col:" + colCounter + " label:" + itemLabel);
        if(itemInclude==null || (itemInclude!=null && itemInclude.getValue())){
            Paragraph para = table.get(rowCounter, colCounter).addParagraph();
            para.getFormat().setBeforeAutoSpacing(false);
            para.getFormat().setAfterAutoSpacing(false);
            para.getFormat().setBeforeSpacing(0);
            para.getFormat().setAfterSpacing(0);
            para.setText(itemLabel + ":" + itemValue);
            para.applyStyle(topLevelDetailsStyle);
            rowCounter++;
            if(rowCounter==tableRows){
                rowCounter=0;
                colCounter++;
            }
        }
        
    }
    
    @Override
    public void listRefreshNeeded() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getPageTitle() {
        System.out.println("getPageTitle: called");
        if(hasStock) return "Pedigree for:" + stock.getDisplayName();
        return "No Pedigree available";
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        System.out.println("setParameter: called with:" + parameter);
        if(parameter==null){
            hasStock = Boolean.FALSE;
        }else{
            stock = stockService.findById(Integer.valueOf(parameter));
            if(stock==null) {
                hasStock = Boolean.FALSE;
            }else{
                hasStock = Boolean.TRUE;
                viewStockType = stock.getStockType();
                name = stock.getDisplayName();
            }
        }
        add(createContent());
    }

    private void showPedigreePDF(ByteArrayOutputStream pdfStream, String name){
        Dialog pdfViewerDialog = new Dialog();
        pdfViewerDialog.setResizable(true);
        pdfViewerDialog.setSizeFull();
        pdfViewerDialog.setHeaderTitle("Pedigree PDF Viewer:" + name);
        VerticalLayout dialogLayout = UIUtilities.getVerticalLayout(true, true, false);
        //pdfViewerDialog.add(dialogLayout);
        Button cancelButton = new Button("Cancel", e -> pdfViewerDialog.close());
        pdfViewerDialog.getFooter().add(cancelButton);
        PdfViewer pdfViewer = new PdfViewer();
        pdfViewerDialog.add(pdfViewer);
        pdfViewer.setSizeFull();
        pdfViewer.setCustomTitle(pdfFileName);
        pdfViewer.setAddPrintButton(true);
        pdfViewer.setZoom("page-fit");

        byte[] reportBytes = pdfStream.toByteArray();
        pdfViewer.setSrc(DownloadHandler.fromInputStream(callback ->{
            try {
                return new DownloadResponse(
                    new ByteArrayInputStream(reportBytes),
                    pdfFileName,
                    "application/pdf",
                    reportBytes.length
                );
            } catch (Exception e) {
                System.out.println("ERROR");
                return DownloadResponse.error(500);
            }            
        }));
        //dialogLayout.add(pdfViewer);
        pdfViewerDialog.open();
    }
    
    private void createPedigreeReport() {
        /*
        List<Stock> breeders = stockService.findAllBreeders();
        Collections.sort(breeders, new StockComparator());
        for(Stock breeder: breeders){
            Integer level = 0;
            System.out.println("BREEDER: " + breeder.getDisplayName());
            level++;
            outputParent(breeder.getFatherId(), level, Gender.MALE);
            outputParent(breeder.getMotherId(), level, Gender.FEMALE);
        }
        */
        Integer counter = 0;
        for(Generation gen: genList){
            System.out.println("Gen:" + counter + " Name:" + gen.getName());
            counter++;
        }
        
    }
    
    private Boolean outputParent(Integer id, Integer level, Gender gender){
        Stock parent = stockService.findById(id);
        if(parent==null) return false;
        String spacer = " ----".repeat(level);
        String parentType;
        if(gender.equals(Gender.MALE)) parentType = "Father";
        else parentType = "Mother";
        System.out.println(spacer + parentType + parent.getDisplayName());
        outputParent(parent.getFatherId(), level+1, Gender.MALE);
        outputParent(parent.getMotherId(), level+1, Gender.FEMALE);
        return true;
    }

}
