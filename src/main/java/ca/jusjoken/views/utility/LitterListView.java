package ca.jusjoken.views.utility;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.LitterGrid;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "litters", layout = MainLayout.class)
@PermitAll
@PageTitle("Litter List")
public class LitterListView extends Main implements ListRefreshNeededListener{
    private final LitterGrid litterGrid = new LitterGrid();
    private final StockTypeService stockTypeService;

    public LitterListView(StockTypeService stockTypeService) {
        this.stockTypeService = stockTypeService;

        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("overflow", "hidden");

        //add a select component to choose the stocktype for the litter grid and then set the littergrid stocktype based on the selection
        Select<StockType> stockTypeSelect = new Select<>();
        stockTypeSelect.setLabel("Stock Type");
        stockTypeSelect.setItems(stockTypeService.findAllStockTypes());
        stockTypeSelect.setItemLabelGenerator(StockType::getName);
        stockTypeSelect.addValueChangeListener(e -> {
           litterGrid.setStockType(e.getValue());
           litterGrid.refreshGrid();
        });

        //add a filter that filters the littergrid by the name of the father or mother
        TextField parentFilter = new TextField();
        parentFilter.setLabel("Parent Name Filter");
        parentFilter.setClearButtonVisible(true);
        parentFilter.setValueChangeMode(ValueChangeMode.EAGER);
        parentFilter.addValueChangeListener(e -> {
           litterGrid.setParentNameFilter(e.getValue());
           litterGrid.refreshGrid();
        });

        //add a vaadin radiobuttongroup to filter the littergrid by active, inactive or all litters
        Select<LitterGrid.LitterDisplayMode> displayModeSelect = new Select<>();
        displayModeSelect.setLabel("Display Mode");
        displayModeSelect.setItems(LitterGrid.LitterDisplayMode.values());
        displayModeSelect.setItemLabelGenerator(mode -> {
            switch (mode) {
                case ALL -> {
                    return "All Litters";   
                }
                case ACTIVE -> {
                    return "Active Litters";
                }
                case ARCHIVED -> {
                    return "Archived Litters";
                }
            }
            return "";
        });
        displayModeSelect.addValueChangeListener(e -> {
            litterGrid.setLitterDisplayMode(e.getValue());
            litterGrid.refreshGrid();
        });
        displayModeSelect.setValue(LitterGrid.LitterDisplayMode.ALL);

        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setPadding(false);
        filterLayout.setSpacing(true);
        filterLayout.setWidthFull();
        filterLayout.getStyle().set("flex", "0 0 auto");
        filterLayout.add(stockTypeSelect, parentFilter, displayModeSelect);
        add(filterLayout);

        parentFilter.setAutofocus(true);

        //default to stocktype rabbits
        stockTypeSelect.setValue(stockTypeService.findRabbits());

        add(litterGrid);
        litterGrid.setSizeFull();
        litterGrid.getStyle().set("flex", "1 1 auto");
        litterGrid.getStyle().set("min-height", "0");

        litterGrid.refreshGrid();
    }

    @Override
    public void listRefreshNeeded() {
        litterGrid.refreshGrid();
    }

}
