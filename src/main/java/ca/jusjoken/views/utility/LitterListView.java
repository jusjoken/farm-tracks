package ca.jusjoken.views.utility;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.LitterGrid;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "litters", layout = MainLayout.class)
@PermitAll
@PageTitle("Litter List")
public class LitterListView extends Main implements ListRefreshNeededListener, BeforeEnterObserver{
    private final LitterGrid litterGrid = new LitterGrid();
    private final StockTypeService stockTypeService;
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;   
    private Registration resizeRegistration;
    private Integer queryLitterIdFilter;

    public LitterListView(StockTypeService stockTypeService) {
        this.stockTypeService = stockTypeService;
        applyDeviceScopedPreferenceKey();

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
        displayModeSelect.setLabel("Include Litters");
        displayModeSelect.setItems(
            LitterGrid.LitterDisplayMode.ALL,
            LitterGrid.LitterDisplayMode.ACTIVE,
            LitterGrid.LitterDisplayMode.ARCHIVED
        );
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
        filterLayout.setPadding(true);
        filterLayout.setSpacing(true);
        filterLayout.setWidthFull();
        filterLayout.getStyle().set("flex", "0 0 auto");
        filterLayout.add(parentFilter, stockTypeSelect, displayModeSelect);
        add(filterLayout);

        parentFilter.setAutofocus(true);

        //default to stocktype rabbits
        stockTypeSelect.setValue(stockTypeService.findRabbits());

        add(litterGrid);
        litterGrid.setSizeFull();
        litterGrid.getStyle().set("flex", "1 1 auto");
        litterGrid.getStyle().set("min-height", "0");

        // litterGrid.createGrid();
        listRefreshNeeded();
    }

    private void updateMobileFlag(int width) {
        boolean isMobileNow = width < MOBILE_BREAKPOINT_PX;
        System.out.println("Window width: " + width + "px. Mobile breakpoint: " + MOBILE_BREAKPOINT_PX + "px. isMobileNow: " + isMobileNow);
        if (this.mobileDevice != isMobileNow) {
            this.mobileDevice = isMobileNow;
            applyDeviceScopedPreferenceKey();
            System.out.println("Mobile device flag updated to: " + mobileDevice + " calling listRefreshNeeded to update grid view style.");
            listRefreshNeeded();
        }
    }

    private void applyDeviceScopedPreferenceKey() {
        litterGrid.setPreferenceScopeKey("litter-list" + (mobileDevice ? ".mobile" : ".desktop"));
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

    @Override
    public void listRefreshNeeded() {
        System.out.println("listRefreshNeeded. MobileDevice: " + mobileDevice );
        // Use mobile as the default only when no saved preference exists.
        litterGrid.setDisplayAsTile(mobileDevice);
        litterGrid.loadDisplayAsTilePreference();
        litterGrid.setLitterIdFilter(queryLitterIdFilter);

        litterGrid.createGrid();
        litterGrid.expandFilteredLitterDetails();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String litterIdValue = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("litterId", java.util.List.of())
                .stream()
                .findFirst()
                .orElse(null);

        queryLitterIdFilter = parseIntegerOrNull(litterIdValue);
        // Rebuild after query-param filtering so deep-link navigation reliably auto-expands/highlights.
        listRefreshNeeded();
    }

    private Integer parseIntegerOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
