package ca.jusjoken.views.utility;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
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

    public PlanListView() {
        setSizeFull();

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
        });

        planGrid.addRefreshListener(this::updateRowCount);

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

        VerticalLayout layout = new VerticalLayout(planGrid);
        add(filterLayout);
        planGrid.setHeightFull();
        layout.setSizeFull();
        layout.getStyle().set("flex", "1 1 auto");
        layout.getStyle().set("min-height", "0");

        add(layout);
        planGrid.refreshGrid();
        updateRowCount();
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
