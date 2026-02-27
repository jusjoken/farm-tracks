package ca.jusjoken.component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.TaskService;

public class TaskGrid extends Grid<Task> {
    private ListDataProvider<Task> dataProvider;
    private final TaskService taskService;
    private final StockService stockService;
    private final TaskEditor taskEditor = new TaskEditor();
    private final PlanEditor planEditor = new PlanEditor();
    private final Button addTaskButton = new Button("Add Task");
    private Integer stockId;
    private String currentCompletionFilter = Utility.TaskCompletionFilter.ACTIVE.filterName;

    public TaskGrid() {
        this(null);
    }

    public TaskGrid(Integer stockId) {
        super(Task.class, false);
        this.stockId = stockId;
        this.taskService = Registry.getBean(TaskService.class);
        this.stockService = Registry.getBean(StockService.class);
        //setSizeFull();
        //setHeight("300px");
        taskEditor.addListener(this::refreshGrid);
        planEditor.addListener(this::refreshGrid);
        configureGrid(stockId);
    }

    private void configureGrid(Integer stockId) {

        addTaskButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addTaskButton.addClickListener(e -> {
            if(stockId != null){
                Stock stock = stockService.findById(stockId);
                Task newTask = new Task();
                newTask.setLinkType(Utility.TaskLinkType.BREEDER);
                newTask.setLinkBreederId(stockId);
                taskEditor.dialogOpen(newTask, TaskEditor.DialogMode.CREATE, stock.getStockType());
            } else {
                taskEditor.dialogOpen();
            }
        });

        setDataProvider();

        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        this.<Icon>addComponentColumn((task) -> {
            Icon editIcon = new Icon("lumo", "edit");
            editIcon.addClickListener(e -> {
                taskEditor.dialogOpen(task, TaskEditor.DialogMode.EDIT, null);
            });
            return editIcon;
        }).setWidth("50px").setFlexGrow(0).setFrozen(true);

        this.addColumn(Task::getName).setHeader("Name").setSortable(true);
        this.addColumn(task -> { return task.getLinkType().getShortName(); }).setHeader("Type").setSortable(true);

        //add a column to show either the linked breeder or the linked litter based on the link type, if the link type is breeder show the breeder name, if the link type is litter show the litter name
        this.addColumn(task -> {
            if(null == task.getLinkType()){
                return "";
            } else return switch (task.getLinkType()) {
                case BREEDER -> stockService.findById(task.getLinkBreederId()).getName();
                case LITTER -> stockService.findById(task.getLinkLitterId()).getName();
                default -> "";
            };
        }).setHeader("Task for:").setSortable(true);

        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        Grid.Column<Task> dateColumn = this.addComponentColumn(task -> {
            LocalDate date = task.getDate();
            Span dateText = new Span(date != null ? date.format(dateFormatter) : "");

            //for completed tasks, show the date in the default text color with a strikethrough. For active tasks, show overdue dates in red and upcoming dates in green.
            if (date != null && Boolean.TRUE.equals(task.getCompleted())) {
                dateText.getStyle().set("text-decoration", "line-through");
            }
            if (date != null && !Boolean.TRUE.equals(task.getCompleted())) {
                if (date.isBefore(LocalDate.now())) {
                    dateText.getStyle().set("color", "var(--lumo-error-text-color)");
                } else if (date.isAfter(LocalDate.now())) {
                    dateText.getStyle().set("color", "var(--lumo-success-text-color)");
                }
                // today => default color
            }
            // completed => default color

            return dateText;
        }).setHeader("Date").setSortable(true).setComparator(Task::getDate);

        Select<String> completionFilter = new Select<>();
        completionFilter.setItems(Utility.TaskCompletionFilter.ALL.filterName, Utility.TaskCompletionFilter.COMPLETED.filterName, Utility.TaskCompletionFilter.ACTIVE.filterName);
        completionFilter.setWidthFull();
        completionFilter.setTooltipText("Filter by completion status");

        completionFilter.addValueChangeListener(event -> {
            currentCompletionFilter = event.getValue();
            updateCompletionFilter(event.getValue());
        });

        this.addComponentColumn(item -> {
            if(item.getCompleted()){
                Icon completedIcon = new Icon(FontAwesome.Regular.SQUARE_CHECK.create().getIcon());
                completedIcon.setColor("green");
                completedIcon.setTooltipText("Completed");
                HorizontalLayout statusLayout = new HorizontalLayout(completedIcon, new Span("Completed"));
                return statusLayout;
            }else{
                Icon incompleteIcon = new Icon(FontAwesome.Regular.SQUARE.create().getIcon());
                incompleteIcon.setTooltipText("Active");
                HorizontalLayout statusLayout = new HorizontalLayout(incompleteIcon, new Span("Active"));
                return statusLayout;
            }
        }).setHeader(completionFilter);

        this.addComponentColumn(task -> {
            Button editPlan = new Button("Edit Plan");
            editPlan.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

            if (task.getTaskPlan() != null) {
                editPlan.addClickListener(e -> {
                    Stock stockEntity = null;
                    planEditor.dialogOpen(task.getTaskPlan().getId(), task.getLinkType(), stockEntity, PlanEditor.DialogMode.EDIT);
                });
            } else {
                editPlan.setEnabled(false);
                editPlan.getStyle().set("visibility", "hidden"); // keeps same row height
            }

            return editPlan;
        }).setHeader(addTaskButton);
        setMultiSort(true);
        this.sort(List.of(new GridSortOrder<>(dateColumn, SortDirection.ASCENDING)));
        // set UI value + apply filter explicitly on first display
        completionFilter.setValue(currentCompletionFilter);
        updateCompletionFilter(currentCompletionFilter);

    }

    private void updateCompletionFilter(String filterValue) {
        System.out.println("Updating completion filter to: " + filterValue);
        dataProvider.clearFilters();
        if (Utility.TaskCompletionFilter.COMPLETED.filterName.equals(filterValue)) {
            dataProvider.addFilter(Task::getCompleted);
        } else if (Utility.TaskCompletionFilter.ACTIVE.filterName.equals(filterValue)) {
            dataProvider.addFilter(task -> !task.getCompleted());
        }
    }

    private void setDataProvider() {
        if(stockId != null){
            dataProvider = new ListDataProvider<>(taskService.findByStockId(stockId));
        } else {
            dataProvider = new ListDataProvider<>(taskService.findAll());
        }
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        setDataProvider();
        // re-apply filter after data provider is recreated
        updateCompletionFilter(currentCompletionFilter);
        dataProvider.refreshAll();
    }

}
