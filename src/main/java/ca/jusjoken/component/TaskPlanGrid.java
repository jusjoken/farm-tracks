package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.ListDataProvider;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.TaskPlanService;
import ca.jusjoken.data.service.TaskService;

public class TaskPlanGrid extends Grid<TaskPlan> {

    private enum StatusFilter {
        ACTIVE("Active", Utility.TaskPlanStatus.ACTIVE),
        INACTIVE("Inactive", Utility.TaskPlanStatus.INACTIVE),
        INCOMPLETE("Incomplete", Utility.TaskPlanStatus.INCOMPLETE),
        ALL("All", null);

        final String label;
        final Utility.TaskPlanStatus status;

        StatusFilter(String label, Utility.TaskPlanStatus status) {
            this.label = label;
            this.status = status;
        }
    }

    private final TaskPlanService taskPlanService;
    private final TaskService taskService;
    private final PlanEditor planEditor;

    private ListDataProvider<TaskPlan> dataProvider;
    private StatusFilter currentStatusFilter = StatusFilter.ACTIVE;

    public TaskPlanGrid() {
        super(TaskPlan.class, false);
        this.taskPlanService = Registry.getBean(TaskPlanService.class);
        this.taskService = Registry.getBean(TaskService.class);
        this.planEditor = new PlanEditor();
        this.planEditor.addListener(this::refreshGrid);

        taskPlanService.reconcileStatusesForAllPlans();

        configureGrid();
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();

        addComponentColumn(this::buildPlanBadge).setHeader("Plan").setAutoWidth(true).setFlexGrow(1);
        addColumn(plan -> plan.getType() != null ? plan.getType().getShortName() : "").setHeader("Type").setAutoWidth(true);
        addComponentColumn(this::buildStatusBadge).setHeader("Status").setAutoWidth(true);
        addColumn(plan -> taskService.findByPlanId(plan.getId()).size()).setHeader("Tasks").setAutoWidth(true);
        addColumn(plan -> taskService.findByPlanId(plan.getId()).stream().filter(task -> !Boolean.TRUE.equals(task.getCompleted())).count())
                .setHeader("Remaining")
                .setAutoWidth(true);

        setDataProviderForFilter();
        createContextMenu(this);
    }

    private Component buildPlanBadge(TaskPlan plan) {
        Badge badge = taskPlanService.getDisplayNameBadge(plan);
        return badge != null ? badge : new Span("");
    }

    private Component buildStatusBadge(TaskPlan plan) {
        Utility.TaskPlanStatus status = plan.getStatus() != null ? plan.getStatus() : Utility.TaskPlanStatus.ACTIVE;
        Badge badge = new Badge(status.getShortName());
        badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.PILL);
        switch (status) {
            case ACTIVE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.SUCCESS);
            case INACTIVE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.CONTRAST);
            case INCOMPLETE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.WARNING);
            default -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.PRIMARY);
        }
        return badge;
    }

    private GridContextMenu<TaskPlan> createContextMenu(Grid<TaskPlan> grid) {
        GridContextMenu<TaskPlan> menu = new GridContextMenu<>(grid);
        menu.setDynamicContentHandler(planEntity -> {
            if (planEntity == null) {
                return false;
            }

            menu.removeAll();

            GridMenuItem<TaskPlan> filterMenu = menu.addItem(new Item("Filter by Status", Utility.ICONS.ACTION_FILTER.getIconSource()));
            GridSubMenu<TaskPlan> filterSubMenu = filterMenu.getSubMenu();
            for (StatusFilter filter : StatusFilter.values()) {
                GridMenuItem<TaskPlan> filterItem = filterSubMenu.addItem(filter.label);
                filterItem.setCheckable(true);
                filterItem.setChecked(currentStatusFilter == filter);
                filterItem.addMenuItemClickListener(click -> {
                    currentStatusFilter = filter;
                    refreshGrid();
                });
            }

            menu.addSeparator();
            menu.addComponentAsFirst(UIUtilities.getContextMenuHeader("Plan: " + taskPlanService.getDisplayName(planEntity)));

            GridMenuItem<TaskPlan> viewPlanMenu = menu.addItem(new Item("View Plan", Utility.ICONS.ACTION_VIEW.getIconSource()));
            viewPlanMenu.addMenuItemClickListener(click ->
                    planEditor.dialogOpen(planEntity.getId(), planEntity.getType(), null, PlanEditor.DialogMode.DISPLAY));

            if (planEntity.getStatus() == Utility.TaskPlanStatus.INCOMPLETE) {
                GridMenuItem<TaskPlan> markActiveMenu = menu.addItem(new Item("Mark Plan Active", Utility.ICONS.ACTION_CHECK.getIconSource()));
                markActiveMenu.addMenuItemClickListener(click -> {
                    taskPlanService.markActive(planEntity.getId());
                    refreshGrid();
                });
            } else {
                GridMenuItem<TaskPlan> markIncompleteMenu = menu.addItem(new Item("Mark Plan Incomplete", Utility.ICONS.ACTION_CHECK.getIconSource()));
                markIncompleteMenu.addMenuItemClickListener(click -> {
                    taskPlanService.markIncomplete(planEntity.getId());
                    refreshGrid();
                });
            }

            return true;
        });

        return menu;
    }

    private void setDataProviderForFilter() {
        List<TaskPlan> plans = new ArrayList<>();
        if (currentStatusFilter.status == null) {
            plans.addAll(taskPlanService.findAll());
        } else {
            plans.addAll(taskPlanService.findByStatus(currentStatusFilter.status));
        }

        dataProvider = new ListDataProvider<>(plans);
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        setDataProviderForFilter();
    }
}
