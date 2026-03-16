package ca.jusjoken.component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.UserUiSettingsService;
import ca.jusjoken.utility.BadgeVariant;

public class LitterGrid extends Grid<Litter>  implements ListRefreshNeededListener{
    private ListDataProvider<Litter> dataProvider;
    private Integer stockId;
    private StockType stockType;
    private Stock stock;
    private final StockService stockService;
    private final LitterService litterService;
    private final StockTypeService stockTypeService;
    private final UserUiSettingsService userUiSettingsService;
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;   
    private Registration resizeRegistration;
    private String parentNameFilter;
    private LitterDisplayMode litterDisplayMode = LitterDisplayMode.ALL;
    private Grid.Column<Litter> kitsCountColumn;
    private Grid.Column<Litter> survivalRateColumn;
    private Grid.Column<Litter> diedKitsCountColumn;
    private Grid.Column<Litter> kitsSurvivedCountColumn;
    private Grid.Column<Litter> bredColumn;
    private Grid.Column<Litter> bornColumn;
    private Registration sortListenerRegistration;
    private final TaskEditor taskEditor;
    private final LitterEditor litterEditor;
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private Boolean displayAsTile = false;
    private String preferenceScopeKey;

    public enum LitterDisplayMode {
        ALL,
        ACTIVE,
        ARCHIVED
    }

    public LitterGrid() {
        this(null);
    }

    public LitterGrid(Integer stockId) {
        super(Litter.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
        this.stockId = stockId;
        this.taskEditor = new TaskEditor();
        this.litterEditor = new LitterEditor();
        this.litterEditor.addListener(this);
        setStockValues();
        createContextMenu(this);
        createGrid();
    }

    public void createGrid(){
        configureGrid();
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();
        
        if(displayAsTile){
            configureTileView();
        }else{
            configureListView();
        }

        setEmptyStateText("No litters available to display");
        // setSizeFull();

        setLitterDataProvider();

    }

    private void configureTileView() {
        addColumn(litterCardRenderer).setKey("name");
    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        //setHeight("200px");

        addComponentColumn(litter -> {
            return litter.getnameAndPrefix(false,true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");

        addColumn(Litter::getParentsFormatted).setHeader("Parents").setSortable(true).setResizable(true).setAutoWidth(true).setKey("parents");
        bornColumn = addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY"))
            .setHeader("Born").setSortable(true).setComparator(Litter::getDoB).setResizable(true).setKey("born");
        bredColumn = addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY"))
            .setHeader("Bred").setSortable(true).setComparator(Litter::getBred).setResizable(true).setKey("bred");
        kitsCountColumn = addColumn(item -> item.getKitsCount()).setHeader(stockType.getNonBreederName()).setSortable(true).setResizable(true).setKey("kits");
        diedKitsCountColumn = addColumn(item -> item.getDiedKitsCount()).setHeader("Died").setSortable(true).setResizable(true).setKey("died");
        kitsSurvivedCountColumn = addColumn(item -> item.getKitsSurvivedCount()).setHeader("Survived").setSortable(true).setResizable(true).setKey("survived");
        survivalRateColumn = addColumn(item -> item.getSurvivalRate()).setHeader("Survival Rate").setSortable(true).setResizable(true).setKey("survival");
        Grid.Column<Litter> statusColumn = addComponentColumn(litter -> {
            return litter.getActiveBadge();
        }).setHeader("Status")
          .setAutoWidth(true)
          .setResizable(true)
          .setKey("status")
          .setSortable(true);

        statusColumn.setFrozenToEnd(true);

        setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            return createKitsInLitterLayout(item);
        }));        

        setMultiSort(true);
        if (sortListenerRegistration != null) sortListenerRegistration.remove();
        sortListenerRegistration = addSortListener(e -> saveSortPreference(e.getSortOrder()));
        updateSortOrder();
        updateFooterStats();
    }

    private Layout createKitsInLitterLayout(Litter litter){
        StockGrid stockGrid = new StockGrid();
        stockGrid.setId(litter.getId(), StockGrid.StockGridType.LITTER);

        //if this is being viewed on a mobile device set the view style to tile otherwise set it to list 
        stockGrid.setDisplayAsTile(mobileDevice);
        stockGrid.setHeight("270px");
        stockGrid.createGrid();
        Layout kitListLayoutForOffset = new Layout(stockGrid);
        kitListLayoutForOffset.addClassNames(Padding.Left.LARGE);
        return kitListLayoutForOffset;
    }

    private final ComponentRenderer<Component, Litter> litterCardRenderer = new ComponentRenderer<>(
            litterEntity -> {
                // return createListItemLayout(litterEntity);
                return createListItemCard(litterEntity);
            });    

    private Card createListItemCard(Litter litter){
        Card card = new Card();
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeader(litter.getnameAndPrefix(false,true));
        card.setHeaderSuffix(new Span(litter.getParentsFormatted()));

        String born = litter.getDoB().format(DateTimeFormatter.ofPattern("MM-dd-YYYY"));
        if(!born.isEmpty()){
            card.addToFooter(UIUtilities.createBadge("Born: ",born, BadgeVariant.SUCCESS));
        }

        String bred = litter.getBred().format(DateTimeFormatter.ofPattern("MM-dd-YYYY"));
        if(!bred.isEmpty()){
            card.addToFooter(UIUtilities.createBadge("Bred: ",bred, BadgeVariant.SUCCESS));
        }

        card.addToFooter(litter.getActiveBadge());

        card.addToFooter(UIUtilities.createBadge("Kits: ",litter.getKitsCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Died: ",litter.getDiedKitsCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Survived: ",litter.getKitsSurvivedCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Rate: ",litter.getSurvivalRate().toString(), BadgeVariant.CONTRAST));

        setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            return createKitsInLitterLayout(item);
        }));        

        return card;
    }

    private void setStockValues() {
        if(stockId != null){
            stock = stockService.findById(stockId);
            stockType = stock.getStockType();
        } else if (stockType != null) {
            stock = null;
        } else {
            stock = null;
            stockType = stockTypeService.findRabbits();
        }
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
        setStockValues();
        refreshGrid();
    }    

    private void setLitterDataProvider() {
        if(stockId != null){
            dataProvider = new ListDataProvider<>(litterService.getLitters(stock));
        } else if (stockType != null) {
            dataProvider = new ListDataProvider<>(litterService.getAllLitters(stockType));
        } else {
            dataProvider = new ListDataProvider<>(litterService.getAllLitters());
        }
        setDataProvider(dataProvider);
        applyFilters();
    }

    public void refreshGrid() {
        setLitterDataProvider();
        updateSortOrder();
        updateFooterStats();
    }

    private void updateSortOrder() {
        if (getColumns().isEmpty()) {
            return;
        }
        List<GridSortOrder<Litter>> savedSort = loadSortPreference();
        if (savedSort != null && !savedSort.isEmpty()) {
            sort(savedSort);
        } else if (bornColumn != null) {
            sort(List.of(new GridSortOrder<>(bornColumn, SortDirection.DESCENDING)));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Initial width (non-deprecated approach)
        attachEvent.getUI().getPage()
                .executeJs("return window.innerWidth;")
                .then(Integer.class, width -> {
                    updateMobileFlag(width);
                });

        // Keep it updated when browser is resized
        resizeRegistration = attachEvent.getUI().getPage()
                .addBrowserWindowResizeListener(event -> {
                    updateMobileFlag(event.getWidth());
                });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (resizeRegistration != null) {
            resizeRegistration.remove();
            resizeRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    private void updateMobileFlag(int width) {
        boolean isMobileNow = width < MOBILE_BREAKPOINT_PX;
        if (this.mobileDevice != isMobileNow) {
            this.mobileDevice = isMobileNow;
            getDataProvider().refreshAll();
        }
    }

    public StockType getStockType() {
        return stockType;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public void setParentNameFilter(String value) {
        parentNameFilter = (value == null) ? "" : value.trim().toLowerCase();
        applyFilters();
    }

    public LitterDisplayMode getLitterDisplayMode() {
        return litterDisplayMode;
    }

    public void setLitterDisplayMode(LitterDisplayMode litterDisplayMode) {
        this.litterDisplayMode = (litterDisplayMode == null) ? LitterDisplayMode.ALL : litterDisplayMode;
        applyFilters();
    }

    private void applyFilters() {
        if (dataProvider == null) {
            return;
        }

        dataProvider.clearFilters();

        if (parentNameFilter != null && !parentNameFilter.isEmpty()) {
            dataProvider.addFilter(litter -> {
                String mother = (litter.getMother() != null && litter.getMother().getName() != null)
                        ? litter.getMother().getName().toLowerCase()
                        : "";
                String father = (litter.getFather() != null && litter.getFather().getName() != null)
                        ? litter.getFather().getName().toLowerCase()
                        : "";
                return mother.contains(parentNameFilter) || father.contains(parentNameFilter);
            });
        }

        if (litterDisplayMode != LitterDisplayMode.ALL) {
            dataProvider.addFilter(this::matchesDisplayMode);
        }

        dataProvider.refreshAll();
        updateFooterStats();
    }

    private void updateFooterStats() {
        if (dataProvider == null) {
            return;
        }

        int litterCount = 0;
        int kitsTotal = 0;
        int diedTotal = 0;
        int survivedTotal = 0;
        double survivalSum = 0.0;
        int survivalCount = 0;

        for (Litter litter : dataProvider.getItems()) {
            if (!matchesAllCurrentFilters(litter)) {
                continue;
            }

            litterCount++;
            kitsTotal += safeInt(litter.getKitsCount());
            diedTotal += safeInt(litter.getDiedKitsCount());
            survivedTotal += safeInt(litter.getKitsSurvivedCount());

            Double survivalValue = toSurvivalNumber(litter.getSurvivalRate());
            if (survivalValue != null) {
                survivalSum += survivalValue;
                survivalCount++;
            }
        }

        if(!displayAsTile){
            if (kitsCountColumn == null || survivalRateColumn == null || diedKitsCountColumn == null
                    || kitsSurvivedCountColumn == null || dataProvider == null || bredColumn == null) {
                return;
            }

            bredColumn.setFooter("Litters: " + litterCount);
            kitsCountColumn.setFooter("Kits: " + kitsTotal);
            diedKitsCountColumn.setFooter("Died: " + diedTotal);
            kitsSurvivedCountColumn.setFooter("Survived: " + survivedTotal);
            survivalRateColumn.setFooter(survivalCount == 0 ? "Avg: -" : String.format("Survival: %.1f%%", survivalSum / survivalCount));

        }else{
            //for tile view put all the stats in the first column as it is the only column
            getColumnByKey("name").setFooter("Litters: " + litterCount + " | Kits: " + kitsTotal + " | Died: " + diedTotal + " | Survived: " + survivedTotal + " | Survival: " + (survivalCount == 0 ? "-" : String.format("%.1f%%", survivalSum / survivalCount)));
        }


    }

    private boolean matchesAllCurrentFilters(Litter litter) {
        boolean parentMatch = true;
        if (parentNameFilter != null && !parentNameFilter.isEmpty()) {
            String mother = (litter.getMother() != null && litter.getMother().getName() != null)
                    ? litter.getMother().getName().toLowerCase()
                    : "";
            String father = (litter.getFather() != null && litter.getFather().getName() != null)
                    ? litter.getFather().getName().toLowerCase()
                    : "";
            parentMatch = mother.contains(parentNameFilter) || father.contains(parentNameFilter);
        }

        boolean statusMatch = litterDisplayMode == LitterDisplayMode.ALL || matchesDisplayMode(litter);
        return parentMatch && statusMatch;
    }

    private boolean matchesDisplayMode(Litter litter) {
        Boolean active = resolveActiveState(litter);
        if (active == null) {
            return true;
        }
        if (litterDisplayMode == LitterDisplayMode.ACTIVE) {
            return active;
        }
        if (litterDisplayMode == LitterDisplayMode.ARCHIVED) {
            return !active;
        }
        return true;
    }

    private Boolean resolveActiveState(Litter litter) {
        try {
            Object value = litter.getClass().getMethod("isActive").invoke(litter);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Exception ignored) {
            // ...existing code...
        }

        try {
            Object value = litter.getClass().getMethod("getActive").invoke(litter);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        } catch (Exception ignored) {
            // ...existing code...
        }

        return null;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Double toSurvivalNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = value.toString().trim().replace("%", "");
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private GridContextMenu<Litter> createContextMenu(Grid<Litter> grid) {
        GridContextMenu<Litter> menu = new GridContextMenu<>(grid);

        menu.setDynamicContentHandler(litterEntity -> {
            if(litterEntity==null){ //this is when the header row is right clicked
                return false;
            }else{
                menu.removeAll();
                GridMenuItem<Litter> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
                displayAsTileMenu.setCheckable(true);
                displayAsTileMenu.setChecked(displayAsTile);
                displayAsTileMenu.addMenuItemClickListener(click -> {
                    displayAsTile = displayAsTileMenu.isChecked();
                    saveDisplayAsTilePreference();
                    configureGrid();
                    listRefreshNeeded();
                });
                menu.addSeparator();

                String litterName = litterEntity.getDisplayName();

                // //add a label at the top with the stock name
                menu.addComponentAsFirst(UIUtilities.getContextMenuHeader(litterName));

                String addNewMenuTitle = "Add new litter";
                GridMenuItem<Litter> addNewMenu = menu.addItem(new Item(addNewMenuTitle, Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addNewMenu.addMenuItemClickListener(click -> {
                    //open litter edit dialog - to be create yet
                    litterEditor.dialogOpen(new Litter(), LitterEditor.DialogMode.CREATE, litterEntity.getStockType());
                    
                });

                GridMenuItem<Litter> addTaskMenu = menu.addItem(new Item("Add Task", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addTaskMenu.addMenuItemClickListener(click -> {
                    //open task edit dialog
                    //create a new task for this litter with default values
                    Task newTask = new Task();
                    newTask.setLinkType(Utility.TaskLinkType.LITTER);
                    newTask.setLinkLitterId(litterEntity.getId());

                    taskEditor.dialogOpen(newTask, TaskEditor.DialogMode.CREATE, litterEntity.getStockType());
                    
                });
                menu.addSeparator();

                GridMenuItem<Litter> editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
                editMenu.addMenuItemClickListener(click -> {
                    //open litter edit dialog
                });
                menu.addSeparator();
                GridMenuItem<Litter> deleteMenu = menu.addItem(new Item("Delete", Utility.ICONS.ACTION_DELETE.getIconSource()));
                deleteMenu.addMenuItemClickListener(click -> {
                    //ask for confirmation and then delete the litter and all associated kits
                    ConfirmDialog confirm = new ConfirmDialog();
                    confirm.setHeader("Delete litter:" + litterEntity.getDisplayName());
                    confirm.setText("Are you sure you want to delete this litter? This will also delete all associated kits.");
                    confirm.setCancelable(true);
                    confirm.setCancelText("No");
                    confirm.setConfirmText("Yes");
                    confirm.addConfirmListener(event -> {
                        System.out.println("Deleting litter with ID: " + litterEntity.getId());
                        litterService.deleteById(litterEntity.getId());
                        listRefreshNeeded();
                        confirm.close();
                    });
                    confirm.addCancelListener(event -> {
                        confirm.close();
                    });
                    confirm.open();
                    return;
                });

                return true;
            }
        });

        return menu;
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
        configureGrid();
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

    private void saveDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }
        userUiSettingsService.setBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(displayAsTile));
    }

    private String getDisplayAsTilePreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".displayAsTile";
    }

    private void saveSortPreference(List<GridSortOrder<Litter>> sortOrders) {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) return;
        if (sortOrders == null || sortOrders.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (GridSortOrder<Litter> order : sortOrders) {
            String key = order.getSorted().getKey();
            if (key == null) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(key).append(":").append(order.getDirection().name());
        }
        if (sb.isEmpty()) return;
        userUiSettingsService.setValueForCurrentUser(settingsKey, sb.toString());
    }

    private List<GridSortOrder<Litter>> loadSortPreference() {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) return null;
        Optional<Object> value = userUiSettingsService.getValueForCurrentUser(settingsKey);
        if (value.isEmpty()) return null;
        String serialized = String.valueOf(value.get());
        if (serialized.isBlank()) return null;
        List<GridSortOrder<Litter>> result = new ArrayList<>();
        for (String part : serialized.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            Grid.Column<Litter> col = getColumnByKey(kv[0]);
            if (col == null) continue;
            try {
                SortDirection dir = SortDirection.valueOf(kv[1]);
                result.add(new GridSortOrder<>(col, dir));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved sort directions.
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String getSortPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) return null;
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".sortOrder";
    }

}
