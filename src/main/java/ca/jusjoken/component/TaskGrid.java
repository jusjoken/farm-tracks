package ca.jusjoken.component;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.TaskPlanService;
import ca.jusjoken.data.service.TaskService;
import ca.jusjoken.utility.TaskType;

public class TaskGrid extends Grid<Task> {
    private ListDataProvider<Task> dataProvider;
    private final TaskService taskService;
    private final StockService stockService;
    private final TaskPlanService taskPlanService;
    private final TaskEditor taskEditor = new TaskEditor();
    private final LitterEditor litterEditor = new LitterEditor();
    private PlanEditor planEditor; // lazy to avoid constructor recursion
    private final boolean enablePlanActions;
    private Integer stockId;
    private Integer taskPlanId;
    private String currentCompletionFilter = Utility.TaskCompletionFilter.ACTIVE.filterName;
    Grid.Column<Task> dateColumn;
    private Boolean menuCreated = false;
    private Boolean minimalColumns = false;
    private Boolean displayAsTile = false;

    public TaskGrid() {
        this(null, true, false);
    }

    public TaskGrid(Integer stockId) {
        this(stockId, true, false);
    }

    public TaskGrid(Integer stockId, boolean enablePlanActions, boolean minimalColumns) {
        super(Task.class, false);
        this.taskService = Registry.getBean(TaskService.class);
        this.stockService = Registry.getBean(StockService.class);
        this.taskPlanService = Registry.getBean(TaskPlanService.class);
        this.enablePlanActions = enablePlanActions;
        this.minimalColumns = minimalColumns;

        taskEditor.addListener(this::refreshGrid);
        if (enablePlanActions) {
            getPlanEditor().addListener(this::refreshGrid);
        }

        this.stockId = stockId;
        if(minimalColumns){
            currentCompletionFilter = Utility.TaskCompletionFilter.ALL.filterName;
        }
        configureGrid();
    }

    private PlanEditor getPlanEditor() {
        if (planEditor == null) {
            planEditor = new PlanEditor();
        }
        return planEditor;
    }

    private final ComponentRenderer<Component, Task> taskCardRenderer = new ComponentRenderer<>(
            taskEntity -> {
                // return createListItemLayout(taskEntity);
                return createListItemCard(taskEntity);
    });    

    private String getTaskFor(Task task) {
        if (task.getLinkType() == null) {
            return "";
        }
        return switch (task.getLinkType()) {
            case BREEDER -> stockService.findById(task.getLinkBreederId()).getName();
            case LITTER -> stockService.findById(task.getLinkLitterId()).getName();
            default -> "";
        };
    }


    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();

        setDataProvider();
        
        if(displayAsTile){
            configureTileView();
        }else{
            configureListView();
        }

        setEmptyStateText("No litters available to display");



        // System.out.println("Configuring TaskGrid with stockId: " + stockId);

        if(!menuCreated){
            createContextMenu(this);
            menuCreated = true;
        }

    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        this.addColumn(Task::getName).setHeader("Name").setSortable(true).setFrozen(true);
        if(!minimalColumns) {
            this.addColumn(task -> { return task.getLinkType().getShortName(); }).setHeader("Type").setSortable(true);
            //add a column to show either the linked breeder or the linked litter based on the link type, if the link type is breeder show the breeder name, if the link type is litter show the litter name
            this.addColumn(this::getTaskFor).setHeader("Task for:").setSortable(true);
        }

        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        dateColumn = this.addComponentColumn(task -> {
            return task.getDueDateBadge();
        }).setHeader("Date").setSortable(true).setComparator(Task::getDate);

        if(!minimalColumns){
            this.addComponentColumn(item -> {
                return getPlanNameComponent(item);
            }).setHeader("Plan");       
        }

        Select<String> completionFilter = new Select<>();
        completionFilter.setItems(Utility.TaskCompletionFilter.ALL.filterName, Utility.TaskCompletionFilter.COMPLETED.filterName, Utility.TaskCompletionFilter.ACTIVE.filterName);
        completionFilter.setWidthFull();
        completionFilter.setTooltipText("Filter by completion status");

        completionFilter.addValueChangeListener(event -> {
            currentCompletionFilter = event.getValue();
            updateCompletionFilter(event.getValue());
        });

        this.addComponentColumn(Task::getStatusBadge).setHeader(completionFilter);

        setMultiSort(true);
        completionFilter.setValue(currentCompletionFilter);
        updateCompletionFilter(currentCompletionFilter);
        updateSortOrder();
    }

    private void configureTileView() {
        addColumn(taskCardRenderer).setKey("name");
    }

    private Component getPlanNameComponent(Task task) {
        Badge planName = safeGetPlanName(task);
        if (planName != null) {
            return planName;
            
        }
        return new Span("");
    }

    private Component createListItemCard(Task taskEntity) {
        Card card = new Card();
        card.setWidthFull();
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeader(taskEntity.getHeader());
        card.setHeaderSuffix(new Span(this.getTaskFor(taskEntity)));

        card.addToFooter(taskEntity.getDueDateBadge());
        card.addToFooter(taskEntity.getStatusBadge());
        
        card.addToFooter(getPlanNameComponent(taskEntity));
        return card;
    }

    private GridContextMenu<Task> createContextMenu(Grid<Task> grid) {
        GridContextMenu<Task> menu = new GridContextMenu<>(grid);
        menu.setDynamicContentHandler(menuEntity -> {
            if (menuEntity == null) {
                return false;
            } else {
                menu.removeAll();
                GridMenuItem<Task> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
                displayAsTileMenu.setCheckable(true);
                displayAsTileMenu.setChecked(displayAsTile);
                displayAsTileMenu.addMenuItemClickListener(click -> {
                    displayAsTile = displayAsTileMenu.isChecked();
                    configureGrid();
                    refreshGrid();
                });
                menu.addSeparator();


                menu.addComponentAsFirst(UIUtilities.getContextMenuHeader("Task: " + menuEntity.getName()));

                GridMenuItem<Task> addNewMenu = menu.addItem(new Item("Add Task", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addNewMenu.addMenuItemClickListener(click -> {
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
                menu.addSeparator();
                //create a submenu that allows selection of the view style
                


                // Context menu opened on a task, show option to edit
                GridMenuItem<Task> editMenu = menu.addItem(new Item("Edit Task", Utility.ICONS.ACTION_EDIT.getIconSource()));
                editMenu.addMenuItemClickListener(click -> {
                    taskEditor.dialogOpen(menuEntity, TaskEditor.DialogMode.EDIT, null);
                });
                //add a menu item to view the plan if the task has a plan
                Integer taskPlanId = safeGetTaskPlanId(menuEntity);
                if (enablePlanActions && taskPlanId != null) {
                    GridMenuItem<Task> editPlanMenu = menu.addItem(new Item("View Plan", Utility.ICONS.ACTION_EDIT.getIconSource()));
                    editPlanMenu.addMenuItemClickListener(click -> {
                        Stock stockEntity = null;
                        getPlanEditor().dialogOpen(taskPlanId, menuEntity.getLinkType(), stockEntity, PlanEditor.DialogMode.DISPLAY);
                    });
                }
                //if the current task type has an action associated with it, show the action in the context menu. For example, if the task type is BIRTH then show a "View Offspring" action that opens the stock list view filtered to show the offspring from this birth task.
                if (menuEntity.getType() != null && menuEntity.getType().hasAction()) {
                    if (menuEntity.getType() == TaskType.BIRTH) {
                        GridMenuItem<Task> actionMenu = menu.addItem(new Item(menuEntity.getType().getAction(), Utility.ICONS.ACTION_BIRTH.getIconSource()));
                        actionMenu.addMenuItemClickListener(click -> {
                            litterEditor.runTaskAction(menuEntity, null);
                        });

                    }
                } else {
                    //if no specific action then add a make complete action for active tasks and a mark incomplete action for completed tasks
                    String actionText = menuEntity.getCompleted() ? "Mark Incomplete" : "Mark Complete";
                    GridMenuItem<Task> actionMenu = menu.addItem(new Item(actionText, Utility.ICONS.ACTION_CHECK.getIconSource()));
                    actionMenu.addMenuItemClickListener(click -> {
                        menuEntity.setCompleted(!menuEntity.getCompleted());
                        taskService.save(menuEntity);
                        refreshGrid();
                    });
                }
                return true;
            }
        });

        return menu;
    }

    private Badge safeGetPlanName(Task task) {
        Integer taskPlanId = safeGetTaskPlanId(task);
        if (taskPlanId == null) {
            return null;
        }
        try {
            TaskPlan plan = taskPlanService.findById(taskPlanId).get();
            if (plan == null) {
                return null;
            }
            return (taskPlanService.getDisplayNameBadge(plan) == null) ? null : taskPlanService.getDisplayNameBadge(plan);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer safeGetTaskPlanId(Task task) {
        if (task == null) {
            return null;
        }
        try {
            if (task.getTaskPlan() != null) {
                return task.getTaskPlan().getId(); // proxy id access
            }
        } catch (Exception ignored) {
            // fallback below
        }

        try {
            var method = task.getClass().getMethod("getTaskPlanId");
            Object value = method.invoke(task);
            if (value instanceof Integer id) {
                return id;
            }
        } catch (Exception ignored) {
            // no fallback left
        }

        return null;
    }

    public void setStockId(Integer stockId) {
        this.stockId = stockId;
        this.taskPlanId = null;
        refreshGrid();
    }    

    public void setTaskPlanId(Integer id) {
        this.taskPlanId = id;
        this.stockId = null;
        refreshGrid();
    }

    private void updateCompletionFilter(String filterValue) {
        // System.out.println("Updating completion filter to: " + filterValue);
        dataProvider.clearFilters();
        if (Utility.TaskCompletionFilter.COMPLETED.filterName.equals(filterValue)) {
            dataProvider.addFilter(Task::getCompleted);
        } else if (Utility.TaskCompletionFilter.ACTIVE.filterName.equals(filterValue)) {
            dataProvider.addFilter(task -> !task.getCompleted());
        }
    }

    private void updateSortOrder(){
            //System.out.println("Re-applying sort order after data provider refresh");
            this.sort(List.of(new GridSortOrder<>(dateColumn, SortDirection.ASCENDING)));
    }

    private void setDataProvider() {
        // System.out.println("Setting data provider for TaskGrid with stockId: " + stockId);
        if(stockId != null){
            dataProvider = new ListDataProvider<>(taskService.findByStockId(stockId));
        } else if (taskPlanId != null) {
            dataProvider = new ListDataProvider<>(taskService.findByPlanId(taskPlanId));
        } else {
            dataProvider = new ListDataProvider<>(taskService.findAll());
        }
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        // System.out.println("Refreshing TaskGrid with stockId: " + stockId);
        setDataProvider();
        // re-apply filter after data provider is recreated
        updateCompletionFilter(currentCompletionFilter);
        updateSortOrder();
    }

    public Boolean getMinimalColumns() {
        return minimalColumns;
    }

    public Boolean getDisplayAsTile() {
        return displayAsTile;
    }

    public void setDisplayAsTile(Boolean displayAsTile) {
        this.displayAsTile = displayAsTile;
    }

}
