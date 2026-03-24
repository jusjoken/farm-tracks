package ca.jusjoken.component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
    private StockService stockService;
    private LitterService litterService;
    private StockTypeService stockTypeService;
    private UserUiSettingsService userUiSettingsService;
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;   
    private Registration resizeRegistration;
    private String parentNameFilter;
    private Integer litterIdFilter;
    private LitterDisplayMode litterDisplayMode = LitterDisplayMode.ALL;
    private Grid.Column<Litter> kitsCountColumn;
    private Grid.Column<Litter> survivalRateColumn;
    private Grid.Column<Litter> diedKitsCountColumn;
    private Grid.Column<Litter> kitsSurvivedCountColumn;
    private Grid.Column<Litter> bredColumn;
    private Grid.Column<Litter> bornColumn;
    private Registration sortListenerRegistration;
    private TaskEditor taskEditor;
    private LitterEditor litterEditor;
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private final Map<Integer, Integer> assignedKitCountsByLitterId = new HashMap<>();
    private Boolean displayAsTile = false;
    private String preferenceScopeKey;
    private static final String ACTION_COLUMN_KEY = "row-actions";

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
        this.stockId = stockId;
        initializeServicesIfNeeded();
        setStockValues();
        createContextMenu(this);
        createGrid();
    }

    private boolean initializeServicesIfNeeded() {
        try {
            if (stockService == null) {
                stockService = Registry.getBean(StockService.class);
            }
            if (litterService == null) {
                litterService = Registry.getBean(LitterService.class);
            }
            if (stockTypeService == null) {
                stockTypeService = Registry.getBean(StockTypeService.class);
            }
            if (userUiSettingsService == null) {
                userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
            }
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private LitterEditor getOrCreateLitterEditor() {
        if (litterEditor != null) {
            return litterEditor;
        }
        try {
            litterEditor = new LitterEditor();
            litterEditor.addListener(this);
            return litterEditor;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private TaskEditor getOrCreateTaskEditor() {
        if (taskEditor != null) {
            return taskEditor;
        }
        try {
            taskEditor = new TaskEditor();
            return taskEditor;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public void createGrid(){
        configureGrid();
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();
        removeClassName("mobile-tile-scroll-fix");
        
        if(displayAsTile){
            addClassName("mobile-tile-scroll-fix");
            configureTileView();
        }else{
            configureListView();
        }

        setEmptyStateText("No litters available to display");
        // setSizeFull();

        setLitterDataProvider();

    }

    private void configureTileView() {
        setPartNameGenerator(item -> null);
        addColumn(litterCardRenderer).setKey("name");
    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        setPartNameGenerator(item -> null);

        addRowActionsColumn();

        //setHeight("200px");

        addComponentColumn(litter -> {
            return litter.getnameAndPrefix(false,true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");

        addColumn(Litter::getParentsFormatted).setHeader("Parents").setSortable(true).setResizable(true).setAutoWidth(true).setKey("parents");
        bornColumn = addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY"))
            .setHeader("Born").setSortable(true).setComparator(Litter::getDoB).setResizable(true).setKey("born");
        bredColumn = addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY"))
            .setHeader("Bred").setSortable(true).setComparator(Litter::getBred).setResizable(true).setKey("bred");
        String kitsHeader = (stockType != null && stockType.getNonBreederName() != null)
            ? stockType.getNonBreederName()
            : "Kits";
        kitsCountColumn = addColumn(this::getDisplayKitsCount).setHeader(kitsHeader).setSortable(true).setResizable(true).setKey("kits");
        diedKitsCountColumn = addColumn(item -> item.getDiedKitsCount()).setHeader("Died").setSortable(true).setResizable(true).setKey("died");
        kitsSurvivedCountColumn = addColumn(this::getDisplayKitsSurvivedCount).setHeader("Survived").setSortable(true).setResizable(true).setKey("survived");
        survivalRateColumn = addColumn(this::getDisplaySurvivalRate).setHeader("Survival Rate").setSortable(true).setResizable(true).setKey("survival");
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
        card.getStyle().set("margin", "var(--lumo-space-xs) 0");
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeader(litter.getnameAndPrefix(false,true));

        card.setHeaderSuffix(createTileHeaderSuffix(litter.getParentsFormatted()));

        String born = litter.getDoB().format(DateTimeFormatter.ofPattern("MM-dd-YYYY"));
        if(!born.isEmpty()){
            card.addToFooter(UIUtilities.createBadge("Born: ",born, BadgeVariant.SUCCESS));
        }

        String bred = litter.getBred().format(DateTimeFormatter.ofPattern("MM-dd-YYYY"));
        if(!bred.isEmpty()){
            card.addToFooter(UIUtilities.createBadge("Bred: ",bred, BadgeVariant.SUCCESS));
        }

        card.addToFooter(litter.getActiveBadge());

        card.addToFooter(UIUtilities.createBadge("Kits: ", String.valueOf(getDisplayKitsCount(litter)), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Died: ",litter.getDiedKitsCount().toString(), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Survived: ", String.valueOf(getDisplayKitsSurvivedCount(litter)), BadgeVariant.CONTRAST));
        card.addToFooter(UIUtilities.createBadge("Rate: ", getDisplaySurvivalRate(litter), BadgeVariant.CONTRAST));

        return card;
    }

    private void addRowActionsColumn() {
        addComponentColumn(litter -> createRowMenuButton())
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

    private HorizontalLayout createTileHeaderSuffix(String text) {
        Span textSpan = new Span(text == null ? "" : text);
        textSpan.getStyle().set("overflow", "hidden");
        textSpan.getStyle().set("text-overflow", "ellipsis");
        textSpan.getStyle().set("white-space", "nowrap");
        textSpan.getStyle().set("min-width", "0");
        textSpan.getStyle().set("flex", "1 1 auto");

        HorizontalLayout headerSuffix = UIUtilities.getHorizontalLayoutNoWidthCentered();
        headerSuffix.setPadding(false);
        headerSuffix.setSpacing(true);
        headerSuffix.setAlignItems(HorizontalLayout.Alignment.CENTER);
        headerSuffix.getStyle().set("min-width", "0");
        headerSuffix.getStyle().set("max-width", "100%");
        headerSuffix.add(textSpan, createRowMenuButton());
        return headerSuffix;
    }

    private void setStockValues() {
        if (!initializeServicesIfNeeded()) {
            stock = null;
            return;
        }
        if(stockId != null){
            stock = stockService.findById(stockId);
            stockType = stock == null ? null : stock.getStockType();
        } else if (stockType != null) {
            stock = null;
        } else {
            stock = null;
            stockType = stockTypeService == null ? null : stockTypeService.findRabbits();
        }
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
        setStockValues();
        refreshGrid();
    }    

    private void setLitterDataProvider() {
        if (!initializeServicesIfNeeded()) {
            dataProvider = new ListDataProvider<>(new ArrayList<>());
            setDataProvider(dataProvider);
            refreshAssignedKitCounts();
            applyFilters();
            return;
        }
        if(stockId != null){
            dataProvider = new ListDataProvider<>(litterService.getLitters(stock));
        } else if (stockType != null) {
            dataProvider = new ListDataProvider<>(litterService.getAllLitters(stockType));
        } else {
            dataProvider = new ListDataProvider<>(litterService.getAllLitters());
        }
        setDataProvider(dataProvider);
        refreshAssignedKitCounts();
        applyFilters();
    }

    public void refreshGrid() {
        setLitterDataProvider();
        updateSortOrder();
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
        if (sameStockType(this.stockType, stockType)) {
            return;
        }
        this.stockType = stockType;
    }

    private boolean sameStockType(StockType left, StockType right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        Integer leftId = left.getId();
        Integer rightId = right.getId();
        if (leftId != null && rightId != null) {
            return leftId.equals(rightId);
        }
        return left.equals(right);
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

    public void setLitterIdFilter(Integer litterIdFilter) {
        this.litterIdFilter = litterIdFilter;
        applyFilters();
    }

    public void expandFilteredLitterDetails() {
        if (litterIdFilter == null || dataProvider == null) {
            return;
        }

        for (Litter litter : dataProvider.getItems()) {
            if (litter == null || litter.getId() == null || !litter.getId().equals(litterIdFilter)) {
                continue;
            }

            Litter targetLitter = litter;
            getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this, context -> {
                dataProvider.refreshAll();
                setDetailsVisible(targetLitter, true);
                scrollToItem(targetLitter);
            }));
            break;
        }
    }

    private void applyFilters() {
        if (dataProvider == null) {
            return;
        }

        dataProvider.clearFilters();

        if (litterIdFilter != null) {
            dataProvider.addFilter(litter -> litter != null && litter.getId() != null && litter.getId().equals(litterIdFilter));
        }

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
            kitsTotal += getDisplayKitsCount(litter);
            diedTotal += safeInt(litter.getDiedKitsCount());
            survivedTotal += getDisplayKitsSurvivedCount(litter);

            Double survivalValue = toSurvivalNumber(getDisplaySurvivalRate(litter));
            if (survivalValue != null) {
                survivalSum += survivalValue;
                survivalCount++;
            }
        }

        String avgSurvivalText = (survivalCount == 0 ? "-" : String.format("%.1f%%", survivalSum / survivalCount));

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
            Grid.Column<Litter> tileColumn = getColumnByKey("name");
            if (tileColumn == null) {
                return;
            }

            if (mobileDevice) {
                // Keep the footer to one compact line on mobile and move full stats into a dialog.
                final String compactFooterText = "Litters: " + litterCount + " | Kits: " + kitsTotal;
                final String fullStatsText = "Litters: " + litterCount
                    + "\nKits: " + kitsTotal
                    + "\nDied: " + diedTotal
                    + "\nSurvived: " + survivedTotal
                    + "\nSurvival: " + avgSurvivalText;

                HorizontalLayout compactFooter = UIUtilities.getHorizontalLayoutNoWidthCentered();
                compactFooter.setWidthFull();
                compactFooter.setPadding(false);
                compactFooter.setSpacing(true);

                Span compactSummary = new Span(compactFooterText);
                compactSummary.getStyle().set("white-space", "nowrap");
                compactSummary.getStyle().set("overflow", "hidden");
                compactSummary.getStyle().set("text-overflow", "ellipsis");
                compactSummary.getStyle().set("flex", "1 1 auto");

                Button moreButton = new Button("More");
                moreButton.getStyle().set("flex", "0 0 auto");
                moreButton.addClickListener(click -> {
                    ConfirmDialog statsDialog = new ConfirmDialog();
                    statsDialog.setHeader("Litter Totals");
                    statsDialog.setText(fullStatsText);
                    statsDialog.setCancelable(false);
                    statsDialog.setConfirmText("Close");
                    statsDialog.open();
                });

                compactFooter.add(compactSummary, moreButton);
                tileColumn.setFooter(compactFooter);
            } else {
                tileColumn.setFooter(
                    "Litters: " + litterCount
                    + " | Kits: " + kitsTotal
                    + " | Died: " + diedTotal
                    + " | Survived: " + survivedTotal
                    + " | Survival: " + avgSurvivalText
                );
            }
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
        boolean litterIdMatch = litterIdFilter == null || (litter != null && litter.getId() != null && litter.getId().equals(litterIdFilter));
        return parentMatch && statusMatch && litterIdMatch;
    }

    private boolean matchesDisplayMode(Litter litter) {
        Boolean active = litter == null ? null : litter.getActive();
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int getDisplayKitsCount(Litter litter) {
        if (litter == null) {
            return 0;
        }
        Integer litterId = litter.getId();
        if (litterId == null) {
            return safeInt(litter.getKitsCount());
        }
        return assignedKitCountsByLitterId.getOrDefault(litterId, safeInt(litter.getKitsCount()));
    }

    private int getDisplayKitsSurvivedCount(Litter litter) {
        int kitsCount = getDisplayKitsCount(litter);
        int diedCount = safeInt(litter == null ? null : litter.getDiedKitsCount());
        return Math.max(0, kitsCount - diedCount);
    }

    private String getDisplaySurvivalRate(Litter litter) {
        int survived = getDisplayKitsSurvivedCount(litter);
        int died = safeInt(litter == null ? null : litter.getDiedKitsCount());
        int total = survived + died;
        if (total <= 0) {
            return "0%";
        }
        double ratio = (double) survived / (double) total;
        return String.format("%.0f%%", ratio * 100.0);
    }

    private void refreshAssignedKitCounts() {
        assignedKitCountsByLitterId.clear();
        if (dataProvider == null || !initializeServicesIfNeeded() || stockService == null) {
            return;
        }

        List<Integer> litterIds = new ArrayList<>();
        for (Litter litter : dataProvider.getItems()) {
            if (litter == null || litter.getId() == null) {
                continue;
            }
            litterIds.add(litter.getId());
        }

        if (litterIds.isEmpty()) {
            return;
        }

        Map<Integer, Long> countsByLitterId = stockService.getKitsAssignedCountsForLitters(litterIds);
        for (Integer litterId : litterIds) {
            Long count = countsByLitterId.getOrDefault(litterId, 0L);
            assignedKitCountsByLitterId.put(litterId, count.intValue());
        }
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
                    LitterEditor editor = getOrCreateLitterEditor();
                    if (editor != null) {
                        editor.dialogOpen(new Litter(), LitterEditor.DialogMode.CREATE, litterEntity.getStockType());
                    }
                    
                });

                GridMenuItem<Litter> addTaskMenu = menu.addItem(new Item("Add Task", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addTaskMenu.addMenuItemClickListener(click -> {
                    //open task edit dialog
                    //create a new task for this litter with default values
                    Task newTask = new Task();
                    newTask.setLinkType(Utility.TaskLinkType.LITTER);
                    newTask.setLinkLitterId(litterEntity.getId());

                    TaskEditor editor = getOrCreateTaskEditor();
                    if (editor != null) {
                        editor.dialogOpen(newTask, TaskEditor.DialogMode.CREATE, litterEntity.getStockType());
                    }
                    
                });
                menu.addSeparator();

                GridMenuItem<Litter> editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
                editMenu.addMenuItemClickListener(click -> {
                    //open litter edit dialog with the selected litter
                    LitterEditor editor = getOrCreateLitterEditor();
                    if (editor != null) {
                        editor.dialogOpen(litterEntity, LitterEditor.DialogMode.EDIT, litterEntity.getStockType());
                    }
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
                        if (initializeServicesIfNeeded() && litterService != null) {
                            litterService.deleteById(litterEntity.getId());
                        }
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
        if (!initializeServicesIfNeeded() || userUiSettingsService == null) {
            return;
        }
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
        if (!initializeServicesIfNeeded() || userUiSettingsService == null) {
            return;
        }
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
        if (!initializeServicesIfNeeded() || userUiSettingsService == null) {
            return;
        }
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
        if (!initializeServicesIfNeeded() || userUiSettingsService == null) {
            return null;
        }
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
