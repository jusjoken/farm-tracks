/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.stock;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.WeightInput;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.Generation;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.views.MainLayout;
import com.flowingcode.vaadin.addons.badgelist.Badge;
import com.flowingcode.vaadin.addons.badgelist.BadgeList;
import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.spire.doc.AutoFitBehaviorType;
import com.spire.doc.CellWidthType;
import com.spire.doc.Document;
import com.spire.doc.DocumentObject;
import com.spire.doc.FileFormat;
import com.spire.doc.Section;
import com.spire.doc.Table;
import com.spire.doc.TableCell;
import com.spire.doc.TableRow;
import com.spire.doc.documents.BookmarksNavigator;
import com.spire.doc.documents.BorderStyle;
import com.spire.doc.documents.DocumentObjectType;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ParagraphStyle;
import com.spire.doc.documents.TextBodyPart;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.fields.TextRange;
import com.vaadin.componentfactory.pdfviewer.PdfViewer;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayoutVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializableConsumer;
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
    //private String name = Utility.EMPTY_VALUE;
    private List<Generation> genList;
    private TreeData<Generation> treeData = new TreeData<>();
    private TreeGrid<Generation> tree = new TreeGrid<>();
    private Integer selectedGenerationIndex = null;
    private Generation selectedGeneration;
    private final MasterDetailLayout mdLayout = new MasterDetailLayout();
    private final File appPath = new File(System.getProperty("user.dir"));
    private final File outputDir = new File(appPath,"output");
    //private File pdfFile = null;
    private String pdfFileName = "";
    @Value("classpath:Pedigree_Template.docx")
    private final Resource resourcePedigreeTemplate;
    @Value("classpath:Pedigree_Template_portrait.docx")
    private final Resource resourcePedigreeTemplatePortrait;
    @Value("classpath:/META-INF/resources/images/gender_male.png")
    private final Resource resourceGenderMale;
    @Value("classpath:/META-INF/resources/images/gender_female.png")
    private final Resource resourceGenderFemale;
    private final Logger log = LoggerFactory.getLogger(StockPedigreeEditor.class);
    //all include options
    private final FormLayout settingsForm = new FormLayout();
    private final DialogCommon dialogCommon;

    private final Checkbox includeDob = new Checkbox();
    private final Checkbox includeColor = new Checkbox();
    private final Checkbox includeBreed = new Checkbox();
    private final Checkbox includeWeight = new Checkbox();
    private final Checkbox includeGeno = new Checkbox();
    private final Checkbox includeId = new Checkbox();
    private final Checkbox includeLegs = new Checkbox();
    private final Checkbox includeRegNo = new Checkbox();
    private final Checkbox includeChampNo = new Checkbox();

    private final List<Integer> topLevelGenerations = new ArrayList<>();
    private ParagraphStyle topLevelDetailsStyle;
    private ParagraphStyle allLevelDetailsStyle;
    private Integer rowCounter = 0;
    private Integer colCounter = 0;
    private Integer tableRows = 5;

    public StockPedigreeEditor(StockService stockService) {
        this.stockService = stockService;
        this.resourcePedigreeTemplate = new ClassPathResource("Pedigree_Template.docx");
        this.resourcePedigreeTemplatePortrait = new ClassPathResource("Pedigree_Template_portrait.docx");
        this.resourceGenderMale = new ClassPathResource("/META-INF/resources/images/gender_male.png");
        this.resourceGenderFemale = new ClassPathResource("/META-INF/resources/images/gender_female.png");
        this.dialogCommon = new DialogCommon();
        dialogCommon.addListener(this);
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
            mdLayout.setMaster(createPedigree());
            mdLayout.setDetail(null);
        }
        return mdLayout;
    }
    
    private Component createPedigree(){
        VerticalLayout layout = UIUtilities.getVerticalLayout(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
        layout.setSizeFull();
        
        tree = new TreeGrid<>();
        treeData = new TreeData<>();

        tree.setEmptyStateText("Select an item above to retrieve the pedigree.");
        
        tree.setSelectionMode(Grid.SelectionMode.SINGLE);
        
        tree.asSingleSelect().addValueChangeListener(event -> {
            Generation selected = event.getValue();
            if(selected != null){
                selectedGeneration = selected;
                selectedGenerationIndex = genList.indexOf(selectedGeneration);
                mdLayout.setDetail(createItemEditor(selected));
            }else{
                selectedGeneration = null;
                selectedGenerationIndex = null;
                mdLayout.setDetail(null);
            }
        });
        
        TreeDataProvider<Generation> treeDataProvider = new TreeDataProvider<>(treeData, HierarchicalDataProvider.HierarchyFormat.FLATTENED);
        tree.setDataProvider(treeDataProvider);
        layout.add(tree);
        
        if(hasStock) genList = loadGenerations();

        if(selectedGenerationIndex==null){
            mdLayout.setDetail(null);
        }else{
            Generation selected = genList.get(selectedGenerationIndex);
            //mdLayout.setDetail(createItemEditor(selected));
            tree.select(selected);
            //System.out.println("SCROLLING to selected:" + selected.getName());
            runBeforeClientResponse(ui -> tree.scrollToItem(selected));   
        }
        
        FormLayout headerForm = new FormLayout();
        headerForm.setLabelSpacing("0px");
        headerForm.setColumnWidth(50, Unit.EM);
        Select<Stock> stockSelect = new Select<>();
        
        stockSelect.setItems(stockService.findAllBreeders());
        stockSelect.setItemLabelGenerator(Stock::getDisplayNameWithStatus);
        stockSelect.setValue(stock);
        stockSelect.addValueChangeListener(item -> {
            stock = item.getValue();
            selectedGeneration = null;
            selectedGenerationIndex = null;
            buildView(stock);
        });
        headerForm.addFormItem(stockSelect, "Pedigree for:");
        
        //open details button
        Button detailsButton = new Button("Details/PDF Options");
        detailsButton.addClickListener(click -> {
            if(mdLayout.getDetail()==null){
                mdLayout.setDetail(createItemEditor(genList.get(0)));
            }else{
                mdLayout.setDetail(null);
            }
        });
        if(hasStock) headerForm.add(detailsButton);
        
        //tree design
        Column<Generation> treeColumn = tree.addComponentHierarchyColumn(gen -> {
            return new Div("");
        }).setWidth("100px").setFlexGrow(0);

        Column<Generation> mainColumn = tree.addComponentColumn(Generation::getHeader).setHeader("").setWidth("400px").setFlexGrow(1);
        HeaderRow headerRow = tree.prependHeaderRow();
        headerRow.join(treeColumn,mainColumn).setComponent(headerForm);

        tree.setHeightFull();
        tree.addCollapseListener(listener -> {
            //the following forces all nodes to be expanded all the time and diables collapse
            tree.expand(genList);
        });
        treeDataProvider.refreshAll();
        if(hasStock) tree.expand(genList);

        return layout;
    }
    
    private void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this, context -> command.accept(ui)));
    }       
    
    private Component createItemEditor(Generation item) {
        VerticalLayout editorLayout = UIUtilities.getVerticalLayout(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        //editorLayout.setThemeVariants(VerticalLayoutVariant.LUMO_SPACING_S);
        VerticalLayout editor = UIUtilities.getVerticalLayout(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
        editor.setSizeFull();
        Scroller scroller = new Scroller(editor);
        //scroller.setMaxHeight("500px");
        scroller.setSizeFull();
        editorLayout.setSizeFull();
        editorLayout.setFlexGrow(1, scroller);

        HorizontalLayout header = UIUtilities.getHorizontalLayout(false, true, false);
        header.setAlignItems(FlexComponent.Alignment.END);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        
        //PDF output settings
        FormLayout settingsForm = getFormLayout();

        //pdf button
        Button pdfButton = new Button("PDF", FontAwesome.Solid.FILE_PDF.create());
        pdfButton.setTooltipText("View pedigree as PDF");
        pdfButton.getElement().addEventListener("click", click -> {
            createPedigreePDF(item);
        }).addEventData("event.stopPropagation()");        
        
        Button editCurrent = new Button(FontAwesome.Solid.PENCIL_ALT.create());
        editCurrent.setTooltipText("Edit " + item.getName());
        editCurrent.getElement().addEventListener("click", click -> {
            System.out.println("createParentSelect: edit button clicked:" + click);
            //open stock edit dialog
            dialogCommon.setDialogTitle("Edit Stock");
            dialogCommon.dialogOpen(item.getStock(),DialogCommon.DisplayMode.STOCK_DETAILS);
        }).addEventData("event.stopPropagation()");
        if(item.getStock().isTemp()){
            editCurrent.setEnabled(false);
        }else{
            editCurrent.setEnabled(true);
        }
        
        
        //close button
        Button closeButton = new Button(FontAwesome.Solid.X.create());
        closeButton.setTooltipText("Close details");
        closeButton.getElement().addEventListener("click", click -> {
            mdLayout.setDetail(null);
        }).addEventData("event.stopPropagation()");
        
        HorizontalLayout editorToolbarLayout = UIUtilities.getHorizontalLayout(false, true, false);
        editorToolbarLayout.setThemeVariants(HorizontalLayoutVariant.LUMO_SPACING_XS);
        editorToolbarLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        editorToolbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        editorToolbarLayout.add(editCurrent,closeButton);
        editorToolbarLayout.setWidthFull();

        HorizontalLayout pdfLayout = UIUtilities.getHorizontalLayout(false, true, false);
        pdfLayout.setThemeVariants(HorizontalLayoutVariant.LUMO_SPACING_XS);
        pdfLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        pdfLayout.add(new Span("PDF Settings"), pdfButton);
        pdfLayout.setWidthFull();

        HorizontalLayout detailSummary = UIUtilities.getHorizontalLayout(false, true, false);
        detailSummary.setAlignItems(FlexComponent.Alignment.BASELINE);
        //detailSummary.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        detailSummary.addToStart(pdfLayout);
        detailSummary.addToEnd(editorToolbarLayout);
        detailSummary.setWidthFull();
        //detailSummary.add(pdfLayout, closeSaveLayout);
        Details pdfSettings = new Details(detailSummary,settingsForm);
        pdfSettings.setWidthFull();
        //header.add(pdfSettings);
        
        editorLayout.add(pdfSettings);
        
        //add breadcrumbs to show path to current selection
        List<Badge> badges = new ArrayList<>();
        for(Generation badgeItem: item.getBreadcrumbs()){
            Badge newBadge = new Badge(badgeItem.getName());
            UIUtilities.setBorders(newBadge, badgeItem.getStock(), UIUtilities.BorderSize.XSMALL);
            newBadge.addClickListener(click -> {
                tree.select(badgeItem);
            });
            badges.add(newBadge);
        }
        BadgeList badgeList = new BadgeList(badges);
        badgeList.setWidthFull();
        //Div div = new Div(badgeList);
        HorizontalLayout badgeLayout = UIUtilities.getHorizontalLayout(false, false, false);
        badgeLayout.add(badgeList);
        editorLayout.add(badgeLayout);
        editorLayout.add(scroller);
        
        //show parent selection for the child record
        Stock childStock = null;
        String parentTypeAction = "";
        Boolean showParentSelect = Boolean.FALSE;
        Boolean showDetails = Boolean.FALSE;
        Boolean showDetailsReadOnly = Boolean.FALSE;

        String parentType = "";
        String parentStockTypeName = "";
        if(item.getSex().equals(Gender.MALE)){
            parentType = "Father";
            parentStockTypeName = viewStockType.getMaleName();
        }else{
            parentType = "Mother";
            parentStockTypeName = viewStockType.getFemaleName();
        }
        
        if(item.getChild()!=null && item.getChild().getStock().isTemp()){
            //previous level should be completed first
            Span noOptions = new Span("Previous pedigree level must be completed first!");
            editor.add(noOptions);
        }else{
            //provide edit options
            parentTypeAction = "Select existing";
            if(item.getChild()!=null) childStock = item.getChild().getStock();

            if(childStock==null){
                //root item selected as no child
                System.out.println("createItemEditor: childstock is NULL");
                editor.add(createDetailsLayout(item, true));
            }else{
                if(item.getStock().isTemp()){
                    parentTypeAction = "Select an existing";
                    editor.add(createParentSelect(item, childStock, parentTypeAction, parentType, parentStockTypeName));
                    Span noParent = new Span(parentType + " unknown.  Select an existing " + parentStockTypeName + " above or add a new external " + parentStockTypeName + " using the add (+) function.");
                    editor.add(new Hr(), noParent);
                }else{
                    parentTypeAction = "Change existing";
                    editor.add(createParentSelect(item, childStock, parentTypeAction, parentType, parentStockTypeName));
                    editor.add(createDetailsLayout(item, true));
                }
            }
            
        }

        System.out.println("createItemEditor: showParentSelect:" + showParentSelect + " showDetails:" + showDetails + " showDetailsRO:" + showDetailsReadOnly);
        
        return editorLayout;
    }
    
    private FormLayout getFormLayout(){
        //FormLayout settingsForm = new FormLayout();
        settingsForm.removeAll();

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
        
        addIncludeField(settingsForm, includeGeno, "includeGeno", "Geno");
        addIncludeField(settingsForm, includeId, "includeTattoo", "ID");
        addIncludeField(settingsForm, includeDob, "includeDob", "DOB");
        addIncludeField(settingsForm, includeColor, "includeColour", "Colour");
        addIncludeField(settingsForm, includeBreed, "includeBreed", "Breed");
        addIncludeField(settingsForm, includeWeight, "includeWeight", "Weight");
        addIncludeField(settingsForm, includeLegs, "includeLegs", "Legs");
        addIncludeField(settingsForm, includeChampNo, "includeChampNo", "ChampNo");
        addIncludeField(settingsForm, includeRegNo, "includeRegNo", "RegNo");
        return settingsForm;
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
    
    private void removeParent(Stock stockToSave, Gender sex){
        if(stockToSave==null){
            System.out.println("removeParent: Could not remove parent as stock was NULL: stock:" + stockToSave);
        }else{
            if(sex.equals(Gender.MALE)){
                stockToSave.setFatherId(null);
            }else{
                stockToSave.setMotherId(null);
            }
            stockService.save(stockToSave);
        }
    }
    
    private VerticalLayout createParentSelect(Generation item, Stock childStock, String parentTypeAction, String parentType, String parentStockTypeName){
        VerticalLayout parentLayoutV = UIUtilities.getVerticalLayout(false, false, false);
        HorizontalLayout parentLayout = UIUtilities.getHorizontalLayout(false, true, false);
        parentLayout.setThemeVariants(HorizontalLayoutVariant.LUMO_SPACING_XS);
        parentLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        Select<Stock> parentSelect = new Select<>();
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
        }else{
            parentSelect.setItems(stockService.getMothers(childStock.getMotherExtName(),childStock.getStockType()));
        }
        Span layoutLabel = new Span(parentTypeAction + " " + parentType + " for:" + childStock.getDisplayName());
        parentSelect.setValue(item.getStock());
        System.out.println("createParentSelect: item name:" + item.getName() + " actionStr:" + parentTypeAction + " setValue to:" + childStock);
        parentSelect.addValueChangeListener(selection -> {
            selectedParent = selection.getValue();
            //System.out.println("createItemEditor: SAVE: item name" + item.getChild().getStock().getDisplayName() + " selected parent:" + selectedParent.getDisplayName() + " Gender:" + item.getSex());
            saveParent(item.getChild().getStock(), selectedParent, item.getSex());
            buildView(stock);
        });
        Icon removeIcon = new Icon(FontAwesome.Solid.MINUS.getIconName());
        Button removeParent = new Button(removeIcon);
        removeParent.addClickListener(event -> {
            System.out.println("createParentSelect: remove button clicked:" + event);
            //TODO: confirm delete then remove and save the parent
            removeParent(item.getChild().getStock(),item.getSex());
            buildView(stock);
        });
        if(item.getStock().isTemp()){
            removeIcon.setColor("light gray");
            removeParent.setTooltipText("Unable to remove parent as none selected.");
            removeParent.setEnabled(false);
        }else{
            removeIcon.setColor("red");
            removeParent.setTooltipText("Remove " + item.getName() + " as " + parentType);
            removeParent.setEnabled(true);
        }
        Button addParent = new Button(FontAwesome.Solid.PLUS.create());
        addParent.setTooltipText("Add new external " + parentStockTypeName + " and set as " + parentType);
        addParent.addClickListener(event -> {
            Stock external = new Stock();
            external.setExternal(true);
            external.setBreeder(true);
            external.setSex(item.getSex());
            external.setStockType(viewStockType);
            external.setWeight(0);
            
            dialogCommon.setDialogTitle("Create new");
            dialogCommon.dialogOpen(external,DialogCommon.DisplayMode.STOCK_DETAILS);
        });
        parentLayoutV.add(layoutLabel,parentLayout);
        parentLayout.add(parentSelect, removeParent, addParent);
        return parentLayoutV;
    }
    
    private FormLayout createDetailsLayout(Generation item, Boolean showDetailsReadOnly){
        System.out.println("createDetailsLayout: item name:" + item.getName() + " RO:" + showDetailsReadOnly);
        FormLayout formLayout = new FormLayout();
        
        TextField prefixField = new TextField();
        formLayout.addFormItem(prefixField, "Prefix");
        prefixField.setValue(item.getPrefix());
        TextField nameField = new TextField();
        formLayout.addFormItem(nameField, "Name");
        nameField.setValue(item.getName());
        TextField genoField = new TextField();
        formLayout.addFormItem(genoField, "Geno");
        genoField.setValue(item.getGenotype());
        TextField tattooField = new TextField();
        formLayout.addFormItem(tattooField, "ID");
        tattooField.setValue(item.getTattoo());
        DatePicker doBField = new DatePicker();
        formLayout.addFormItem(doBField, "DOB");
        doBField.setValue(item.getDoB());
        TextField colorField = new TextField();
        formLayout.addFormItem(colorField, "Color");
        colorField.setValue(item.getColor());
        TextField breedField = new TextField();
        formLayout.addFormItem(breedField, "Breed");
        breedField.setValue(item.getBreed());
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
                            ((AbstractField<?, ?>) formItemField).setReadOnly(true);
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
        Stock genStock = new Stock(Utility.EMPTY_VALUE ,Boolean.TRUE, viewStockType);
        genStock.setSex(sex);
        return genStock;
    }
    
    private void createPedigreePDF(Generation baseItem){
        System.out.println("createPedigreePDF: baseItem:" + baseItem);
        pdfFileName = "Pedigree_" + stock.getDisplayName() + ".pdf";
        
        try {
            Files.createDirectories(outputDir.toPath());
            System.out.println("Directory created: " + outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
        }        
        
        //File outputFile = new File(outputDir,pdfFileName);
        //pdfFile = outputFile;
        try {
            
            InputStream in = resourcePedigreeTemplate.getInputStream();
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
                
                System.out.println("createPedigreePDF: Geno:" + item.getGenotype());
                

                //add Geno as a paragraph as it typically takes full width
                addParagraph(document, counter, bp, "Geno:", item.getGenotype(),includeGeno);
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
                    if(item.getDoB()==null){
                        addParagraphToTable(document, table, "DOB:", "",includeDob);
                    }else{
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
                    if(item.getDoB()==null){
                        addParagraph(document, counter, bp, "DOB:", "",includeDob);
                    }else{
                        addParagraph(document, counter, bp, "DOB:", item.getDoB().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),includeDob);
                    }
                    addParagraph(document, counter, bp, "Colour:", item.getColor(),includeColor);
                    addParagraph(document, counter, bp, "Breed:", item.getBreed(),includeBreed);
                    addParagraph(document, counter, bp, "Weight:", item.getWeightInLbsOz(),includeWeight);
                    addParagraph(document, counter, bp, "Geno:", item.getGenotype(),includeGeno);
                    addParagraph(document, counter, bp, "Legs:", item.getLegs(),includeLegs);
                    addParagraph(document, counter, bp, "ChampNo:", item.getChampNo(),includeChampNo);
                    addParagraph(document, counter, bp, "RegNo:", item.getRegNo(),includeRegNo);
                }

                
                
                bn.replaceBookmarkContent(bp,false);
                
                counter++;


            }
            
            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            document.saveToStream(pdfStream, FileFormat.PDF);
            showPedigreePDF(pdfStream);
            
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
    
    @SuppressWarnings("unchecked")
    private void replaceGenderImage(Document document, Gender sex){
       ArrayList<DocumentObject> pictures = new ArrayList<>();

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
        //System.out.println("addParagraphToTable: row:" + rowCounter + " col:" + colCounter + " label:" + itemLabel);
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
        if(dialogCommon.getReturnId()!=null){
            //new parent has been created so set the parent
            Stock childToUpdate = selectedGeneration.getChild().getStock();
            if(selectedGeneration.getSex().equals(Gender.MALE)){
                childToUpdate.setFatherId(dialogCommon.getReturnId());
            }else{
                childToUpdate.setMotherId(dialogCommon.getReturnId());
            }
            stockService.save(childToUpdate);
            System.out.println("listRefreshNeeded: selected:" + selectedGeneration.getName());
            System.out.println("listRefreshNeeded: selected Child:" + selectedGeneration.getChild().getName());
            
        }
        buildView(stock);
    }

    @Override
    public String getPageTitle() {
        System.out.println("getPageTitle: called");
        return "Pedigree Manager";
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        System.out.println("setParameter: called with:" + parameter);
        if(parameter!=null){
            stock = stockService.findById(Integer.valueOf(parameter));
        }else{
            stock = null;
        }
        buildView(stock);
    }

    private void buildView(Stock currentStock){
        if(currentStock==null){
            hasStock = Boolean.FALSE;
            
        }else{
            hasStock = Boolean.TRUE;
            viewStockType = stock.getStockType();
            //name = stock.getDisplayName();
        }
        removeAll();
        add(createContent());
    }
    
    private void showPedigreePDF(ByteArrayOutputStream pdfStream){
        Dialog pdfViewerDialog = new Dialog();
        pdfViewerDialog.setResizable(true);
        pdfViewerDialog.setSizeFull();
        pdfViewerDialog.setHeaderTitle("Pedigree PDF Viewer:" + stock.getDisplayName());
        Button cancelButton = new Button(FontAwesome.Solid.X.create());
        cancelButton.setTooltipText("Close");
        cancelButton.addClickListener(e -> {
            pdfViewerDialog.close();
        });
        pdfViewerDialog.getHeader().add(cancelButton);
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
        pdfViewerDialog.open();
    }
    
}
