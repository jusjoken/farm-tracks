package ca.jusjoken.views.utility;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.LitterGrid;
import ca.jusjoken.data.Utility;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "litters", layout = MainLayout.class)
@PermitAll
@PageTitle("Litter List")
public class LitterListView extends Main implements ListRefreshNeededListener, BeforeEnterObserver{
    private final LitterGrid litterGrid = new LitterGrid();
    private final TextField parentFilter = new TextField();
    private final HorizontalLayout filterStatusBar = new HorizontalLayout();
    private final Icon filterStatusIcon = new Icon(Utility.ICONS.ACTION_FILTER.getIconSource());
    private final HorizontalLayout filterChipLayout = new HorizontalLayout();
    private final Button clearAllFiltersButton = new Button("Clear all");
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;   
    private Registration resizeRegistration;
    private boolean gridListenerRegistered;
    private Integer queryLitterIdFilter;

    public LitterListView() {
        applyDeviceScopedPreferenceKey();

        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("overflow", "hidden");
        getStyle().set("min-height", "0");

        //add a filter that filters the littergrid by the name of the father or mother
        parentFilter.setLabel("Parent Name Filter");
        parentFilter.setClearButtonVisible(true);
        parentFilter.setValueChangeMode(ValueChangeMode.EAGER);
        parentFilter.addValueChangeListener(e -> {
           litterGrid.setParentNameFilter(e.getValue());
              refreshFilterStatusBar();
        });

        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setPadding(true);
        filterLayout.setSpacing(true);
        filterLayout.setWidthFull();
        filterLayout.getStyle().set("flex", "0 0 auto");
          filterLayout.add(parentFilter);
        add(filterLayout);

          filterStatusBar.setWidthFull();
          filterStatusBar.setPadding(false);
          filterStatusBar.setSpacing(true);
          filterStatusBar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
          filterStatusBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
          filterStatusBar.getStyle().set("padding", "0 var(--lumo-space-m) var(--lumo-space-xs) var(--lumo-space-m)");

          filterStatusIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
          filterStatusIcon.getStyle().set("cursor", "pointer");
          filterStatusIcon.getElement().setAttribute("aria-label", "Active filters");
          filterStatusIcon.getElement().setAttribute("title", "Edit filters");
          filterStatusIcon.getElement().setAttribute("tabindex", "0");
          filterStatusIcon.getElement().addEventListener("click", e -> openGridHeaderMenu());
          filterStatusIcon.getElement().addEventListener("keydown", e -> openGridHeaderMenu())
              .setFilter("event.key === 'Enter' || event.key === ' '");

          filterChipLayout.setSpacing(true);
          filterChipLayout.setPadding(false);

          clearAllFiltersButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
          clearAllFiltersButton.addClickListener(click -> clearAllFilters());

          HorizontalLayout statusLeft = new HorizontalLayout(filterStatusIcon, filterChipLayout);
          statusLeft.setPadding(false);
          statusLeft.setSpacing(true);
          statusLeft.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

          filterStatusBar.add(statusLeft, clearAllFiltersButton);
          add(filterStatusBar);

        parentFilter.setAutofocus(true);

        add(litterGrid);
        litterGrid.setWidthFull();
        litterGrid.getStyle().set("flex", "1 1 auto");
        litterGrid.getStyle().set("min-height", "0");
    }

    private void updateMobileFlag(int width) {
        boolean isMobileNow = width < MOBILE_BREAKPOINT_PX;
        if (this.mobileDevice != isMobileNow) {
            this.mobileDevice = isMobileNow;
            applyDeviceScopedPreferenceKey();
            listRefreshNeeded();
        }
    }

    private void applyDeviceScopedPreferenceKey() {
        litterGrid.setPreferenceScopeKey("litter-list" + (mobileDevice ? ".mobile" : ".desktop"));
    }

    
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!gridListenerRegistered) {
            litterGrid.addListener(this);
            gridListenerRegistered = true;
        }
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
        if (gridListenerRegistered) {
            litterGrid.removeListener(this);
            gridListenerRegistered = false;
        }
        if (resizeRegistration != null) {
            resizeRegistration.remove();
            resizeRegistration = null;
        }
        super.onDetach(detachEvent);
    }

    @Override
    public void listRefreshNeeded() {
        boolean previousDisplayMode = Boolean.TRUE.equals(litterGrid.getDisplayAsTile());
        // Use mobile as the default only when no saved preference exists.
        litterGrid.setDisplayAsTile(mobileDevice);
        litterGrid.loadDisplayAsTilePreference();
        boolean currentDisplayMode = Boolean.TRUE.equals(litterGrid.getDisplayAsTile());
        litterGrid.setLitterIdFilter(queryLitterIdFilter);

        if (previousDisplayMode != currentDisplayMode) {
            litterGrid.createGrid();
        }

        if (queryLitterIdFilter != null) {
            litterGrid.expandFilteredLitterDetails();
        }
        refreshFilterStatusBar();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String litterIdValue = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("litterId", java.util.List.of())
                .stream()
                .findFirst()
                .orElse(null);

        queryLitterIdFilter = parseIntegerOrNull(litterIdValue);
        // Always refresh to ensure a previously scoped litter filter can be cleared
        // when navigating back to /litters without query parameters.
        listRefreshNeeded();
    }

    private void clearAllFilters() {
        queryLitterIdFilter = null;
        parentFilter.clear();
        litterGrid.clearAllUserFilters();
        refreshFilterStatusBar();
        getUI().ifPresent(ui -> ui.navigate(LitterListView.class));
    }

    private void refreshFilterStatusBar() {
        int activeCount = litterGrid.getActiveFilterCount();
        boolean hasFilters = activeCount > 0;

        filterStatusBar.setVisible(hasFilters);
        clearAllFiltersButton.setVisible(activeCount > 1);
        if (!hasFilters) {
            return;
        }

        filterChipLayout.removeAll();
        for (LitterGrid.FilterChip chip : litterGrid.getActiveFilterChips()) {
            filterChipLayout.add(createRemovableChip(chip));
        }
    }

    private HorizontalLayout createRemovableChip(LitterGrid.FilterChip chip) {
        Span label = new Span(chip.getLabel());
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        label.getStyle().set("color", "var(--lumo-secondary-text-color)");
        label.getStyle().set("white-space", "nowrap");

        Button remove = new Button(VaadinIcon.CLOSE_SMALL.create());
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        remove.getElement().setAttribute("aria-label", "Remove filter " + chip.getLabel());
        remove.addClickListener(click -> removeFilterChip(chip));

        HorizontalLayout chipLayout = new HorizontalLayout(label, remove);
        chipLayout.setPadding(false);
        chipLayout.setSpacing(false);
        chipLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        chipLayout.getStyle().set("padding", "0.1rem 0.2rem 0.1rem 0.45rem");
        chipLayout.getStyle().set("border-radius", "999px");
        chipLayout.getStyle().set("background", "var(--lumo-contrast-10pct)");
        return chipLayout;
    }

    private void openGridHeaderMenu() {
        litterGrid.getElement().executeJs(
                "const grid=this;"
                        + "const rect=grid.getBoundingClientRect();"
                        + "grid.dispatchEvent(new MouseEvent('contextmenu', {"
                        + "bubbles:true,cancelable:true,composed:true,view:window,"
                        + "clientX:rect.left + Math.min(120, Math.max(16, rect.width * 0.2)),clientY:rect.top + 18"
                        + "}));");
    }

    private void removeFilterChip(LitterGrid.FilterChip chip) {
        if (chip == null) {
            return;
        }

        if (chip.getType() == LitterGrid.FilterType.LITTER_ID) {
            queryLitterIdFilter = null;
            litterGrid.clearFilter(chip.getType());
            refreshFilterStatusBar();
            getUI().ifPresent(ui -> ui.navigate(LitterListView.class));
            return;
        }

        if (chip.getType() == LitterGrid.FilterType.PARENT_NAME) {
            parentFilter.clear();
            litterGrid.clearFilter(chip.getType());
            refreshFilterStatusBar();
            return;
        }

        litterGrid.clearFilter(chip.getType());
        refreshFilterStatusBar();
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
