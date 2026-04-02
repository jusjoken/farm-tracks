package ca.jusjoken.views.utility;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.component.TaskPlanGrid;
import ca.jusjoken.data.Utility;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "plans", layout = MainLayout.class)
@PermitAll
@PageTitle("Plan List")
public class PlanListView extends Main {

    private final TaskPlanGrid planGrid = new TaskPlanGrid();
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;
    private Registration resizeRegistration;

    private final Tab allTypesTab = new Tab("All Types");
    private final Tab generalTab = new Tab("General");
    private final Tab breederTab = new Tab("Breeder");
    private final Tab litterTab = new Tab("Litter");
    private final Tabs typeTabs = new Tabs(allTypesTab, generalTab, breederTab, litterTab);
    private final Span rowCount = new Span();
    private final HorizontalLayout filterStatusBar = new HorizontalLayout();
    private final Icon filterStatusIcon = new Icon(Utility.ICONS.ACTION_FILTER.getIconSource());
    private final HorizontalLayout filterChipLayout = new HorizontalLayout();
    private final Button clearAllFiltersButton = new Button("Clear all");

    public PlanListView() {
        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("overflow", "hidden");
        getStyle().set("min-height", "0");

        applyDeviceScopedPreferenceKey();

        typeTabs.setWidthFull();
        typeTabs.setSelectedTab(allTypesTab);
        typeTabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            if (selected == breederTab) {
                planGrid.setTypeFilter(Utility.TaskLinkType.BREEDER);
            } else if (selected == litterTab) {
                planGrid.setTypeFilter(Utility.TaskLinkType.LITTER);
            } else if (selected == generalTab) {
                planGrid.setTypeFilter(Utility.TaskLinkType.GENERAL);
            } else {
                planGrid.setTypeFilter(null);
            }
            updateRowCount();
            refreshFilterStatusBar();
        });

        planGrid.addRefreshListener(() -> {
            updateRowCount();
            refreshFilterStatusBar();
        });

        HorizontalLayout filterLayout = new HorizontalLayout(typeTabs);
        filterLayout.setPadding(true);
        filterLayout.setSpacing(true);
        filterLayout.setWidthFull();
        filterLayout.getStyle().set("flex", "0 0 auto");
        filterLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        filterLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        rowCount.getStyle().set("font-weight", "600");
        rowCount.getStyle().set("white-space", "nowrap");
        filterLayout.add(rowCount);

        configureFilterStatusBar();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().set("flex", "1 1 auto");
        layout.getStyle().set("min-height", "0");
        layout.getStyle().set("overflow", "hidden");

        add(filterLayout);
        add(filterStatusBar);
        planGrid.setWidthFull();
        planGrid.getStyle().set("flex", "1 1 auto");
        planGrid.getStyle().set("min-height", "0");
        layout.add(planGrid);
        layout.setFlexGrow(1, planGrid);

        add(layout);
        planGrid.refreshGrid();
        updateRowCount();
        refreshFilterStatusBar();
    }

    private void configureFilterStatusBar() {
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
    }

    private void refreshFilterStatusBar() {
        int activeCount = planGrid.getActiveFilterCount();
        boolean hasFilters = activeCount > 0;

        filterStatusBar.setVisible(hasFilters);
        clearAllFiltersButton.setVisible(activeCount > 1);
        if (!hasFilters) {
            return;
        }

        filterChipLayout.removeAll();
        for (TaskPlanGrid.FilterChip chip : planGrid.getActiveFilterChips()) {
            filterChipLayout.add(createRemovableChip(chip));
        }
    }

    private HorizontalLayout createRemovableChip(TaskPlanGrid.FilterChip chip) {
        Span label = new Span(chip.getLabel());
        label.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        label.getStyle().set("color", "var(--lumo-secondary-text-color)");
        label.getStyle().set("white-space", "nowrap");

        Button remove = new Button(com.vaadin.flow.component.icon.VaadinIcon.CLOSE_SMALL.create());
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

    private void clearAllFilters() {
        planGrid.clearAllUserFilters();
        refreshFilterStatusBar();
    }

    private void removeFilterChip(TaskPlanGrid.FilterChip chip) {
        if (chip == null) {
            return;
        }
        planGrid.clearFilter(chip.getType());
        refreshFilterStatusBar();
    }

    private void openGridHeaderMenu() {
        planGrid.getElement().executeJs(
                "const grid=this;"
                        + "const rect=grid.getBoundingClientRect();"
                        + "grid.dispatchEvent(new MouseEvent('contextmenu', {"
                        + "bubbles:true,cancelable:true,composed:true,view:window,"
                        + "clientX:rect.left + Math.min(120, Math.max(16, rect.width * 0.2)),clientY:rect.top + 18"
                        + "}));");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().getPage()
                .executeJs("return window.innerWidth;")
                .then(Integer.class, this::updateMobileFlag);

        resizeRegistration = attachEvent.getUI().getPage()
                .addBrowserWindowResizeListener(event -> updateMobileFlag(event.getWidth()));
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
        if (mobileDevice != isMobileNow) {
            mobileDevice = isMobileNow;
            applyDeviceScopedPreferenceKey();
            planGrid.refreshGrid();
        }
    }

    private void applyDeviceScopedPreferenceKey() {
        planGrid.setPreferenceScopeKey("plan-list" + (mobileDevice ? ".mobile" : ".desktop"));
    }

    private void updateRowCount() {
        int count = planGrid.getDisplayedCount();
        rowCount.setText(count == 1 ? "1 plan" : count + " plans");
    }
}
