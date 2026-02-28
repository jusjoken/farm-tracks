package ca.jusjoken.component;

import java.util.List;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
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

        setDataProvider();
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        setHeight("200px");

        addColumn(Litter::getPrefix).setHeader("Prefix").setSortable(true);
        addColumn(Litter::getName).setHeader("Name").setSortable(true);
        addColumn(Litter::getParentsFormatted).setHeader("Parents").setSortable(true);
        Grid.Column<Litter> bornColumn = addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY")).setHeader("Born").setSortable(true).setComparator(Litter::getDoB);
        addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY")).setHeader("Bred").setSortable(true).setComparator(Litter::getBred);
        addColumn(item -> item.getKitsCount()).setHeader(stockType.getNonBreederName()).setSortable(true);
        addColumn(item -> item.getSurvivalRate()).setHeader("Survival Rate").setSortable(true);
        addColumn(item -> item.getDiedKitsCount()).setHeader("Died").setSortable(true);
        addColumn(item -> item.getKitsSurvivedCount()).setHeader("Survived").setSortable(true);

        setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            StockGrid stockGrid = new StockGrid();
            stockGrid.setId(item.getId(), StockGrid.StockGridType.LITTER);
            stockGrid.createGrid();
            Layout kitListLayoutForOffset = new Layout(stockGrid);
            kitListLayoutForOffset.addClassNames(Padding.Left.LARGE);
            return kitListLayoutForOffset;
        }));        

        setMultiSort(true);
        sort(List.of(new GridSortOrder<>(bornColumn, SortDirection.DESCENDING)));
        
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

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
        setStockValues();
        refreshGrid();
    }    

    private void setDataProvider() {
        if(stockId != null){
            dataProvider = new ListDataProvider<>(litterService.getLitters(stock));
        } else {
            dataProvider = new ListDataProvider<>(litterService.getAllLitters());
        }
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        setDataProvider();
        dataProvider.refreshAll();
    }


}
