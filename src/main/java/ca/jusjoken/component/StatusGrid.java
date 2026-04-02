package ca.jusjoken.component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.shared.Registration;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.UserUiSettingsService;

public class StatusGrid extends Grid<StockStatusHistory> implements ListRefreshNeededListener {
    private static final String ACTION_COLUMN_KEY = "row-actions";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd-YYYY HHmm");

    private final StockStatusHistoryService statusService;
    private final UserUiSettingsService userUiSettingsService;
    private final StatusEditor statusEditor;
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private final ComponentRenderer<Component, StockStatusHistory> statusCardRenderer = new ComponentRenderer<>(this::createStatusCard);

    private ListDataProvider<StockStatusHistory> dataProvider;
    private Stock stock;
    private boolean menuCreated = false;
    private Boolean displayAsTile = false;
    private String preferenceScopeKey;
    private Registration sortListenerRegistration;
    private Grid.Column<StockStatusHistory> sortDateColumn;

    public StatusGrid() {
        super(StockStatusHistory.class, false);
        this.statusService = Registry.getBean(StockStatusHistoryService.class);
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
        this.statusEditor = new StatusEditor();
        this.statusEditor.addListener(this::listRefreshNeeded);
    }

    public void createGrid() {
        configureGrid();
        refreshGrid();
        if (!menuCreated) {
            createContextMenu(this);
            menuCreated = true;
        }
    }

    public void setStock(Stock stock) {
        this.stock = stock;
        refreshGrid();
    }

    private void configureGrid() {
        removeAllColumns();
        removeAllHeaderRows();
        removeAllFooterRows();
        removeClassName("mobile-tile-scroll-fix");

        if (Boolean.TRUE.equals(displayAsTile)) {
            addClassName("mobile-tile-scroll-fix");
            configureTileView();
        } else {
            configureListView();
        }

        setEmptyStateText("No status history available");
    }

    private void configureListView() {
        addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        setMultiSort(true);

        addRowActionsColumn();

        addComponentColumn(this::createStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(StockStatusHistory::getStatusName)
                .setKey("status");
        addColumn(item -> stock == null ? "" : item.getAge(stock, item.getSortDate().toLocalDate()))
                .setHeader("Age")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(StockStatusHistory::getSortDate)
                .setKey("age");
        sortDateColumn = addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getSortDate, "MM-dd-YYYY HHmm"))
                .setHeader("Status Date")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(StockStatusHistory::getSortDate)
                .setKey("status-date");
        addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getCreatedDate, "MM-dd-YYYY HHmm"))
                .setHeader("Created")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(StockStatusHistory::getCreatedDate)
                .setKey("created");
        addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getLastModifiedDate, "MM-dd-YYYY HHmm"))
                .setHeader("Modified")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(StockStatusHistory::getLastModifiedDate)
                .setKey("modified");
        addColumn(item -> item.getNote() == null ? "" : item.getNote())
                .setHeader("Note")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(item -> item.getNote() == null ? "" : item.getNote())
                .setKey("note");

        if (sortListenerRegistration != null) {
            sortListenerRegistration.remove();
        }
        sortListenerRegistration = addSortListener(event -> saveSortPreference(event.getSortOrder()));
    }

    private void configureTileView() {
        addColumn(statusCardRenderer).setKey("status");
    }

    private Card createStatusCard(StockStatusHistory statusHistory) {
        Card card = new Card();
        card.setWidthFull();
        card.getStyle().set("margin", "var(--lumo-space-xs) 0");
        card.addThemeVariants(CardVariant.LUMO_ELEVATED);

        card.setHeader(createStatusHeaderText(statusHistory));
        card.setHeaderSuffix(createTileHeaderSuffix(formatDateTime(statusHistory.getSortDate())));
        card.addToFooter(UIUtilities.createBadge("Age", getAgeText(statusHistory), BadgeVariant.CONTRAST));

        LocalDateTime createdDate = statusHistory.getCreatedDate();
        if (createdDate != null) {
            card.addToFooter(UIUtilities.createBadge("Created", formatDateTime(createdDate), BadgeVariant.SUCCESS));
        }

        LocalDateTime modifiedDate = statusHistory.getLastModifiedDate();
        if (modifiedDate != null) {
            card.addToFooter(UIUtilities.createBadge("Modified", formatDateTime(modifiedDate), BadgeVariant.SUCCESS));
        }

        if (statusHistory.hasNote()) {
            Badge noteBadge = UIUtilities.createBadge("Note", statusHistory.getNote());
            noteBadge.getElement().setAttribute("title", statusHistory.getNote());
            noteBadge.getStyle().set("max-width", "100%");
            noteBadge.getStyle().set("overflow", "hidden");
            noteBadge.getStyle().set("text-overflow", "ellipsis");
            noteBadge.getStyle().set("white-space", "nowrap");
            card.addToFooter(noteBadge);
        }

        return card;
    }

    private Badge createStatusBadge(StockStatusHistory statusHistory) {
        String statusName = statusHistory == null || statusHistory.getStatusName() == null ? "Unknown" : statusHistory.getStatusName();
        BadgeVariant badgeVariant = switch (statusName.toLowerCase()) {
            case "active", "listed" -> BadgeVariant.SUCCESS;
            case "deposit", "sold", "butchered" -> BadgeVariant.WARNING;
            case "died", "culled", "archived" -> BadgeVariant.CONTRAST;
            default -> null;
        };
        if (badgeVariant == null) {
            return UIUtilities.createBadge(null, statusName);
        }
        return UIUtilities.createBadge(null, statusName, badgeVariant);
    }

    private Span createStatusHeaderText(StockStatusHistory statusHistory) {
        String statusName = statusHistory == null || statusHistory.getStatusName() == null ? "Unknown" : statusHistory.getStatusName();
        Span headerText = new Span(statusName);
        headerText.getStyle().set("font-weight", "600");
        headerText.getStyle().set("text-align", "left");
        return headerText;
    }

    private String getAgeText(StockStatusHistory statusHistory) {
        if (stock == null || statusHistory == null || statusHistory.getSortDate() == null) {
            return "";
        }
        return statusHistory.getAge(stock, statusHistory.getSortDate().toLocalDate());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMAT);
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

    private void addRowActionsColumn() {
        addComponentColumn(item -> createRowMenuButton())
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

    private GridContextMenu<StockStatusHistory> createContextMenu(Grid<StockStatusHistory> grid) {
        GridContextMenu<StockStatusHistory> menu = new GridContextMenu<>(grid);
        menu.setDynamicContentHandler(statusHistory -> {
            menu.removeAll();

            GridMenuItem<StockStatusHistory> displayAsTileMenu = menu.addItem(new Item("Display as Tile", Utility.ICONS.ACTION_VIEW.getIconSource()));
            displayAsTileMenu.setCheckable(true);
            displayAsTileMenu.setChecked(displayAsTile);
            displayAsTileMenu.addMenuItemClickListener(click -> {
                displayAsTile = displayAsTileMenu.isChecked();
                saveDisplayAsTilePreference();
                configureGrid();
                refreshGrid();
            });

            if (stock != null) {
                menu.addSeparator();
                GridMenuItem<StockStatusHistory> addStatusMenu = menu.addItem(new Item("Add Status", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
                addStatusMenu.addMenuItemClickListener(click -> statusEditor.dialogOpen(stock));
            }

            if (statusHistory == null) {
                menu.addComponentAsFirst(UIUtilities.getContextMenuHeader("Status Grid"));
                return true;
            }

            menu.addSeparator();
            menu.addComponentAsFirst(UIUtilities.getContextMenuHeader("Status: " + statusHistory.getFormattedStatus()));

            GridMenuItem<StockStatusHistory> editMenu = menu.addItem(new Item("Edit Status", Utility.ICONS.ACTION_EDIT.getIconSource()));
            editMenu.addMenuItemClickListener(click -> statusEditor.dialogOpen(stock, statusHistory.getStatusName(), statusHistory, StatusEditor.DialogMode.EDIT));

            if (getDisplayedCount() > 1) {
                GridMenuItem<StockStatusHistory> deleteMenu = menu.addItem(new Item("Delete Status", Utility.ICONS.ACTION_DELETE.getIconSource()));
                deleteMenu.addMenuItemClickListener(click -> deleteStatusWithConfirm(statusHistory));
            }

            return true;
        });
        return menu;
    }

    private void deleteStatusWithConfirm(StockStatusHistory statusHistory) {
        if (stock == null || statusHistory == null) {
            return;
        }

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete \"" + statusHistory.getFormattedStatus() + "\"?");
        dialog.setText("Are you sure you want to permanently delete " + statusHistory.getFormattedStatus() + " for " + stock.getDisplayName() + "?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            statusService.delete(statusHistory, stock);
            listRefreshNeeded();
        });
        dialog.open();
    }

    private void setDataProvider() {
        if (stock == null || stock.getId() == null) {
            dataProvider = new ListDataProvider<>(List.of());
        } else {
            dataProvider = new ListDataProvider<>(statusService.findByStockId(stock.getId()));
        }
        setDataProvider(dataProvider);
    }

    public void refreshGrid() {
        setDataProvider();
        updateSortOrder();
    }

    public int getDisplayedCount() {
        return dataProvider == null ? 0 : dataProvider.getItems().size();
    }

    public void addListener(ListRefreshNeededListener listener) {
        if (listener != null) {
            listRefreshNeededListeners.add(listener);
        }
    }

    private void notifyRefreshNeeded() {
        for (ListRefreshNeededListener listener : listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

    public void setPreferenceScopeKey(String preferenceScopeKey) {
        this.preferenceScopeKey = preferenceScopeKey;
        loadDisplayAsTilePreference();
        configureGrid();
        refreshGrid();
    }

    public void applyDisplayAsTilePreference() {
        loadDisplayAsTilePreference();
        configureGrid();
        refreshGrid();
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
        if (getColumns().isEmpty()) {
            return;
        }
        List<GridSortOrder<StockStatusHistory>> saved = loadSortPreference();
        if (saved != null && !saved.isEmpty()) {
            sort(saved);
        } else if (sortDateColumn != null && !Boolean.TRUE.equals(displayAsTile)) {
            sort(List.of(new GridSortOrder<>(sortDateColumn, SortDirection.DESCENDING)));
        }
    }

    private void saveSortPreference(List<GridSortOrder<StockStatusHistory>> sortOrders) {
        String settingsKey = getSortPreferenceKey();
        if (settingsKey == null || sortOrders == null || sortOrders.isEmpty()) {
            return;
        }
        StringBuilder serialized = new StringBuilder();
        for (GridSortOrder<StockStatusHistory> order : sortOrders) {
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

    private List<GridSortOrder<StockStatusHistory>> loadSortPreference() {
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
        List<GridSortOrder<StockStatusHistory>> result = new ArrayList<>();
        for (String part : serialized.split(",")) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length != 2) {
                continue;
            }
            Grid.Column<StockStatusHistory> column = getColumnByKey(keyValue[0]);
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

    @Override
    public void listRefreshNeeded() {
        refreshGrid();
        notifyRefreshNeeded();
    }
}