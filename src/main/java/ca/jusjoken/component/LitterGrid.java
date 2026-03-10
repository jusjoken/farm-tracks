package ca.jusjoken.component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.utility.BadgeVariant;

public class LitterGrid extends Grid<Litter>  implements ListRefreshNeededListener{
    private ListDataProvider<Litter> dataProvider;
    private Integer stockId;
    private StockType stockType;
    private Stock stock;
    private final StockService stockService;
    private final LitterService litterService;
    private final StockTypeService stockTypeService;
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;   
    private Registration resizeRegistration;
    private String parentNameFilter;
    private LitterDisplayMode litterDisplayMode = LitterDisplayMode.ALL;
    private Select<LitterViewStyle> viewStyleSelect;
    private Grid.Column<Litter> kitsCountColumn;
    private Grid.Column<Litter> survivalRateColumn;
    private Grid.Column<Litter> diedKitsCountColumn;
    private Grid.Column<Litter> kitsSurvivedCountColumn;
    private Grid.Column<Litter> bredColumn;
    private final TaskEditor taskEditor;
    private final LitterEditor litterEditor;
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    public enum LitterDisplayMode {
        ALL,
        ACTIVE,
        ARCHIVED
    }

    public static enum LitterViewStyle{
        TILE("Tile"), LIST("List");

        private final String shortName;
        
        private LitterViewStyle(String shortName) {
            this.shortName = shortName;
        }
        
        public String getShortName(){
            return shortName;
        }
        
        public static LitterViewStyle fromShortName(String shortName){
            switch (shortName){
                case "Tile" -> {
                    return LitterViewStyle.TILE;
                }
                case "List" -> {
                    return LitterViewStyle.LIST;
                }
                default -> throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
            }
        }
    }

    private LitterViewStyle currentViewStyle = LitterViewStyle.TILE;

    public LitterGrid() {
        this(null);
    }

    public LitterGrid(Integer stockId) {
        super(Litter.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.litterService = Registry.getBean(LitterService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
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
        
        if(null == currentViewStyle){
            configureListView(); // Default to list view if style is unrecognized
        }else // Configuration logic for the KitGrid
            switch (currentViewStyle) {
                case LIST -> configureListView();
                case TILE -> configureTileView();
                default -> configureListView(); // Default to list view if style is unrecognized
        }

        setEmptyStateText("No litters available to display");
        // setSizeFull();

        setLitterDataProvider();

        getHeaderRows().get(0).getCell(getColumnByKey("name")).setComponent(getViewSelectLayout());
        
    }

    private void configureTileView() {
        addColumn(litterCardRenderer).setKey("name").setHeader("Name");
    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        //setHeight("200px");

        addComponentColumn(litter -> {
            return litter.getnameAndPrefix(false,true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");

        addColumn(Litter::getParentsFormatted).setHeader("Parents").setSortable(true).setResizable(true).setAutoWidth(true);
        Grid.Column<Litter> bornColumn = addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY"))
                .setHeader("Born").setSortable(true).setComparator(Litter::getDoB).setResizable(true);
        bredColumn = addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY"))
                .setHeader("Bred").setSortable(true).setComparator(Litter::getBred).setResizable(true);
        kitsCountColumn = addColumn(item -> item.getKitsCount()).setHeader(stockType.getNonBreederName()).setSortable(true).setResizable(true);
        diedKitsCountColumn = addColumn(item -> item.getDiedKitsCount()).setHeader("Died").setSortable(true).setResizable(true);
        kitsSurvivedCountColumn = addColumn(item -> item.getKitsSurvivedCount()).setHeader("Survived").setSortable(true).setResizable(true);
        survivalRateColumn = addColumn(item -> item.getSurvivalRate()).setHeader("Survival Rate").setSortable(true).setResizable(true);
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
        sort(List.of(new GridSortOrder<>(bornColumn, SortDirection.DESCENDING)));
        updateFooterStats();
    }

    private Layout createKitsInLitterLayout(Litter litter){
        StockGrid stockGrid = new StockGrid();
        stockGrid.setId(litter.getId(), StockGrid.StockGridType.LITTER);

        //if this is being viewed on a mobile device set the view style to tile otherwise set it to list 
        stockGrid.setCurrentViewStyle(
            mobileDevice
                ? StockSavedQuery.StockViewStyle.TILE
                : StockSavedQuery.StockViewStyle.LIST
        );
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
            card.addToFooter(createBadge("Born: ",born, BadgeVariant.SUCCESS));
        }

        String bred = litter.getBred().format(DateTimeFormatter.ofPattern("MM-dd-YYYY"));
        if(!bred.isEmpty()){
            card.addToFooter(createBadge("Bred: ",bred, BadgeVariant.SUCCESS));
        }

        card.addToFooter(litter.getActiveBadge());

        card.addToFooter(createBadge("Kits: ",litter.getKitsCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(createBadge("Died: ",litter.getDiedKitsCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(createBadge("Survived: ",litter.getKitsSurvivedCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(createBadge("Rate: ",litter.getSurvivalRate().toString(), BadgeVariant.CONTRAST));

        setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            return createKitsInLitterLayout(item);
        }));        

        return card;
    }

    private Badge createBadge(String prefix, String text, BadgeVariant... variants){
        if(prefix!=null){
            text = prefix + ": " + text;
        }
        Badge badge = new Badge(text);
        badge.addClassNames(FontSize.SMALL, FontWeight.MEDIUM);
        badge.addThemeVariants(BadgeVariant.PILL);
        badge.addThemeVariants(variants);
        return badge;
    }

    private HorizontalLayout getViewSelectLayout() {

        viewStyleSelect = new Select<>();
        viewStyleSelect.setWidth("100px");
        viewStyleSelect.setItems(LitterViewStyle.values());
        viewStyleSelect.setItemLabelGenerator(style -> style.getShortName());
        viewStyleSelect.setValue(currentViewStyle != null ? currentViewStyle : LitterViewStyle.LIST);

        viewStyleSelect.addValueChangeListener(event -> {
            if (!event.isFromClient() || event.getValue() == null || event.getValue() == currentViewStyle) {
                return;
            }
            currentViewStyle = event.getValue();
            configureGrid();
            refreshGrid();
        });

        HorizontalLayout leftAlignedHeader = new HorizontalLayout(new Span("View Style: "), viewStyleSelect);
        leftAlignedHeader.setWidthFull();
        leftAlignedHeader.setPadding(false);
        leftAlignedHeader.setSpacing(false);
        leftAlignedHeader.setJustifyContentMode(HorizontalLayout.JustifyContentMode.START);
        leftAlignedHeader.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        return leftAlignedHeader;
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
        dataProvider.refreshAll();
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

        if(currentViewStyle == LitterViewStyle.LIST){
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
                String litterName = litterEntity.getDisplayName();

                // Div heading = new Div();
                // heading.setText(litterName);
                // heading.getStyle().set("text-align", "center");
                // heading.getStyle().set("font-weight", "bold");
                // heading.getStyle().set("padding", "8px");
                
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

    public LitterViewStyle getCurrentViewStyle() {
        return currentViewStyle;
    }

    public void setCurrentViewStyle(LitterViewStyle currentViewStyle) {
        this.currentViewStyle = currentViewStyle;
        viewStyleSelect.setValue(currentViewStyle);
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


}
