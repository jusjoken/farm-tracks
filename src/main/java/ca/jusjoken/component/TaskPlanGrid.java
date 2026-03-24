package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

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
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.utility.TaskType;

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
    private final StockService stockService;
    private final PlanEditor planEditor;
    private final LitterEditor litterEditor;
    private final UserUiSettingsService userUiSettingsService;
    private final List<Runnable> refreshListeners = new ArrayList<>();

    private ListDataProvider<TaskPlan> dataProvider;
    private StatusFilter currentStatusFilter = StatusFilter.ACTIVE;
    private Utility.TaskLinkType currentTypeFilter;
    private Integer associatedStockId;
    private Registration sortListenerRegistration;
    private Registration selectionListenerRegistration;
    private String preferenceScopeKey;
    private Integer expandedPlanId;
    private Boolean displayAsTile = false;
    private boolean menuCreated = false;
    private static final String ACTION_COLUMN_KEY = "row-actions";

        private final ComponentRenderer<Component, TaskPlan> planCardRenderer = new ComponentRenderer<>(
            this::createPlanCard);

    public TaskPlanGrid() {
        super(TaskPlan.class, false);
        this.taskPlanService = Registry.getBean(TaskPlanService.class);
        this.taskService = Registry.getBean(TaskService.class);
        this.stockService = Registry.getBean(StockService.class);
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
        this.planEditor = new PlanEditor();
        this.litterEditor = new LitterEditor();
        this.planEditor.addListener(this::refreshGrid);
        this.litterEditor.addListener(this::refreshGrid);

        taskPlanService.reconcileStatusesForAllPlans();

        configureGrid();
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();
        removeClassName("mobile-tile-scroll-fix");

        if (displayAsTile) {
            addClassName("mobile-tile-scroll-fix");
            configureTileView();
        } else {
            configureListView();
        }

        setEmptyStateText("No plans available to display");

        setMultiSort(true);
        if (sortListenerRegistration != null) {
            sortListenerRegistration.remove();
        }
        sortListenerRegistration = addSortListener(event -> saveSortPreference(event.getSortOrder()));

        setDataProviderForFilter();
        updateSortOrder();
        if (!displayAsTile) {
            restoreExpandedDetails();
        }
        if (!menuCreated) {
            createContextMenu(this);
            menuCreated = true;
        }
    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);

        addRowActionsColumn();

        addComponentColumn(this::buildPlanHeader)
            .setHeader("Plan")
            .setAutoWidth(true)
            .setFlexGrow(1)
            .setSortable(true)
            .setComparator(this::getPlanDisplayName)
            .setKey("plan");
        addColumn(this::getPlanTypeLabel)
            .setHeader("Type")
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(this::getPlanTypeLabel)
            .setKey("type");
        addColumn(this::getParentsLabel)
            .setHeader("Parents")
            .setAutoWidth(true)
            .setFlexGrow(1)
            .setSortable(true)
            .setComparator(this::getParentsLabel)
            .setKey("parents");
        addComponentColumn(this::buildStatusBadge)
            .setHeader("Status")
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(plan -> plan.getStatus() != null ? plan.getStatus().getShortName() : "")
            .setKey("status");
        addColumn(this::getTaskCount)
            .setHeader("Tasks")
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(this::getTaskCount)
            .setKey("tasks");
        addColumn(this::getRemainingTaskCount)
            .setHeader("Remaining")
            .setAutoWidth(true)
            .setSortable(true)
            .setComparator(this::getRemainingTaskCount)
            .setKey("remaining");

        setSelectionMode(Grid.SelectionMode.SINGLE);
        setItemDetailsRenderer(new ComponentRenderer<>(this::createTaskDetailsLayout));
        if (selectionListenerRegistration != null) {
            selectionListenerRegistration.remove();
        }
        selectionListenerRegistration = asSingleSelect().addValueChangeListener(event -> {
            TaskPlan previousPlan = event.getOldValue();
            if (previousPlan != null) {
                setDetailsVisible(previousPlan, false);
            }

            TaskPlan selectedPlan = event.getValue();
            if (selectedPlan != null && getTaskCount(selectedPlan) > 0) {
                expandedPlanId = selectedPlan.getId();
                setDetailsVisible(selectedPlan, true);
            } else {
                expandedPlanId = null;
            }
        });
    }

    private void configureTileView() {
        addColumn(planCardRenderer).setKey("plan");
        setSelectionMode(Grid.SelectionMode.NONE);
        setItemDetailsRenderer(null);
        if (selectionListenerRegistration != null) {
            selectionListenerRegistration.remove();
            selectionListenerRegistration = null;
        }
        expandedPlanId = null;
    }

        private String getPlanDisplayName(TaskPlan plan) {
        return taskPlanService.getDisplayName(plan);
        }

        private String getPlanTypeLabel(TaskPlan plan) {
        return plan.getType() != null ? plan.getType().getShortName() : "";
        }

        private String getParentsLabel(TaskPlan plan) {
        if (plan == null) {
            return "";
        }

        if (plan.getType() != Utility.TaskLinkType.BREEDER && plan.getType() != Utility.TaskLinkType.LITTER) {
            return "";
        }

        String motherName = normalizeParentName(stockService.getStockNameById(plan.getLinkMotherId()));
        String fatherName = normalizeParentName(stockService.getStockNameById(plan.getLinkFatherId()));
        return motherName + " / " + fatherName;
        }

        private String normalizeParentName(String parentName) {
        if (parentName == null || parentName.isBlank() || "N/A".equalsIgnoreCase(parentName)) {
            return "Unknown";
        }
        return parentName;
        }

        private int getTaskCount(TaskPlan plan) {
        return taskService.findByPlanId(plan.getId()).size();
        }

        private long getRemainingTaskCount(TaskPlan plan) {
        return taskService.findByPlanId(plan.getId()).stream()
            .filter(task -> !Boolean.TRUE.equals(task.getCompleted()))
            .count();
        }

    private Component buildPlanBadge(TaskPlan plan) {
        Badge badge = taskPlanService.getDisplayNameBadge(plan);
        return badge != null ? badge : new Span("");
    }

    private Component buildPlanHeader(TaskPlan plan) {
        return taskPlanService.getHeader(plan);
    }   

    private Component buildStatusBadge(TaskPlan plan) {
        Utility.TaskPlanStatus status = plan.getStatus() != null ? plan.getStatus() : Utility.TaskPlanStatus.ACTIVE;
        Badge badge = new Badge(getStatusBadgeLabel(plan, status));
        badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.PILL);
        switch (status) {
            case ACTIVE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.SUCCESS);
            case INACTIVE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.CONTRAST);
            case INCOMPLETE -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.WARNING);
            default -> badge.addThemeVariants(ca.jusjoken.utility.BadgeVariant.PRIMARY);
        }
        return badge;
    }

    private Component createPlanCard(TaskPlan plan) {

        Card card = new Card();
        card.setWidthFull();
        card.getStyle().set("margin", "var(--lumo-space-xs) 0");
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        // Build header: icon for plan type + display name
        Span headerLayout = new Span();
        headerLayout.getStyle().set("display", "flex");
        headerLayout.getStyle().set("align-items", "center");
        headerLayout.getStyle().set("gap", "0.5em");


        // Display name
        // Component displayNameBadge = buildPlanBadge(plan);
        headerLayout.add(buildPlanHeader(plan));

        card.setHeader(headerLayout);

        card.setHeaderSuffix(createTileHeaderSuffix(getPlanTypeLabel(plan)));

        // Footer chips for visual parity with TaskGrid
        // Status badge first
        card.addToFooter(buildStatusBadge(plan));

        // Parents badge
        String parents = getParentsLabel(plan);
        if (!parents.isBlank()) {
            card.addToFooter(UIUtilities.createBadge(null, parents, BadgeVariant.PRIMARY));
        }

        // Tasks badge
        String tasks = "Tasks: " + getTaskCount(plan);
        card.addToFooter(UIUtilities.createBadge(null, tasks, BadgeVariant.CONTRAST));

        // Remaining badge
        String remaining = "Remaining: " + getRemainingTaskCount(plan);
        card.addToFooter(UIUtilities.createBadge(null, remaining, BadgeVariant.CONTRAST));

        return card;
    }

    private void addRowActionsColumn() {
        addComponentColumn(plan -> createRowMenuButton())
                .setHeader("")
                .setAutoWidth(false)
                .setFlexGrow(0)
                .setWidth("3.25em")
                .setFrozen(true)
                .setResizable(false)
                .setSortable(false)
                .setKey(ACTION_COLUMN_KEY);
    }

    private Button createRowMenuButton() {
        Button menuButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        menuButton.getElement().setAttribute("title", "Actions");
        menuButton.getElement().setAttribute("aria-label", "Open row menu");
        menuButton.getStyle().set("flex-shrink", "0");
        menuButton.addClickListener(event -> menuButton.getElement().executeJs(
                "const rect=this.getBoundingClientRect();"
                        + "this.dispatchEvent(new MouseEvent('contextmenu', {"
                        + "bubbles:true,cancelable:true,view:window,clientX:rect.left + rect.width/2,clientY:rect.bottom"
                        + "}));"));
        return menuButton;
    }

    private HorizontalLayout createTileHeaderSuffix(String text) {
        Span textSpan = new Span(text == null ? "" : text);
        textSpan.getStyle().set("overflow", "hidden");
        textSpan.getStyle().set("text-overflow", "ellipsis");
        textSpan.getStyle().set("white-space", "nowrap");
        textSpan.getStyle().set("min-width", "0");
        textSpan.getStyle().set("flex", "1 1 auto");

        HorizontalLayout headerSuffix = new HorizontalLayout(textSpan, createRowMenuButton());
        headerSuffix.setPadding(false);
        headerSuffix.setSpacing(true);
        headerSuffix.setAlignItems(HorizontalLayout.Alignment.CENTER);
        headerSuffix.getStyle().set("min-width", "0");
        headerSuffix.getStyle().set("max-width", "100%");
        return headerSuffix;
    }

    private Component createTaskDetailsLayout(TaskPlan plan) {
        if (plan == null || getTaskCount(plan) == 0) {
            return new Span("");
        }

        TaskGrid taskGrid = new TaskGrid(null, false, true);
        taskGrid.setTaskPlanId(plan.getId());
        taskGrid.addRefreshListener(this::refreshGrid);
        taskGrid.setWidthFull();
        taskGrid.setHeight("280px");

        Layout detailLayout = new Layout(taskGrid);
        detailLayout.setWidthFull();
        detailLayout.addClassNames(Padding.Left.LARGE);
        return detailLayout;
    }

    private GridContextMenu<TaskPlan> createContextMenu(Grid<TaskPlan> grid) {
        GridContextMenu<TaskPlan> menu = new GridContextMenu<>(grid);
        menu.setDynamicContentHandler(planEntity -> {
            menu.removeAll();

            GridMenuItem<TaskPlan> addPlanMenu = menu.addItem(new Item("Add Plan", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
            addPlanMenu.addMenuItemClickListener(click -> {
                menu.close();
                planEditor.dialogOpen(currentTypeFilter, null, PlanEditor.DialogMode.CREATE);
            });
            menu.addSeparator();

            GridMenuItem<TaskPlan> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
            displayAsTileMenu.setCheckable(true);
            displayAsTileMenu.setChecked(displayAsTile);
            displayAsTileMenu.addMenuItemClickListener(click -> {
                displayAsTile = displayAsTileMenu.isChecked();
                saveDisplayAsTilePreference();
                configureGrid();
                refreshGrid();
            });
            menu.addSeparator();

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

            if (planEntity == null) {
                menu.addComponentAsFirst(UIUtilities.getContextMenuHeader("Plan Grid"));
                return true;
            }

            menu.addSeparator();
            menu.addComponentAsFirst(UIUtilities.getContextMenuHeader(getContextMenuHeaderText(planEntity)));

            GridMenuItem<TaskPlan> viewPlanMenu = menu.addItem(new Item("View Plan", Utility.ICONS.ACTION_VIEW.getIconSource()));
            viewPlanMenu.addMenuItemClickListener(click ->
                    planEditor.dialogOpen(planEntity.getId(), planEntity.getType(), null, PlanEditor.DialogMode.DISPLAY));

            Optional<Task> activeBirthTask = findActiveBirthTask(planEntity);
            if (planEntity.getType() == Utility.TaskLinkType.BREEDER
                    && planEntity.getStatus() == Utility.TaskPlanStatus.ACTIVE
                    && activeBirthTask.isPresent()) {
                GridMenuItem<TaskPlan> createLitterMenu = menu.addItem(new Item("Create Litter", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                createLitterMenu.addMenuItemClickListener(click -> openLitterEditorForBirthTask(activeBirthTask.get(), planEntity));
            }

            if (planEntity.getStatus() == Utility.TaskPlanStatus.INCOMPLETE) {
                GridMenuItem<TaskPlan> markActiveMenu = menu.addItem(new Item("Mark Plan Active", Utility.ICONS.ACTION_CHECK.getIconSource()));
                markActiveMenu.addMenuItemClickListener(click -> {
                    menu.close();
                    taskPlanService.markActive(planEntity.getId());
                    refreshGrid();
                });
            } else {
                String markPlanIncompleteLabel = getMarkPlanIncompleteLabel(planEntity);
                Item markPlanIncompleteItem = new Item(markPlanIncompleteLabel, Utility.ICONS.STATUS_INACTIVE.getIconSource());
                String markPlanIncompleteTooltip = getMarkPlanIncompleteTooltip(markPlanIncompleteLabel);
                if (markPlanIncompleteTooltip != null) {
                    markPlanIncompleteItem.getElement().setAttribute("title", markPlanIncompleteTooltip);
                }
                GridMenuItem<TaskPlan> markIncompleteMenu = menu.addItem(markPlanIncompleteItem);
                markIncompleteMenu.addMenuItemClickListener(click -> {
                    menu.close();
                    taskPlanService.markIncomplete(planEntity.getId());
                    refreshGrid();
                });
            }

            menu.addSeparator();
            GridMenuItem<TaskPlan> deleteMenu = menu.addItem(new Item("Delete Plan", Utility.ICONS.ACTION_DELETE.getIconSource()));
            deleteMenu.addMenuItemClickListener(click -> {
                menu.close();
                ConfirmDialog confirm = new ConfirmDialog();
                confirm.setHeader("Delete plan");
                confirm.setText("Are you sure you want to delete this plan and all associated tasks? This action cannot be undone.");
                confirm.setCancelable(true);
                confirm.setCancelText("No");
                confirm.setConfirmText("Yes");
                confirm.addConfirmListener(event -> {
                    taskPlanService.deletePlanAndTasks(planEntity.getId());
                    refreshGrid();
                    confirm.close();
                });
                confirm.addCancelListener(event -> confirm.close());
                confirm.open();
            });

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

        if (currentTypeFilter != null) {
            plans = plans.stream()
                    .filter(plan -> plan.getType() == currentTypeFilter)
                    .toList();
        }

        if (associatedStockId != null) {
            plans = plans.stream()
                .filter(plan -> associatedStockId.equals(plan.getLinkMotherId())
                    || associatedStockId.equals(plan.getLinkFatherId()))
                .toList();
        }

        dataProvider = new ListDataProvider<>(plans);
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        setDataProviderForFilter();
        updateSortOrder();
        restoreExpandedDetails();
        notifyRefreshListeners();
    }

    public int getDisplayedCount() {
        return dataProvider != null ? dataProvider.getItems().size() : 0;
    }

    public Registration addRefreshListener(Runnable listener) {
        if (listener == null) {
            return () -> {};
        }

        refreshListeners.add(listener);
        return () -> refreshListeners.remove(listener);
    }

    private void notifyRefreshListeners() {
        for (Runnable listener : refreshListeners) {
            listener.run();
        }
    }

    public void setTypeFilter(Utility.TaskLinkType typeFilter) {
        this.currentTypeFilter = typeFilter;
        refreshGrid();
    }

    public void setAssociatedStockId(Integer stockId) {
        this.associatedStockId = stockId;
        refreshGrid();
    }

    public void setPreferenceScopeKey(String preferenceScopeKey) {
        this.preferenceScopeKey = preferenceScopeKey;
        loadDisplayAsTilePreference();
        configureGrid();
    }

    public Boolean getDisplayAsTile() {
        return displayAsTile;
    }

    public void setDisplayAsTile(Boolean displayAsTile) {
        this.displayAsTile = Boolean.TRUE.equals(displayAsTile);
    }

    public void loadDisplayAsTilePreference() {
        String settingsKey = getDisplayAsTilePreferenceKey();
        if (settingsKey == null) {
            return;
        }

        displayAsTile = userUiSettingsService.getValueForCurrentUser(settingsKey)
                .map(this::toBoolean)
                .orElseGet(this::getDefaultDisplayAsTileForScope);
    }

    private boolean getDefaultDisplayAsTileForScope() {
        if (preferenceScopeKey != null && preferenceScopeKey.endsWith(".mobile")) {
            return true;
        }
        if (preferenceScopeKey != null && preferenceScopeKey.endsWith(".desktop")) {
            return false;
        }
        return Boolean.TRUE.equals(displayAsTile);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return false;
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

    private void updateSortOrder() {
        List<GridSortOrder<TaskPlan>> saved = loadSortPreference();
        if (saved != null && !saved.isEmpty()) {
            sort(saved);
        }
    }

    private void saveSortPreference(List<GridSortOrder<TaskPlan>> sortOrders) {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null || sortOrders == null || sortOrders.isEmpty()) {
            return;
        }
        StringBuilder serialized = new StringBuilder();
        for (GridSortOrder<TaskPlan> order : sortOrders) {
            String key = order.getSorted().getKey();
            if (key == null) {
                continue;
            }
            if (serialized.length() > 0) {
                serialized.append(",");
            }
            serialized.append(key).append(":").append(order.getDirection().name());
        }
        if (serialized.isEmpty()) {
            return;
        }
        userUiSettingsService.setValueForCurrentUser(settingsKey, serialized.toString());
    }

    private List<GridSortOrder<TaskPlan>> loadSortPreference() {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null) {
            return null;
        }
        Optional<Object> value = userUiSettingsService.getValueForCurrentUser(settingsKey);
        if (value.isEmpty()) {
            return null;
        }
        String serialized = String.valueOf(value.get());
        if (serialized.isBlank()) {
            return null;
        }
        List<GridSortOrder<TaskPlan>> result = new ArrayList<>();
        for (String part : serialized.split(",")) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length != 2) {
                continue;
            }
            Grid.Column<TaskPlan> column = getColumnByKey(keyValue[0]);
            if (column == null) {
                continue;
            }
            try {
                SortDirection direction = SortDirection.valueOf(keyValue[1]);
                result.add(new GridSortOrder<>(column, direction));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved sort directions.
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String getSortPreferenceKey() {
        if (preferenceScopeKey == null || preferenceScopeKey.isBlank()) {
            return null;
        }
        return "grid." + getClass().getSimpleName() + "." + preferenceScopeKey + ".sortOrder";
    }

    private Optional<Task> findActiveBirthTask(TaskPlan plan) {
        if (plan == null || plan.getId() == null) {
            return Optional.empty();
        }

        return taskService.findByPlanId(plan.getId()).stream()
                .filter(task -> task.getType() == TaskType.BIRTH)
                .filter(task -> !Boolean.TRUE.equals(task.getCompleted()))
                .findFirst();
    }

    private String getMarkPlanIncompleteLabel(TaskPlan plan) {
        return isBreederPlanWithBirthTask(plan) ? "Missed Birth" : "Mark Plan Incomplete";
    }

    private String getStatusBadgeLabel(TaskPlan plan, Utility.TaskPlanStatus status) {
        if (status == Utility.TaskPlanStatus.INCOMPLETE && isBreederPlanWithBirthTask(plan)) {
            return "Missed";
        }
        return status.getShortName();
    }

    private String getMarkPlanIncompleteTooltip(String label) {
        if ("Missed Birth".equals(label)) {
            return "Mark this breeder plan incomplete due to a missed birth.";
        }
        return null;
    }

    private boolean isBreederPlanWithBirthTask(TaskPlan plan) {
        if (plan == null || plan.getId() == null || plan.getType() != Utility.TaskLinkType.BREEDER) {
            return false;
        }

        return taskService.findByPlanId(plan.getId()).stream()
                .anyMatch(task -> task.getType() == TaskType.BIRTH);
    }

    private void openLitterEditorForBirthTask(Task birthTask, TaskPlan plan) {
        if (birthTask == null) {
            return;
        }

        Integer breederId = birthTask.getLinkBreederId() != null ? birthTask.getLinkBreederId() : plan.getLinkMotherId();
        if (breederId == null) {
            return;
        }

        Stock breeder = stockService.findById(breederId);
        if (breeder == null || breeder.getStockType() == null) {
            return;
        }

        litterEditor.runTaskAction(birthTask, breeder.getStockType());
    }

    private void restoreExpandedDetails() {
        if (dataProvider == null || expandedPlanId == null) {
            return;
        }

        TaskPlan expandedPlan = dataProvider.getItems().stream()
                .filter(plan -> Objects.equals(plan.getId(), expandedPlanId))
                .findFirst()
                .orElse(null);

        if (expandedPlan == null) {
            expandedPlanId = null;
            deselectAll();
            return;
        }

        select(expandedPlan);
        if (getTaskCount(expandedPlan) > 0) {
            setDetailsVisible(expandedPlan, true);
        } else {
            setDetailsVisible(expandedPlan, false);
            expandedPlanId = null;
        }
    }

    private String getContextMenuHeaderText(TaskPlan plan) {
        if (plan == null) {
            return "Plan";
        }
        if (plan.getType() == Utility.TaskLinkType.GENERAL) {
            return "Plan: General Plan";
        }
        return "Plan: " + taskPlanService.getDisplayName(plan);
    }
}
