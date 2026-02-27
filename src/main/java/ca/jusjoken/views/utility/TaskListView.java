package ca.jusjoken.views.utility;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.TaskGrid;
import ca.jusjoken.data.service.TaskService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;


@Route(value = "tasks", layout = MainLayout.class)
@PermitAll
@PageTitle("Task List")
public class TaskListView extends Main implements ListRefreshNeededListener {

    private final TaskGrid taskGrid = new TaskGrid();

    public TaskListView(TaskService taskService) {
        configureActions();

        setSizeFull();
        VerticalLayout layout = new VerticalLayout(taskGrid);
        taskGrid.setHeightFull();
        layout.setSizeFull();
        add(layout);

        taskGrid.refreshGrid();
    }

    private void configureActions() {
        //taskEditor.addListener(this::refreshGrid);
    }

    @Override
    public void listRefreshNeeded() {
        taskGrid.refreshGrid();
    }

}
