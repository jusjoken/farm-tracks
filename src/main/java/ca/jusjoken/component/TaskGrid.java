package ca.jusjoken.component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.TaskPlanService;
import ca.jusjoken.data.service.TaskService;
import ca.jusjoken.data.service.UserUiSettingsService;
import ca.jusjoken.utility.TaskType;

public class TaskGrid extends Grid<Task> {
    private ListDataProvider<Task> dataProvider;
    private final TaskService taskService;
    private final StockService stockService;
    private final TaskPlanService taskPlanService;
    private final UserUiSettingsService userUiSettingsService;
    private final TaskEditor taskEditor = new TaskEditor();
    private final LitterEditor litterEditor = new LitterEditor();
    private PlanEditor planEditor; // lazy to avoid constructor recursion
    private final boolean enablePlanActions;
    private Integer stockId;
    private Integer taskPlanId;
    private String currentCompletionFilter = Utility.TaskCompletionFilter.ACTIVE.filterName;
    Grid.Column<Task> dateColumn;
    private Registration sortListenerRegistration;
    private Boolean menuCreated = false;
    private Boolean minimalColumns = false;
    private Boolean displayAsTile = false;
    private String preferenceScopeKey;

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
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
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

        this.addComponentColumn(Task::getHeader).setHeader("Name").setSortable(true).setFrozen(true).setKey("name");
        if(!minimalColumns) {
            this.addColumn(task -> { return task.getLinkType().getShortName(); }).setHeader("Type").setSortable(true).setKey("type");
            //add a column to show either the linked breeder or the linked litter based on the link type, if the link type is breeder show the breeder name, if the link type is litter show the litter name
            this.addColumn(this::getTaskFor).setHeader("Task for:").setSortable(true).setKey("taskfor");
        }

        this.addComponentColumn(Task::getStatusBadge).setHeader("Status").setSortable(true).setComparator(Task::getCompleted).setKey("status");

        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        dateColumn = this.addComponentColumn(task -> {
            return task.getDueDateBadge();
        }).setHeader("Date").setSortable(true).setComparator(Task::getDate).setKey("date");

        if(!minimalColumns){
            this.addComponentColumn(item -> {
                return getPlanNameComponent(item);
            }).setHeader("Plan");       
        }

        setMultiSort(true);
        if (sortListenerRegistration != null) sortListenerRegistration.remove();
        sortListenerRegistration = addSortListener(e -> saveSortPreference(e.getSortOrder()));
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

        card.addToFooter(taskEntity.getStatusBadge());
        card.addToFooter(taskEntity.getDueDateBadge());
        
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
                    saveDisplayAsTilePreference();
                    configureGrid();
                    refreshGrid();
                });
                menu.addSeparator();

                //add a submenu to filter by completion status
                GridMenuItem<Task> filterByCompletionMenu = menu.addItem(new Item("Filter by Completion", Utility.ICONS.ACTION_FILTER.getIconSource()));
                GridSubMenu<Task> filterSubMenu = filterByCompletionMenu.getSubMenu();
                //for each taskcompletionfilter option, add a menu item to the submenu and set it to checked if it is the current filter
                for (Utility.TaskCompletionFilter filter : Utility.TaskCompletionFilter.values()) {
                    GridMenuItem<Task> menuItem = filterSubMenu.addItem(filter.filterName);
                    menuItem.setCheckable(true);
                    menuItem.setChecked(currentCompletionFilter.equals(filter.filterName));
                    menuItem.addMenuItemClickListener(click -> {
                        currentCompletionFilter = filter.filterName;
                        updateCompletionFilter(currentCompletionFilter);
                        updateSortOrder();
                    });
                }

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
        if (getColumns().isEmpty()) {
            return;
        }
        List<GridSortOrder<Task>> saved = loadSortPreference();
        if (saved != null && !saved.isEmpty()) {
            this.sort(saved);
        } else if (dateColumn != null) {
            this.sort(List.of(new GridSortOrder<>(dateColumn, SortDirection.ASCENDING)));
        }
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

    public void setPreferenceScopeKey(String preferenceScopeKey) {
        this.preferenceScopeKey = preferenceScopeKey;
        loadDisplayAsTilePreference();
        configureGrid();
    }

    public String getPreferenceScopeKey() {
        return preferenceScopeKey;
    }

    public void loadDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }
        displayAsTile = userUiSettingsService.getBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(displayAsTile));
    }

    public void applyDisplayAsTilePreference() {
        loadDisplayAsTilePreference();
        configureGrid();
    }

    public Boolean getDisplayAsTile() {
        return displayAsTile;
    }

    public void setDisplayAsTile(Boolean displayAsTile) {
        this.displayAsTile = Boolean.TRUE.equals(displayAsTile);
    }

    private void saveDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }
        userUiSettingsService.setBooleanForCurrentUser(settingsKey, Boolean.TRUE.equals(displayAsTile));
    }

    private String getDisplayAsTilePreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".displayAsTile";
    }

    private void saveSortPreference(List<GridSortOrder<Task>> sortOrders) {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) return;
        if (sortOrders == null || sortOrders.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (GridSortOrder<Task> order : sortOrders) {
            String key = order.getSorted().getKey();
            if (key == null) continue;
            if (sb.length() > 0) sb.append(",");
            sb.append(key).append(":").append(order.getDirection().name());
        }
        if (sb.isEmpty()) return;
        userUiSettingsService.setValueForCurrentUser(settingsKey, sb.toString());
    }

    private List<GridSortOrder<Task>> loadSortPreference() {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) return null;
        Optional<Object> value = userUiSettingsService.getValueForCurrentUser(settingsKey);
        if (value.isEmpty()) return null;
        String serialized = String.valueOf(value.get());
        if (serialized.isBlank()) return null;
        List<GridSortOrder<Task>> result = new ArrayList<>();
        for (String part : serialized.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            Grid.Column<Task> col = getColumnByKey(kv[0]);
            if (col == null) continue;
            try {
                SortDirection dir = SortDirection.valueOf(kv[1]);
                result.add(new GridSortOrder<>(col, dir));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved sort directions.
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String getSortPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) return null;
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".sortOrder";
    }

}
