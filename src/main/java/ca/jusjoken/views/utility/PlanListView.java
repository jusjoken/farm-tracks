package ca.jusjoken.views.utility;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.component.TaskPlanGrid;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "plans", layout = MainLayout.class)
@PermitAll
@PageTitle("Plan List")
public class PlanListView extends Main {

    private final TaskPlanGrid planGrid = new TaskPlanGrid();

    public PlanListView() {
        setSizeFull();

        VerticalLayout layout = new VerticalLayout(planGrid);
        planGrid.setHeightFull();
        layout.setSizeFull();

        add(layout);
        planGrid.refreshGrid();
    }
}
