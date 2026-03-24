package ca.jusjoken.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.StockSaleStatus;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.StockWeightHistoryService;
import ca.jusjoken.data.service.UserUiSettingsService;
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.utility.TaskType;
import ca.jusjoken.views.stock.StockPedigreeEditor;
import ca.jusjoken.views.utility.LitterListView;

public class StockGrid extends Grid<Stock> implements ListRefreshNeededListener{

    private ListDataProvider<Stock> dataProvider;
    private Integer stockId;
    private Integer litterId;
    private Stock stock;
    private StockType stockType;
    private final StockService stockService;
    private final StockTypeService stockTypeService;
    private final LitterService litterService;
    private final UserUiSettingsService userUiSettingsService;
    private StockSavedQuery currentStockSavedQuery;
    private String currentSearchName;
    private final StockEditor dialogCommon;
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
    private static final String ACTION_COLUMN_KEY = "row-actions";
    private Boolean displayAsTile = false;
    private Boolean valueLayout = false;
    private String preferenceScopeKey;
    private Registration sortListenerRegistration;
    private boolean restoringSortOrder = false;
    private boolean suppressSortPersistence = false;

    public static enum StockGridType {
        LITTER, STOCK, KITS
    }

    private enum FosterPlacement {
        NONE,
        FOSTER_IN,
        FOSTER_OUT
    }

    private StockGridType stockGridType = StockGridType.KITS;

    public StockGrid() {
        super(Stock.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
        this.statusService = Registry.getBean(StockStatusHistoryService.class);
        this.weightService = Registry.getBean(StockWeightHistoryService.class);
        this.displayAsTile = false;
        this.valueLayout = false;

        this.dialogCommon = new StockEditor();
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
            setColumns();
            refreshGrid();
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
        List<String> visibleColumnKeys = currentStockSavedQuery != null
                ? currentStockSavedQuery.getVisibleColumnKeyList()
                : loadVisibleColumnPreference();
        if(visibleColumnKeys != null && !visibleColumnKeys.isEmpty()){
            for(Column<Stock> column: columnList){
                getColumnByKey(column.getKey()).setVisible(visibleColumnKeys.contains(column.getKey()));
            }
        }
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();
        removeClassName("mobile-tile-scroll-fix");

        if(displayAsTile && valueLayout){
            addClassName("mobile-tile-scroll-fix");
            configureValueTileView();
        }else if(displayAsTile && !valueLayout){
            addClassName("mobile-tile-scroll-fix");
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
        setMultiSort(true);

        addRowActionsColumn();

        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(false).setKey("name")
          .setComparator(Stock::getDisplayName).setSortable(true);

        addComponentColumn(stockEntity -> { return stockEntity.getSaleStatusBadge(false); })
            .setHeader("Sale Status").setResizable(true).setAutoWidth(true).setKey("salestatus")
            .setComparator(this::getSaleStatusSortRank).setSortable(true);

        addColumn(Stock::getInvoiceNumber)
            .setHeader("Invoice").setResizable(true).setAutoWidth(true).setKey("invoice").setSortable(true);

        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00"))
                .setHeader("Value")
                .setResizable(true)
                .setAutoWidth(true)
                .setKey("value")
                .setFooter(getValueFooter())
                .setTextAlign(ColumnTextAlign.END)
            .setSortable(true)
                .setFrozenToEnd(true);

        configureSortPersistenceListener();

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

        addRowActionsColumn();

        addComponentColumn(stockEntity -> {
            return createNameCell(stockEntity);
                }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(false).setKey("name")
                    .setComparator(Stock::getDisplayName).setSortable(true);

        if (stockGridType == StockGridType.LITTER) {
            addComponentColumn(this::createFosterIndicatorBadge)
                    .setHeader("Foster")
                    .setResizable(true)
                    .setAutoWidth(true)
                    .setKey("foster")
                    .setComparator(this::getFosterSortValue)
                    .setSortable(true);
        }

        if(stockGridType == StockGridType.STOCK){
            addComponentColumn(stockEntity -> {
                        if (stockEntity.getBreeder()) {
                            return new Icon(Utility.ICONS.TYPE_BREEDER.getIconSource());
                        } else {
                            return null;
                        }
                    }).setHeader("Breeder").setResizable(true).setAutoWidth(true).setKey("breeder")
                      .setComparator(Stock::getBreeder).setSortable(true);
        }

        addColumn(Stock::getBreed).setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed").setSortable(true);
        addColumn(Stock::getColor).setHeader("Colour").setResizable(true).setAutoWidth(true).setKey("color").setSortable(true);
        addColumn(Stock::getGenotype).setHeader("Genotype").setResizable(true).setAutoWidth(true).setKey("genotype").setSortable(true);

        if(stockGridType == StockGridType.STOCK){
            addColumn(Stock::getChampNo).setHeader("ChampNo").setResizable(true).setAutoWidth(true).setKey("champno").setSortable(true);
            addColumn(Stock::getLegs).setHeader("Legs").setResizable(true).setAutoWidth(true).setKey("legs").setSortable(true);
            addColumn(Stock::getRegNo).setHeader("RegNo").setResizable(true).setAutoWidth(true).setKey("regno").setSortable(true);
        }

        addColumn(Stock::getStatus).setHeader("Status").setResizable(true).setAutoWidth(true).setKey("status").setSortable(true);
        addColumn(new LocalDateTimeRenderer<>(Stock::getStatusDate,"MM-dd-YYYY HHmm")).setHeader("Status Date").setResizable(true).setAutoWidth(true).setKey("statusdate").setSortable(true);
        addColumn(Stock::getTattoo).setHeader("Tattoo").setResizable(true).setAutoWidth(true).setKey("tattoo").setSortable(true);
        addColumn(Stock::getWeightInLbsOzAsString).setHeader("Weight").setResizable(true).setAutoWidth(true).setKey("weight").setComparator(Stock::getWeight).setSortable(true);
        addColumn(new LocalDateRenderer<>(Stock::getAcquired,"MM-dd-YYYY")).setHeader("Aquired").setResizable(true).setAutoWidth(true).setKey("aquired").setSortable(true);
        addColumn(new LocalDateRenderer<>(Stock::getDoB,"MM-dd-YYYY")).setHeader("DOB").setResizable(true).setAutoWidth(true).setKey("dob").setSortable(true);
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
        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00")).setHeader("Value").setResizable(true).setAutoWidth(true).setKey("value").setSortable(true);
        addColumn(Stock::getNotes).setHeader("Notes").setResizable(true).setAutoWidth(true).setKey("notes").setSortable(true);
        if(stockGridType == StockGridType.STOCK){
            addComponentColumn(stockEntity -> {
                        if (Boolean.TRUE.equals(stockEntity.getExternal())) {
                            return new Icon(Utility.ICONS.ACTION_CHECK.getIconSource());
                        } else {
                            return null;
                        }
                    }).setHeader("External").setResizable(true).setAutoWidth(true).setKey("external")
                      .setComparator(Stock::getExternal).setSortable(true);
        }

        setMultiSort(true);
        configureSortPersistenceListener();

    }

    private void configureSortPersistenceListener() {
        if (sortListenerRegistration != null) {
            sortListenerRegistration.remove();
        }
        sortListenerRegistration = addSortListener(event -> {
            if (restoringSortOrder || suppressSortPersistence) {
                return;
            }
            List<ColumnSort> sortOrders = toColumnSortList(event.getSortOrder());
            persistSortOrders(sortOrders);
            notifySidebarChanged(true);
            if (stockGridType == StockGridType.STOCK && getDataProvider() != null) {
                getDataProvider().refreshAll();
            }
        });
    }

    private final ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stockEntity -> {
                // return createListItemLayout(stockEntity);
                return createListItemCard(stockEntity);
            });    

    private Card createListItemCard(Stock stock){
        Card card = new Card();
        card.setWidthFull();
        card.getStyle().set("margin", "var(--lumo-space-xs) 0");
        Avatar avatar = stock.getAvatar(false).getAvatar();
        avatar.getElement().addEventListener("click", click -> {
            //open image dialog
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,StockEditor.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");  
        
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeaderPrefix(avatar);
        card.setHeader(stock.getHeader());
        card.setHeaderSuffix(createRowMenuButton());

        FosterPlacement fosterPlacement = getFosterPlacementForCurrentLitter(stock);
        if (fosterPlacement == FosterPlacement.FOSTER_OUT) {
            card.getElement().getStyle().set("opacity", "0.65");
        }

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

        if (fosterPlacement == FosterPlacement.FOSTER_IN) {
            card.addToFooter(createFosterIndicatorBadge(stock));
        } else if (fosterPlacement == FosterPlacement.FOSTER_OUT) {
            card.addToFooter(createFosterIndicatorBadge(stock));
        }

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
        card.getStyle().set("margin", "var(--lumo-space-xs) 0");
        Avatar avatar = stock.getAvatar(false).getAvatar();
        avatar.getElement().addEventListener("click", click -> {
            //open image dialog
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,StockEditor.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");  
        
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeaderPrefix(avatar);
        card.setHeader(stock.getHeader());
        card.setHeaderSuffix(createRowMenuButton());

        FosterPlacement fosterPlacement = getFosterPlacementForCurrentLitter(stock);
        if (fosterPlacement == FosterPlacement.FOSTER_OUT) {
            card.getElement().getStyle().set("opacity", "0.65");
        }

        card.addToFooter(stock.getSaleStatusBadge(true));

        card.addToFooter(UIUtilities.createBadge("Invoice", stock.getInvoiceNumber(),BadgeVariant.CONTRAST));

        card.addToFooter(UIUtilities.createBadge("Value", stock.getStockValueFormatted(), BadgeVariant.SUCCESS));

        if (fosterPlacement == FosterPlacement.FOSTER_IN) {
            card.addToFooter(createFosterIndicatorBadge(stock));
        } else if (fosterPlacement == FosterPlacement.FOSTER_OUT) {
            card.addToFooter(createFosterIndicatorBadge(stock));
        }

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
        this.stockGridType = stockGridType;
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
            dataProvider = new ListDataProvider<>(getSortedStocks(stockService.getKitsForParent(stock)));
            super.setDataProvider(dataProvider);
        } else if (litterId != null) {
            dataProvider = new ListDataProvider<>(getSortedStocks(stockService.getKitsForLitterDisplay(litterId)));
            super.setDataProvider(dataProvider);
        }else{
            //dataProvider = new ListDataProvider<>(getListDataView().getItems().collect(Collectors.toList()));
            //super.setDataProvider(dataProvider);
        }

    }

    private List<Stock> getSortedStocks(List<Stock> stocks) {
        if (stocks == null || stocks.size() < 2) {
            return stocks;
        }

        List<ColumnSort> sortOrders = loadPersistedSortOrders();
        if (sortOrders == null || sortOrders.isEmpty()) {
            return stocks;
        }

        List<Stock> sortedStocks = new ArrayList<>(stocks);
        sortedStocks.sort((left, right) -> compareStocks(left, right, sortOrders));
        return sortedStocks;
    }

    private int compareStocks(Stock left, Stock right, List<ColumnSort> sortOrders) {
        for (ColumnSort sortOrder : sortOrders) {
            if (sortOrder == null || sortOrder.getColumnName() == null) {
                continue;
            }

            int result = compareStocksByProperty(left, right, sortOrder.getColumnName());
            if (sortOrder.getColumnSortDirection() == org.springframework.data.domain.Sort.Direction.DESC) {
                result = -result;
            }
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private int compareStocksByProperty(Stock left, Stock right, String sortProperty) {
        return switch (sortProperty) {
            case "name" -> compareStrings(left == null ? null : left.getDisplayName(), right == null ? null : right.getDisplayName());
            case "breeder" -> compareComparable(left == null ? null : left.getBreeder(), right == null ? null : right.getBreeder());
            case "breed" -> compareStrings(left == null ? null : left.getBreed(), right == null ? null : right.getBreed());
            case "color" -> compareStrings(left == null ? null : left.getColor(), right == null ? null : right.getColor());
            case "genotype" -> compareStrings(left == null ? null : left.getGenotype(), right == null ? null : right.getGenotype());
            case "champNo" -> compareStrings(left == null ? null : left.getChampNo(), right == null ? null : right.getChampNo());
            case "legs" -> compareComparable(left == null ? null : left.getLegs(), right == null ? null : right.getLegs());
            case "regNo" -> compareStrings(left == null ? null : left.getRegNo(), right == null ? null : right.getRegNo());
            case "status" -> compareStrings(left == null ? null : left.getStatus(), right == null ? null : right.getStatus());
            case "statusDate" -> compareComparable(left == null ? null : left.getStatusDate(), right == null ? null : right.getStatusDate());
            case "tattoo" -> compareStrings(left == null ? null : left.getTattoo(), right == null ? null : right.getTattoo());
            case "weight" -> compareComparable(left == null ? null : left.getWeight(), right == null ? null : right.getWeight());
            case "acquired" -> compareComparable(left == null ? null : left.getAcquired(), right == null ? null : right.getAcquired());
            case "doB" -> compareComparable(left == null ? null : left.getDoB(), right == null ? null : right.getDoB());
            case "saleStatus" -> Integer.compare(getSaleStatusSortRank(left), getSaleStatusSortRank(right));
            case "invoiceNumber" -> compareStrings(left == null ? null : left.getInvoiceNumber(), right == null ? null : right.getInvoiceNumber());
            case "stockValue" -> compareComparable(left == null ? null : left.getStockValue(), right == null ? null : right.getStockValue());
            case "notes" -> compareStrings(left == null ? null : left.getNotes(), right == null ? null : right.getNotes());
            case "foster" -> Integer.compare(getFosterSortValue(left), getFosterSortValue(right));
            default -> 0;
        };
    }

    private int getSaleStatusSortRank(Stock stockEntity) {
        if (stockEntity == null) {
            return Integer.MAX_VALUE;
        }

        StockSaleStatus saleStatus = stockEntity.getSaleStatus();
        if (saleStatus == null) {
            return Integer.MAX_VALUE;
        }

        return switch (saleStatus) {
            case NONE -> 0;
            case LISTED -> 1;
            case DEPOSIT -> 2;
            case SOLD -> 3;
        };
    }

    private Component createNameCell(Stock stockEntity) {
        Component nameComponent = stockEntity.getnameAndPrefix(false, true, true);
        if (getFosterPlacementForCurrentLitter(stockEntity) == FosterPlacement.FOSTER_OUT) {
            nameComponent.getElement().getStyle().set("opacity", "0.65");
            nameComponent.getElement().getStyle().set("text-decoration", "line-through");
        }
        return nameComponent;
    }

    private Component createFosterIndicatorBadge(Stock stockEntity) {
        FosterPlacement placement = getFosterPlacementForCurrentLitter(stockEntity);
        if (placement == FosterPlacement.FOSTER_IN) {
            Badge badge = UIUtilities.createBadge(null, "Foster In", BadgeVariant.PRIMARY);
            String birthLitterName = getBirthLitterDisplayName(stockEntity);
            if (!birthLitterName.isBlank()) {
                badge.getElement().setProperty("title", "Born in: " + birthLitterName);
            }
            return badge;
        }
        if (placement == FosterPlacement.FOSTER_OUT) {
            Badge badge = UIUtilities.createBadge(null, "Fostered Out", BadgeVariant.WARNING);
            String fosterLitterName = getFosterLitterDisplayName(stockEntity);
            String tooltip = fosterLitterName.isBlank()
                    ? "Moved to foster litter"
                    : "Now in: " + fosterLitterName;
            badge.getElement().setProperty("title", tooltip + " (click for details)");
            badge.getStyle().set("cursor", "pointer");
            badge.addClickListener(event -> openFosterLitterDialog(stockEntity));
            return badge;
        }
        return new Div();
    }

    private String getBirthLitterDisplayName(Stock stockEntity) {
        if (stockEntity == null || stockEntity.getLitter() == null) {
            return "";
        }
        String displayName = stockEntity.getLitter().getDisplayName();
        return displayName == null ? "" : displayName;
    }

    private String getFosterLitterDisplayName(Stock stockEntity) {
        if (stockEntity == null || stockEntity.getFosterLitter() == null) {
            return "";
        }
        String displayName = stockEntity.getFosterLitter().getDisplayName();
        return displayName == null ? "" : displayName;
    }

    private void openFosterLitterDialog(Stock stockEntity) {
        String fosterLitterName = getFosterLitterDisplayName(stockEntity);
        String destination = fosterLitterName.isBlank() ? "Unknown" : fosterLitterName;
        Integer fosterLitterId = stockEntity == null || stockEntity.getFosterLitter() == null
            ? null
            : stockEntity.getFosterLitter().getId();

        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setHeaderTitle("Foster Destination");

        TextField destinationField = new TextField("Current foster litter");
        destinationField.setWidthFull();
        destinationField.setReadOnly(true);
        destinationField.setValue(destination);

        Button openLitters = new Button("Open Litters", click -> {
            UI currentUi = UI.getCurrent();
            if (currentUi != null) {
                if (fosterLitterId != null) {
                    currentUi.navigate("litters", new QueryParameters(Map.of("litterId", List.of(String.valueOf(fosterLitterId)))));
                } else {
                    currentUi.navigate(LitterListView.class);
                }
            }
            dialog.close();
        });
        openLitters.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        openLitters.setEnabled(fosterLitterId != null);

        Button close = new Button("Close", click -> dialog.close());

        HorizontalLayout footer = new HorizontalLayout(openLitters, close);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();

        VerticalLayout content = new VerticalLayout(destinationField, footer);
        content.setPadding(false);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }

    private int getFosterSortValue(Stock stockEntity) {
        FosterPlacement placement = getFosterPlacementForCurrentLitter(stockEntity);
        return switch (placement) {
            case FOSTER_IN -> 1;
            case FOSTER_OUT -> 2;
            case NONE -> 0;
        };
    }

    private FosterPlacement getFosterPlacementForCurrentLitter(Stock stockEntity) {
        if (stockGridType != StockGridType.LITTER || litterId == null || stockEntity == null) {
            return FosterPlacement.NONE;
        }

        Integer birthLitterId = stockEntity.getLitter() == null ? null : stockEntity.getLitter().getId();
        Integer fosterLitterId = stockEntity.getFosterLitter() == null ? null : stockEntity.getFosterLitter().getId();

        if (fosterLitterId != null && Objects.equals(fosterLitterId, litterId) && !Objects.equals(birthLitterId, litterId)) {
            return FosterPlacement.FOSTER_IN;
        }
        if (Objects.equals(birthLitterId, litterId) && fosterLitterId != null && !Objects.equals(fosterLitterId, litterId)) {
            return FosterPlacement.FOSTER_OUT;
        }
        return FosterPlacement.NONE;
    }

    private int compareStrings(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    }

    private <T extends Comparable<? super T>> int compareComparable(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    public void refreshGrid() {
        setStockGridDataProvider();
        if(stockGridType != StockGridType.STOCK){
            dataProvider.refreshAll();
        }
        // Data provider resets can clear visual sort state, so restore persisted sort.
        applySavedSortOrder();
        if (stockGridType != StockGridType.STOCK) {
            refreshClientGridState();
        }
    }

    private void refreshClientGridState() {
        getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this, context -> {
            scrollToStart();
            getElement().executeJs("if (this.clearCache) { this.clearCache(); }");
            List<ColumnSort> indicatorSort = loadPersistedSortOrders();
            if (indicatorSort == null || indicatorSort.isEmpty()) {
                indicatorSort = List.of(new ColumnSort("name", org.springframework.data.domain.Sort.Direction.ASC));
            }
            updateClientSideSortIndicatorsNow(toGridSortOrders(indicatorSort));
        }));
    }

    private void updateClientSideSortIndicatorsNow(List<GridSortOrder<Stock>> gridSortOrders) {
        if (gridSortOrders == null || gridSortOrders.isEmpty()) {
            return;
        }
        try {
            Method method = Grid.class.getDeclaredMethod("updateClientSideSorterIndicators", List.class);
            method.setAccessible(true);
            method.invoke(this, gridSortOrders);
        } catch (Exception ex) {
            // Best effort: keep data ordering behavior even if indicator syncing fails.
        }
    }

    private void syncServerSortStateWithoutResort(List<GridSortOrder<Stock>> gridSortOrders) {
        if (gridSortOrders == null || gridSortOrders.isEmpty()) {
            return;
        }
        try {
            Field sortOrderField = Grid.class.getDeclaredField("sortOrder");
            sortOrderField.setAccessible(true);
            sortOrderField.set(this, new ArrayList<>(gridSortOrders));
        } catch (Exception ex) {
            // Best effort: indicator state can still be restored client-side.
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

    public void restoreSavedSortOrder() {
        applySavedSortOrder();
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
            saveVisibleColumnPreference(getVisibleColumnKeys());
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
                if(displayAsTile){
                    return false;
                }else{
                    menu.removeAll();
                    menu.addComponent(createColumnSelector());
                    
                    return true;
                }

            }else{
                menu.removeAll();
                GridMenuItem<Stock> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
                displayAsTileMenu.setCheckable(true);
                displayAsTileMenu.setChecked(displayAsTile);
                displayAsTileMenu.addMenuItemClickListener(click -> {
                    displayAsTile = displayAsTileMenu.isChecked();
                    if(currentStockSavedQuery != null){
                        currentStockSavedQuery.setDisplayAsTile(displayAsTile);
                    } else {
                        saveDisplayAsTilePreference();
                    }
                    configureGrid();
                    notifySidebarChanged(true);
                    refreshGrid();
                });

                GridMenuItem<Stock> valueLayoutMenu = menu.addItem(new Item("Display as Value Layout", Utility.ICONS.ACTION_VIEW.getIconSource()));
                valueLayoutMenu.setCheckable(true);
                valueLayoutMenu.setChecked(valueLayout);
                valueLayoutMenu.addMenuItemClickListener(click -> {
                    valueLayout = valueLayoutMenu.isChecked();
                    if(currentStockSavedQuery != null){
                        currentStockSavedQuery.setValueLayout(valueLayout);
                    } else {
                        saveValueLayoutPreference();
                    }
                    configureGrid();
                    notifySidebarChanged(true);
                    refreshGrid();
                });
                menu.addSeparator();

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
                    dialogCommon.dialogOpen(newStock,StockEditor.DisplayMode.STOCK_DETAILS);
                    
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

                GridMenuItem<Stock> editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
                editMenu.addMenuItemClickListener(click -> {
                    //open stock edit dialog
                    dialogCommon.setDialogTitle("Edit Stock");
                    dialogCommon.dialogOpen(stockEntity,StockEditor.DisplayMode.STOCK_DETAILS);
                });
                GridMenuItem<Stock> editImageMenu = menu.addItem(new Item("Edit Image", Utility.ICONS.ACTION_EDIT_IMAGE.getIconSource()));
                editImageMenu.addMenuItemClickListener(click -> {
                    //open image dialog
                    dialogCommon.setDialogTitle("Edit Profile Image");
                    dialogCommon.dialogOpen(stockEntity,StockEditor.DisplayMode.PROFILE_IMAGE);
                });
                if(!stockEntity.getStockType().getGenotypes().isEmpty()){
                    GridMenuItem<Stock> editGenotype = menu.addItem(new Item("Edit Genotype", Utility.ICONS.ACTION_PEDIGREE.getIconSource()));
                    editGenotype.addMenuItemClickListener(click -> {
                        //open genotypeEditor
                        genotypeEditor.dialogOpen(stockEntity);
                    });
                }
                menu.addSeparator();
                GridMenuItem<Stock> planBreedingMenu = menu.addItem(new Item("Plan Breeding", Utility.ICONS.TYPE_BREEDER.getIconSource()));
                planBreedingMenu.addMenuItemClickListener(click -> {
                    Task newTask = new Task();
                    newTask.setType(TaskType.BREEDING);
                    newTask.setLinkType(Utility.TaskLinkType.BREEDER);
                    newTask.setLinkBreederId(stockEntity.getId());
                    newTask.setDate(LocalDate.now());
                    taskEditor.dialogOpen(newTask, TaskEditor.DialogMode.CREATE, stockEntity.getStockType());
                });
                GridMenuItem<Stock> breedPlanMenu = menu.addItem(new Item("Create Breed Plan", Utility.ICONS.TYPE_BREEDER.getIconSource()));
                    breedPlanMenu.addMenuItemClickListener(click -> {
                        //open breed plan dialog
                        planEditor.dialogOpen(Utility.TaskLinkType.BREEDER, stockEntity);
                    });
                menu.addSeparator();
                createStatusMenuItem(menu, stockEntity, "listed");
                createStatusMenuItem(menu, stockEntity, "deposit");
                createStatusMenuItem(menu, stockEntity, "sold");
                createStatusMenuItem(menu, stockEntity, "active");
                menu.addSeparator();
                createStatusMenuItem(menu, stockEntity, "butchered");
                createStatusMenuItem(menu, stockEntity, "died");
                createStatusMenuItem(menu, stockEntity, "archived");
                createStatusMenuItem(menu, stockEntity, "culled");
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
        returnList.removeIf(column -> ACTION_COLUMN_KEY.equals(column.getKey()));
        if (!returnList.isEmpty()) {
            returnList.remove(0);
        }
        return returnList;
    }

    private void addRowActionsColumn() {
        addComponentColumn(stockEntity -> createRowMenuButton())
                .setHeader("")
                .setAutoWidth(false)
                .setFlexGrow(0)
                .setWidth("3.25em")
                .setFrozen(true)
                .setResizable(false)
                .setSortable(false)
                .setKey(ACTION_COLUMN_KEY);
    }

    private Button createRowMenuButton() {
        Button menuButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        menuButton.getElement().setAttribute("title", "Actions");
        menuButton.getElement().setAttribute("aria-label", "Open row menu");
        menuButton.getStyle().set("flex-shrink", "0");
        menuButton.addClickListener(event -> menuButton.getElement().executeJs(
                "const rect=this.getBoundingClientRect();"
                        + "this.dispatchEvent(new MouseEvent('contextmenu', {"
                        + "bubbles:true,cancelable:true,view:window,clientX:rect.left + rect.width/2,clientY:rect.bottom" 
                        + "}));"));
        return menuButton;
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
        String currentStatus = stockEntity.getStatus();
        if (Objects.equals(currentStatus, statusToEdit)) {
            return;
        }

        StockStatus status = Utility.getInstance().getStockStatus(statusToEdit);
        if (status == null || status.getActionName() == null || status.getIcon() == null) {
            return;
        }

        GridMenuItem<Stock> item = menu.addItem(new Item(status.getActionName(), status.getIcon().getIconSource()));
        item.addMenuItemClickListener(click -> {
            //open status editor
            statusEditor.dialogOpen(stockEntity, statusToEdit);
        });
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
        this.displayAsTile = Boolean.TRUE.equals(displayAsTile);
    }

    public void setPreferenceScopeKey(String preferenceScopeKey) {
        this.preferenceScopeKey = preferenceScopeKey;
        loadDisplayAsTilePreference();
        loadValueLayoutPreference();
    }

    public String getPreferenceScopeKey() {
        return preferenceScopeKey;
    }

    public void loadDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }
        displayAsTile = userUiSettingsService.getBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(displayAsTile));
    }

    public Boolean getValueLayout() {
        return valueLayout;
    }

    public void loadValueLayoutPreference() {
        String settingsKey = getValueLayoutPreferenceKey();
        if (settingsKey == null) {
            return;
        }
        valueLayout = userUiSettingsService.getBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(valueLayout));
    }

    private void saveDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }
        userUiSettingsService.setBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(displayAsTile));
    }

    private void saveValueLayoutPreference() {
        String settingsKey = getValueLayoutPreferenceKey();
        if (settingsKey == null) {
            return;
        }
        userUiSettingsService.setBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(valueLayout));
    }

    private String getDisplayAsTilePreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".displayAsTile";
    }

    private String getValueLayoutPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".valueLayout";
    }

    private String getSortPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".sortOrder";
    }

    private String getVisibleColumnsPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".visibleColumns";
    }

    private List<String> loadVisibleColumnPreference() {
        if (currentStockSavedQuery != null) {
            return currentStockSavedQuery.getVisibleColumnKeyList();
        }

        String settingsKey = getVisibleColumnsPreferenceKey();
        if (settingsKey == null) {
            return List.of();
        }

        Optional<Object> value = userUiSettingsService.getValueForCurrentUser(settingsKey);
        if (value.isEmpty()) {
            return List.of();
        }

        String serialized = String.valueOf(value.get());
        if (serialized.isBlank()) {
            return List.of();
        }

        List<String> parsed = new ArrayList<>();
        for (String part : serialized.split(",")) {
            if (part == null) {
                continue;
            }
            String key = part.trim();
            if (!key.isBlank()) {
                parsed.add(key);
            }
        }
        return parsed;
    }

    private void saveVisibleColumnPreference(Set<String> visibleColumnKeys) {
        if (currentStockSavedQuery != null) {
            return;
        }

        String settingsKey = getVisibleColumnsPreferenceKey();
        if (settingsKey == null) {
            return;
        }

        if (visibleColumnKeys == null || visibleColumnKeys.isEmpty()) {
            userUiSettingsService.setValueForCurrentUser(settingsKey, "");
            return;
        }

        String serialized = visibleColumnKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        userUiSettingsService.setValueForCurrentUser(settingsKey, serialized);
    }

    private void applySavedSortOrder() {
        if (getColumns().isEmpty()) {
            return;
        }

        List<ColumnSort> effectiveSort = loadPersistedSortOrders();
        if (effectiveSort == null || effectiveSort.isEmpty()) {
            effectiveSort = List.of(new ColumnSort("name", org.springframework.data.domain.Sort.Direction.ASC));
        }

        List<GridSortOrder<Stock>> gridSortOrders = toGridSortOrders(effectiveSort);
        if (gridSortOrders.isEmpty()) {
            return;
        }

        if (stockGridType != StockGridType.STOCK) {
            syncServerSortStateWithoutResort(gridSortOrders);
            if (getDataProvider() != null) {
                getDataProvider().refreshAll();
            }
            return;
        }

        suppressSortPersistence = true;
        getUI().ifPresent(ui -> ui.beforeClientResponse(this, context -> suppressSortPersistence = false));

        restoringSortOrder = true;
        try {
            sort(gridSortOrders);
            if (stockGridType != StockGridType.STOCK && getDataProvider() != null) {
                getDataProvider().refreshAll();
            }
        } finally {
            restoringSortOrder = false;
            if (getUI().isEmpty()) {
                suppressSortPersistence = false;
            }
        }
    }

    private List<ColumnSort> loadPersistedSortOrders() {
        if (currentStockSavedQuery != null) {
            return currentStockSavedQuery.getSortOrders();
        }

        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) {
            return List.of();
        }

        Optional<Object> value = userUiSettingsService.getValueForCurrentUser(settingsKey);
        if (value.isEmpty()) {
            return List.of();
        }

        String serialized = String.valueOf(value.get());
        if (serialized.isBlank()) {
            return List.of();
        }

        List<ColumnSort> parsed = new ArrayList<>();
        for (String part : serialized.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2 || kv[0] == null || kv[0].isBlank()) {
                continue;
            }
            try {
                parsed.add(new ColumnSort(kv[0], org.springframework.data.domain.Sort.Direction.fromString(kv[1])));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved sort directions.
            }
        }
        return parsed;
    }

    private void persistSortOrders(List<ColumnSort> sortOrders) {
        if (currentStockSavedQuery != null) {
            currentStockSavedQuery.setSortOrders(sortOrders);
            return;
        }

        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) {
            return;
        }

        if (sortOrders == null || sortOrders.isEmpty()) {
            return;
        }

        String serialized = sortOrders.stream()
                .filter(order -> order != null && order.getColumnName() != null && !order.getColumnName().isBlank())
                .map(order -> order.getColumnName() + ":" + order.getColumnSortDirection().name())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        if (serialized.isBlank()) {
            return;
        }

        userUiSettingsService.setValueForCurrentUser(settingsKey, serialized);
    }

    private List<ColumnSort> toColumnSortList(List<GridSortOrder<Stock>> sortOrders) {
        List<ColumnSort> result = new ArrayList<>();
        for (GridSortOrder<Stock> order : sortOrders) {
            String key = order.getSorted() == null ? null : order.getSorted().getKey();
            String sortProperty = toSortProperty(key);
            if (sortProperty == null) {
                continue;
            }
            org.springframework.data.domain.Sort.Direction direction =
                    order.getDirection() == SortDirection.DESCENDING
                            ? org.springframework.data.domain.Sort.Direction.DESC
                            : org.springframework.data.domain.Sort.Direction.ASC;
            result.add(new ColumnSort(sortProperty, direction));
        }
        return result;
    }

    private List<GridSortOrder<Stock>> toGridSortOrders(List<ColumnSort> sortOrders) {
        List<GridSortOrder<Stock>> result = new ArrayList<>();
        for (ColumnSort sort : sortOrders) {
            if (sort == null || sort.getColumnName() == null) {
                continue;
            }
            String key = toColumnKey(sort.getColumnName());
            if (key == null) {
                continue;
            }
            Column<Stock> column = getColumnByKey(key);
            if (column == null) {
                continue;
            }
            SortDirection direction = sort.getColumnSortDirection() == org.springframework.data.domain.Sort.Direction.DESC
                    ? SortDirection.DESCENDING
                    : SortDirection.ASCENDING;
            result.add(new GridSortOrder<>(column, direction));
        }
        return result;
    }

    private String toSortProperty(String key) {
        if (key == null) {
            return null;
        }
        return switch (key) {
            case "name" -> "name";
            case "breeder" -> "breeder";
            case "breed" -> "breed";
            case "color" -> "color";
            case "genotype" -> "genotype";
            case "champno" -> "champNo";
            case "legs" -> "legs";
            case "regno" -> "regNo";
            case "status" -> "status";
            case "statusdate" -> "statusDate";
            case "tattoo" -> "tattoo";
            case "weight" -> "weight";
            case "aquired" -> "acquired";
            case "dob" -> "doB";
            case "salestatus" -> "saleStatus";
            case "invoice" -> "invoiceNumber";
            case "value" -> "stockValue";
            case "notes" -> "notes";
            case "external" -> "external";
            default -> null;
        };
    }

    private String toColumnKey(String sortProperty) {
        if (sortProperty == null) {
            return null;
        }
        return switch (sortProperty) {
            case "name" -> "name";
            case "breeder" -> "breeder";
            case "breed" -> "breed";
            case "color" -> "color";
            case "genotype" -> "genotype";
            case "champNo" -> "champno";
            case "legs" -> "legs";
            case "regNo" -> "regno";
            case "status" -> "status";
            case "statusDate" -> "statusdate";
            case "tattoo" -> "tattoo";
            case "weight" -> "weight";
            case "acquired" -> "aquired";
            case "doB" -> "dob";
            case "saleStatus" -> "salestatus";
            case "invoiceNumber" -> "invoice";
            case "stockValue" -> "value";
            case "notes" -> "notes";
            case "external" -> "external";
            default -> null;
        };
    }

    public void setValueLayout(Boolean valueLayout) {
        this.valueLayout = Boolean.TRUE.equals(valueLayout);
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
