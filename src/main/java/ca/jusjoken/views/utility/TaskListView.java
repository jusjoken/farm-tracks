package ca.jusjoken.views.utility;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

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
    private boolean mobileDevice = false;
    private static final int MOBILE_BREAKPOINT_PX = 768;
    private Registration resizeRegistration;

    public TaskListView(TaskService taskService) {
        applyDeviceScopedPreferenceKey();
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
            taskGrid.refreshGrid();
        }
    }

    private void applyDeviceScopedPreferenceKey() {
        taskGrid.setPreferenceScopeKey("task-list" + (mobileDevice ? ".mobile" : ".desktop"));
    }

    @Override
    public void listRefreshNeeded() {
        taskGrid.refreshGrid();
    }

}
