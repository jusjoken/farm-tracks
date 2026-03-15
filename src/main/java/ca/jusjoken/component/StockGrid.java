package ca.jusjoken.component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockSavedQuery.StockViewStyle;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.StockWeightHistoryService;
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.views.stock.StockPedigreeEditor;

public class StockGrid extends Grid<Stock> implements ListRefreshNeededListener{

    private ListDataProvider<Stock> dataProvider;
    private Integer stockId;
    private Integer litterId;
    private Stock stock;
    private StockType stockType;
    private final StockService stockService;
    private final StockTypeService stockTypeService;
    private final LitterService litterService;
    private StockSavedQuery currentStockSavedQuery;
    private String currentSearchName;
    private final DialogCommon dialogCommon;
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private final List<SidebarChangedListener> sidebarChangedListeners = new ArrayList<>();
    private final StockStatusHistoryService statusService;
    private final StockWeightHistoryService weightService;
    private final StatusEditor statusEditor;
    private final WeightEditor weightEditor;
    private final GenotypeEditor genotypeEditor;
    private final TaskEditor taskEditor;
    private final PlanEditor planEditor;
    private List<Column<Stock>> columnList;
    private Boolean menuCreated = false;
    private Boolean displayAsTile = false;
    private Boolean valueLayout = false;

    public static enum StockGridType {
        LITTER, STOCK, KITS
    }

    private StockGridType stockGridType = StockGridType.KITS;

    public StockGrid() {
        super(Stock.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.statusService = Registry.getBean(StockStatusHistoryService.class);
        this.weightService = Registry.getBean(StockWeightHistoryService.class);
        this.displayAsTile = false;
        this.valueLayout = false;

        this.dialogCommon = new DialogCommon();
        this.statusEditor = new StatusEditor();
        this.weightEditor = new WeightEditor();
        this.genotypeEditor = new GenotypeEditor();
        this.taskEditor = new TaskEditor();
        this.planEditor = new PlanEditor();
        setupListeners();
    }

    private void setupListeners() {
        dialogCommon.addListener(this);
        statusEditor.addListener(this);
        weightEditor.addListener(this);
        genotypeEditor.addListener(this);
        taskEditor.addListener(this);
        planEditor.addListener(this);
    }

    public void createGrid() {
        //only configure grid after stockId or litterId is set and stockType is determined
        if(stockId != null || litterId != null || stockGridType == StockGridType.STOCK){
            configureGrid();
            refreshGrid();
            setColumns();
            addColumnReorderListener(event -> {
                columnList = getCleanColumnList(event.getColumns());
                notifySidebarChanged(true);
            });
            if(!menuCreated){
                createContextMenu(this);
                menuCreated = true;
            }
        }
    }

    private void setColumns(){
        columnList = getCleanColumnList(getColumns());
        // System.out.println("applyFilters: columnList:" + columnList);
        //retreive the visible columns and set them
        if(currentStockSavedQuery != null && !currentStockSavedQuery.getVisibleColumnKeyList().isEmpty()){
            // System.out.println("applyFilters: setting visible columns to:" + currentStockSavedQuery.getVisibleColumnKeyList());
            for(Column<Stock> column: columnList){
                getColumnByKey(column.getKey()).setVisible(currentStockSavedQuery.getVisibleColumnKeyList().contains(column.getKey()));
            }
        }
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();

        if(displayAsTile && valueLayout){
            configureValueTileView();
        }else if(displayAsTile && !valueLayout){
            configureTileView();
        }else if(!displayAsTile && valueLayout){
            configureValueView();
        }else{
            configureListView();
        }

        setEmptyStateText("No data available to display");

    }

    private void configureValueView() {
        setColumnReorderingAllowed(false);

        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(false).setKey("name");

        addColumn(Stock::getBreed)
                .setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed");

        addColumn(Stock::getStatus)
                .setHeader("Status").setResizable(true).setAutoWidth(true).setKey("status");

        addComponentColumn(stockEntity -> { return stockEntity.getSaleStatusBadge(false); }).setHeader("Sale Status").setResizable(true).setAutoWidth(true).setKey("salestatus");

        addColumn(Stock::getInvoiceNumber)
                .setHeader("Invoice").setResizable(true).setAutoWidth(true).setKey("invoice");

        addColumn(new LocalDateTimeRenderer<>(Stock::getStatusDate,"MM-dd-YYYY HHmm"))
                .setHeader("Status Date").setResizable(true).setAutoWidth(true).setKey("statusdate");

        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00"))
                .setHeader("Value")
                .setResizable(true)
                .setAutoWidth(true)
                .setKey("value")
                .setFooter(getValueFooter())
                .setTextAlign(ColumnTextAlign.END)
                .setFrozenToEnd(true);

    }

    private String getValueFooter() {
        List<Stock> currentStockList;
        if(stockGridType == StockGridType.STOCK){
            currentStockList = stockService.listByExample(currentSearchName, currentStockSavedQuery);
        } else {
            if(this.dataProvider == null){
                currentStockList = List.of();
            }else{      
                currentStockList = dataProvider.getItems().stream().toList();
            }
        }

        // System.out.println("getValueFooter: count:" + currentStockList.size());
        Double totalValue = 0.0;
        for (Stock item: currentStockList) {
            totalValue = totalValue + item.getStockValue();
        }
        NumberFormat usFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return "Total value: " + usFormat.format(totalValue);
    }



    private void configureTileView() {
        addColumn(stockCardRenderer).setKey("name");
    }

    private void configureValueTileView() {
        addColumn(stockValueCardRenderer).setKey("name").setFooter(getValueFooter());
    }

    private void configureListView() {
        // Additional configuration such as columns, themes, etc.
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        //setHeight("270px");
        setColumnReorderingAllowed(true);
        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(false).setKey("name");

        if(stockGridType == StockGridType.STOCK){
            addComponentColumn(stockEntity -> {
                        if (stockEntity.getBreeder()) {
                            return new Icon(Utility.ICONS.TYPE_BREEDER.getIconSource());
                        } else {
                            return null;
                        }
                    }).setHeader("Breeder").setResizable(true).setAutoWidth(true).setKey("breeder");   
        }

        addColumn(Stock::getBreed).setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed");
        addColumn(Stock::getColor).setHeader("Colour").setResizable(true).setAutoWidth(true).setKey("color");
        addColumn(Stock::getGenotype).setHeader("Genotype").setResizable(true).setAutoWidth(true).setKey("genotype");

        if(stockGridType == StockGridType.STOCK){
            addColumn(Stock::getChampNo).setHeader("ChampNo").setResizable(true).setAutoWidth(true).setKey("champno");
            addColumn(Stock::getLegs).setHeader("Legs").setResizable(true).setAutoWidth(true).setKey("legs");
            addColumn(Stock::getRegNo).setHeader("RegNo").setResizable(true).setAutoWidth(true).setKey("regno");
        }

        addColumn(Stock::getStatus).setHeader("Status").setResizable(true).setAutoWidth(true).setKey("status");
        addColumn(new LocalDateTimeRenderer<>(Stock::getStatusDate,"MM-dd-YYYY HHmm")).setHeader("Status Date").setResizable(true).setAutoWidth(true).setKey("statusdate");
        addColumn(Stock::getTattoo).setHeader("Tattoo").setResizable(true).setAutoWidth(true).setKey("tattoo");
        addColumn(Stock::getWeightInLbsOzAsString).setHeader("Weight").setResizable(true).setAutoWidth(true).setKey("weight");
        addColumn(new LocalDateRenderer<>(Stock::getAcquired,"MM-dd-YYYY")).setHeader("Aquired").setResizable(true).setAutoWidth(true).setKey("aquired");
        addColumn(new LocalDateRenderer<>(Stock::getDoB,"MM-dd-YYYY")).setHeader("DOB").setResizable(true).setAutoWidth(true).setKey("dob");
        addColumn(stockEntity -> {
            return stockEntity.getAge().getAgeFormattedString();
        }).setHeader("Age").setResizable(true).setAutoWidth(true).setKey("age");
        addColumn(stockEntity -> {
            Stock parent = stockService.getFather(stockEntity);
            if(parent==null) return null;
            return parent.getDisplayName();
        }).setHeader("Father").setResizable(true).setAutoWidth(true).setKey("father");
        addColumn(stockEntity -> {
            Stock parent = stockService.getMother(stockEntity);
            if(parent==null) return null;
            return parent.getDisplayName();
        }).setHeader("Mother").setResizable(true).setAutoWidth(true).setKey("mother");
        addColumn(stockEntity -> {return stockTypeService.getGenderForType(stockEntity.getSex(), stockEntity.getStockType());}).setHeader("Gender").setResizable(true).setAutoWidth(true).setKey("gender");
        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00")).setHeader("Value").setResizable(true).setAutoWidth(true).setKey("value");
        addColumn(Stock::getNotes).setHeader("Notes").setResizable(true).setAutoWidth(true).setKey("notes");

    }

    private final ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stockEntity -> {
                // return createListItemLayout(stockEntity);
                return createListItemCard(stockEntity);
            });    

    private Card createListItemCard(Stock stock){
        Card card = new Card();
        card.setWidthFull();
        Avatar avatar = stock.getAvatar(false).getAvatar();
        avatar.getElement().addEventListener("click", click -> {
            //open image dialog
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,DialogCommon.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");  
        
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeaderPrefix(avatar);
        card.setHeader(stock.getHeader());

        String stockTattoo = stock.getTattoo();
        if(!stockTattoo.isEmpty() && !stockTattoo.equals(stock.getDisplayName())){
            card.addToFooter(UIUtilities.createBadge(null,stockTattoo, BadgeVariant.SUCCESS));
        }

        Badge breedBadge = UIUtilities.createBadge(null,stock.getBreed(), BadgeVariant.SUCCESS);
        if(!breedBadge.getText().isEmpty()){
            card.addToFooter(breedBadge);
        }

        Badge colorBadge = UIUtilities.createBadge(null,stock.getColor(), BadgeVariant.SUCCESS);
        if(!colorBadge.getText().isEmpty()){
            card.addToFooter(colorBadge);
        }

        if(stock.getBreeder()){
            card.addToFooter(UIUtilities.createBadge("Litters",litterService.getLitterCountForParent(stock).toString(), BadgeVariant.CONTRAST));
            card.addToFooter(UIUtilities.createBadge(stock.getStockType().getNonBreederName(), stockService.getKitCountForParent(stock).toString(), BadgeVariant.CONTRAST));
        }

        card.addToFooter(UIUtilities.createBadge(null,stock.getAge().getAgeFormattedString()));
        card.addToFooter(UIUtilities.createBadge(null,stock.getWeightInLbsOzAsString()));

        UIUtilities.setCardBorders(card, stock);

        return card;
    }

    private final ComponentRenderer<Component, Stock> stockValueCardRenderer = new ComponentRenderer<>(
            stockEntity -> {
                return createListItemValueCard(stockEntity);
            });    

    private Card createListItemValueCard(Stock stock){
        Card card = new Card();
        card.setWidthFull();
        Avatar avatar = stock.getAvatar(false).getAvatar();
        avatar.getElement().addEventListener("click", click -> {
            //open image dialog
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,DialogCommon.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");  
        
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeaderPrefix(avatar);
        card.setHeader(stock.getHeader());

        card.addToFooter(stock.getStatusBadge());

        card.addToFooter(stock.getSaleStatusBadge(true));

        card.addToFooter(UIUtilities.createBadge("Invoice", stock.getInvoiceNumber(),BadgeVariant.CONTRAST));

        card.addToFooter(UIUtilities.createBadge("Value", stock.getStockValueFormatted(), BadgeVariant.SUCCESS));

        UIUtilities.setCardBorders(card, stock);

        return card;
    }

    private void setStockValues() {
        if(stockId != null){
            stock = stockService.findById(stockId);
            stockType = stock.getStockType();
        }else{
            stock = null;
            stockType = stockTypeService.findRabbits();
        }
    }

    public void setId(Integer id, StockGridType stockGridType) {
        if(stockGridType == StockGridType.KITS){
            this.stockId = id;
            this.litterId = null; // Clear litterId when stockId is set
        } else if (stockGridType == StockGridType.LITTER) {
            this.litterId = id;
            this.stockId = null; // Clear stockId when litterId is set
        }
        setStockValues();
    }

    private void setStockGridDataProvider() {
        if(stockId != null){
            dataProvider = new ListDataProvider<>(stockService.getKitsForParent(stock));
            super.setDataProvider(dataProvider);
        } else if (litterId != null) {
            dataProvider = new ListDataProvider<>(stockService.getKitsForLitter(litterId));
            super.setDataProvider(dataProvider);
        }else{
            //dataProvider = new ListDataProvider<>(getListDataView().getItems().collect(Collectors.toList()));
            //super.setDataProvider(dataProvider);
        }

    }

    public void refreshGrid() {
        setStockGridDataProvider();
        if(stockGridType != StockGridType.STOCK){
            dataProvider.refreshAll();
        }
    }

    public StockGridType getStockGridType() {
        return stockGridType;
    }

    public void setStockGridType(StockGridType stockGridType) {
        this.stockGridType = stockGridType;
    }

    private void showAllColumns(){
        for(Column<Stock> column: getColumns()){
            getColumnByKey(column.getKey()).setVisible(true);
        }
    }
    



    public StockSavedQuery getCurrentStockSavedQuery() {
        return currentStockSavedQuery;
    }

    public void setCurrentStockSavedQuery(StockSavedQuery currentStockSavedQuery) {
        this.currentStockSavedQuery = currentStockSavedQuery;
    }

    public String getCurrentSearchName() {
        return currentSearchName;
    }

    public void setCurrentSearchName(String currentSearchName) {
        this.currentSearchName = currentSearchName;
    }

    private VerticalLayout createColumnSelector(){
        Set<String> resetColumnList = getVisibleColumnNames();
        VerticalLayout columnSelector = UIUtilities.getVerticalLayout(true, true, false);
        Div heading = new Div("Configure columns");
        heading.getStyle().set("font-weight", "600");
        heading.getStyle().set("padding", "var(--lumo-space-xs)");        

        Checkbox selectAllCheckbox = new Checkbox("Select All");
        if(getVisibleColumnKeys().size() == getColumnNames().size()){
            selectAllCheckbox.setValue(true);
            selectAllCheckbox.setIndeterminate(false);
        }else if(getVisibleColumnKeys().isEmpty()){
            selectAllCheckbox.setValue(false);
            selectAllCheckbox.setIndeterminate(false);
        }else{
            selectAllCheckbox.setIndeterminate(true);
        }


        CheckboxGroup<String> group = new CheckboxGroup<>();
        group.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        group.setItems(getColumnNames());
        group.setValue(getVisibleColumnNames());

        group.addValueChangeListener((e) -> {
            if (e.getValue().size() == getColumnNames().size()) {
                selectAllCheckbox.setValue(true);
                selectAllCheckbox.setIndeterminate(false);
            } else if (e.getValue().isEmpty()) {
                selectAllCheckbox.setValue(false);
                selectAllCheckbox.setIndeterminate(false);
            } else {
                selectAllCheckbox.setIndeterminate(true);
            }            

            for(Column<Stock> column: columnList){
                getColumnByKey(column.getKey()).setVisible(e.getValue().contains(column.getHeaderText()));
            }
            notifySidebarChanged(true);
        });

        selectAllCheckbox.addValueChangeListener(event -> {
            if (event.getValue()) {
                group.setValue(new HashSet<>(getColumnNames()));
            } else {
                group.deselectAll();
            }            
        });

        Button reset = new Button("Reset", (e) -> {
            group.setValue(resetColumnList);
        });
        reset.addThemeVariants(ButtonVariant.LUMO_SMALL);

        HorizontalLayout options = new HorizontalLayout(selectAllCheckbox, reset);
        options.setSpacing(false);
        options.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        options.setAlignItems(Alignment.BASELINE);

        columnSelector.add(heading,options,group);

        return columnSelector;
    }

    private GridContextMenu<Stock> createContextMenu(Grid<Stock> grid) {
        GridContextMenu<Stock> menu = new GridContextMenu<>(grid);

        menu.setDynamicContentHandler(stockEntity -> {
            if(stockEntity==null){
                if(currentStockSavedQuery != null && currentStockSavedQuery.getViewStyle().equals(StockViewStyle.TILE)){
                    return false;
                }else{
                    menu.removeAll();
                    menu.addComponent(createColumnSelector());
                    
                    return true;
                }

            }else{
                menu.removeAll();
                String stockName = stockEntity.getDisplayName();
                
                //add a label at the top with the stock name
                menu.addComponentAsFirst(UIUtilities.getContextMenuHeader(stockName));

                String addNewMenuTitle = "Add new " + stockEntity.getStockType().getNameSingular();
                GridMenuItem<Stock> addNewMenu = menu.addItem(new Item(addNewMenuTitle, Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addNewMenu.addMenuItemClickListener(click -> {
                    //open stock edit dialog
                    Stock newStock = new Stock();
                    newStock.setExternal(false);
                    newStock.setBreeder(true);
                    newStock.setStockType(stockEntity.getStockType());
                    newStock.setWeight(0);
                    //newStock.setStatus("active");
                    //newStock.setStatusDate(LocalDateTime.now());
                    
                    dialogCommon.setDialogTitle("Create new");
                    dialogCommon.dialogOpen(newStock,DialogCommon.DisplayMode.STOCK_DETAILS);
                    
                });

                GridMenuItem<Stock> addTaskMenu = menu.addItem(new Item("Add Task", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addTaskMenu.addMenuItemClickListener(click -> {
                    //open task edit dialog
                    //create a new task for this stock with default values
                    Task newTask = new Task();
                    newTask.setLinkType(Utility.TaskLinkType.BREEDER);
                    newTask.setLinkBreederId(stockEntity.getId());

                    taskEditor.dialogOpen(newTask, TaskEditor.DialogMode.CREATE, stockEntity.getStockType());
                    
                });
                menu.addSeparator();

                System.out.println("Context menu opened for stock: " + stockEntity.getDisplayName() + ", displayAsTile: " + displayAsTile + ", valueLayout: " + valueLayout);
                GridMenuItem<Stock> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
                displayAsTileMenu.setCheckable(true);
                displayAsTileMenu.setChecked(displayAsTile);
                displayAsTileMenu.addMenuItemClickListener(click -> {
                    displayAsTile = displayAsTileMenu.isChecked();
                    if(currentStockSavedQuery != null){
                        currentStockSavedQuery.setDisplayAsTile(displayAsTile);
                    }
                    configureGrid();
                    notifySidebarChanged(true);
                    refreshGrid();
                });
                menu.addSeparator();

                GridMenuItem<Stock> valueLayoutMenu = menu.addItem(new Item("Display as Value Layout", Utility.ICONS.ACTION_VIEW.getIconSource()));
                valueLayoutMenu.setCheckable(true);
                valueLayoutMenu.setChecked(valueLayout);
                valueLayoutMenu.addMenuItemClickListener(click -> {
                    valueLayout = valueLayoutMenu.isChecked();
                    if(currentStockSavedQuery != null){
                        currentStockSavedQuery.setValueLayout(valueLayout);
                    }
                    configureGrid();
                    notifySidebarChanged(true);
                    refreshGrid();
                });

                GridMenuItem<Stock> editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
                editMenu.addMenuItemClickListener(click -> {
                    //open stock edit dialog
                    dialogCommon.setDialogTitle("Edit Stock");
                    dialogCommon.dialogOpen(stockEntity,DialogCommon.DisplayMode.STOCK_DETAILS);
                });
                GridMenuItem<Stock> editImageMenu = menu.addItem(new Item("Edit Image", Utility.ICONS.ACTION_EDIT_IMAGE.getIconSource()));
                editImageMenu.addMenuItemClickListener(click -> {
                    //open image dialog
                    dialogCommon.setDialogTitle("Edit Profile Image");
                    dialogCommon.dialogOpen(stockEntity,DialogCommon.DisplayMode.PROFILE_IMAGE);
                });
                if(!stockEntity.getStockType().getGenotypes().isEmpty()){
                    GridMenuItem<Stock> editGenotype = menu.addItem(new Item("Edit Genotype", Utility.ICONS.ACTION_PEDIGREE.getIconSource()));
                    editGenotype.addMenuItemClickListener(click -> {
                        //open genotypeEditor
                        genotypeEditor.dialogOpen(stockEntity);
                    });
                }
                menu.addSeparator();
                //TODO: possible remove the Breed and Birth items as they are in Tasks
                GridMenuItem<Stock> breedPlanMenu = menu.addItem(new Item("Breed Plan", Utility.ICONS.TYPE_BREEDER.getIconSource()));
                    breedPlanMenu.addMenuItemClickListener(click -> {
                        //open breed plan dialog
                        planEditor.dialogOpen(Utility.TaskLinkType.BREEDER, stockEntity);
                    });
                menu.addItem(new Item("Birth", Utility.ICONS.ACTION_BIRTH.getIconSource()));
                createStatusMenuItem(menu, stockEntity, "sold");
                createStatusMenuItem(menu, stockEntity, "forsale");
                createStatusMenuItem(menu, stockEntity, "deposit");
                createStatusMenuItem(menu, stockEntity, "butchered");
                createStatusMenuItem(menu, stockEntity, "died");
                createStatusMenuItem(menu, stockEntity, "archived");
                createStatusMenuItem(menu, stockEntity, "culled");
                createStatusMenuItem(menu, stockEntity, "active");
                GridMenuItem<Stock> weighMenu = menu.addItem(new Item("Weigh", Utility.ICONS.ACTION_WEIGH.getIconSource()));
                weighMenu.addMenuItemClickListener(click -> {
                    weightEditor.dialogOpen(stockEntity);
                });
                menu.addSeparator();
                GridMenuItem<Stock> pedigreeEditorMenu = menu.addItem(new Item("Pedigree", Utility.ICONS.ACTION_PEDIGREE.getIconSource()));
                pedigreeEditorMenu.addMenuItemClickListener(click -> {
                    UI.getCurrent().navigate(StockPedigreeEditor.class, stockEntity.getId().toString());
                });
                menu.addSeparator();
                GridMenuItem<Stock> deleteMenu = menu.addItem(new Item("Delete", Utility.ICONS.ACTION_DELETE.getIconSource()));
                deleteMenu.addMenuItemClickListener(click -> {
                    deleteStockEntityWithConfirm(stockEntity);
                });

                return true;
            }
        });

        return menu;
    }

    private List<Column<Stock>> getCleanColumnList(List<Column<Stock>> originalList){
        List<Column<Stock>> returnList = new ArrayList<>(originalList);
        returnList.remove(0);
        return returnList;
    }

    private List<String> getColumnNames(){
        List<String> columnNames = new ArrayList<>();
        for (Column<Stock> column : columnList) {
            columnNames.add(column.getHeaderText());
        }
        return columnNames;
    }
    
    private Set<String> getVisibleColumnNames(){
        List<String> columnNames = new ArrayList<>();
        for (Column<Stock> column : columnList) {
            if(column.isVisible()){
                columnNames.add(column.getHeaderText());
            }
        }
        return new HashSet<>(columnNames);
    }
    
    public Set<String> getVisibleColumnKeys(){
        List<String> columnNames = new ArrayList<>();
        for (Column<Stock> column : columnList) {
            if(column.isVisible()){
                columnNames.add(column.getKey());
            }
        }
        return new HashSet<>(columnNames);
    }
    
    private void createStatusMenuItem(GridContextMenu<Stock> menu, Stock stockEntity, String statusToEdit){
        if(!stockEntity.getStatus().equals(statusToEdit)){
            StockStatus status = Utility.getInstance().getStockStatus(statusToEdit);
            GridMenuItem<Stock> item = menu.addItem(new Item(status.getActionName(), status.getIcon().getIconSource()));
            item.addMenuItemClickListener(click -> {
                //open status editor
                statusEditor.dialogOpen(stockEntity, statusToEdit);
            });
        }
    }
    
    private void deleteStockEntityWithConfirm(Stock stockEntity){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete \"" + stockEntity.getDisplayName() + "\"?");
        dialog.setText(
                "Are you sure you want to permanently delete " + stockEntity.getDisplayName() + "?");

        dialog.setCancelable(true);
        //dialog.addCancelListener(event -> setStatus("Canceled"));

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            if(stockService.checkInUse(stockEntity)){ //if in use then do not delete and WARN
                warnStockInUse(stockEntity);
            }else{
                //delete status
                statusService.deleteByStockId(stockEntity.getId());
                //delete weight
                weightService.deleteByStockId(stockEntity.getId());
                //delete stock
                stockService.delete(stockEntity.getId());
                listRefreshNeeded();
            }
        });     
        dialog.open();
    }

    private void warnStockInUse(Stock stockEntity){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("\"" + stockEntity.getDisplayName() + "\" in use!");
        dialog.setText(
                "Cannot permanently delete " + stockEntity.getDisplayName() + " as is in use as a parent!");

        //dialog.setCancelable(false);
        //dialog.addCancelListener(event -> setStatus("Canceled"));

        dialog.setConfirmText("Ok");
        //dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    public Boolean getDisplayAsTile() {
        return displayAsTile;
    }

    public void setDisplayAsTile(Boolean displayAsTile) {
        this.displayAsTile = displayAsTile;
    }

    public Boolean getValueLayout() {
        return valueLayout;
    }

    public void setValueLayout(Boolean valueLayout) {
        this.valueLayout = valueLayout;
    }

    @Override
    public void listRefreshNeeded() {
        refreshGrid();
        notifyRefreshNeeded();
    }

    public void addListener(ListRefreshNeededListener listener) {
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded() {
        for (ListRefreshNeededListener listener : listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

    public void addSidebarChangedListener(SidebarChangedListener listener) {
        sidebarChangedListeners.add(listener);
    }

    private void notifySidebarChanged(Boolean changed) {
        for (SidebarChangedListener listener : sidebarChangedListeners) {
            listener.sidebarChanged(changed);
        }
    }

}
