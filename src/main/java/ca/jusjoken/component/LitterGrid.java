package ca.jusjoken.component;

import java.util.List;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockTypeService;

public class LitterGrid extends Grid<Litter> {
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
        this.stockId = stockId;
        setStockValues();
        configureGrid();
    }

    private void configureGrid() {

        setEmptyStateText("No litters available to display");
        setSizeFull();

        setDataProvider();
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        //setHeight("200px");

        addComponentColumn(litter -> {
            return litter.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");

        addColumn(Litter::getParentsFormatted).setHeader("Parents").setSortable(true).setResizable(true);
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
            StockGrid stockGrid = new StockGrid();
            stockGrid.setId(item.getId(), StockGrid.StockGridType.LITTER);

            //if this is being viewed on a mobile device set the view style to tile otherwise set it to list 
            stockGrid.setCurrentViewStyle(
                mobileDevice
                    ? StockSavedQuery.StockViewStyle.TILE
                    : StockSavedQuery.StockViewStyle.LIST
            );
            stockGrid.setHeight("270px");
            stockGrid.setShowViewStyleChoice(true);
            stockGrid.createGrid();
            Layout kitListLayoutForOffset = new Layout(stockGrid);
            kitListLayoutForOffset.addClassNames(Padding.Left.LARGE);
            return kitListLayoutForOffset;
        }));        

        setMultiSort(true);
        sort(List.of(new GridSortOrder<>(bornColumn, SortDirection.DESCENDING)));
        updateFooterStats();
        
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

    private void setDataProvider() {
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
        setDataProvider();
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
        if (kitsCountColumn == null || survivalRateColumn == null || diedKitsCountColumn == null
                || kitsSurvivedCountColumn == null || dataProvider == null || bredColumn == null) {
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

        bredColumn.setFooter("Litters: " + litterCount);
        kitsCountColumn.setFooter("Kits: " + kitsTotal);
        diedKitsCountColumn.setFooter("Died: " + diedTotal);
        kitsSurvivedCountColumn.setFooter("Survived: " + survivedTotal);
        survivalRateColumn.setFooter(survivalCount == 0 ? "Avg: -" : String.format("Survival: %.1f%%", survivalSum / survivalCount));
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

    private Grid.Column<Litter> kitsCountColumn;
    private Grid.Column<Litter> survivalRateColumn;
    private Grid.Column<Litter> diedKitsCountColumn;
    private Grid.Column<Litter> kitsSurvivedCountColumn;
    private Grid.Column<Litter> bredColumn;
}
