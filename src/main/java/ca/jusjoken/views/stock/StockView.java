package ca.jusjoken.views.stock;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Flex;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Height;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.MinWidth;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Position;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.AvatarDiv;
import ca.jusjoken.component.ComponentConfirmEvent;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.DialogCommonEvent;
import ca.jusjoken.component.Item;
import ca.jusjoken.component.Layout;
import ca.jusjoken.component.LazyComponent;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.StatusEditor;
import ca.jusjoken.component.StockDetailsFormLayout;
import ca.jusjoken.component.WeightEditor;
import ca.jusjoken.data.ColumnName;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.BreederFilter;
import ca.jusjoken.data.Utility.TabType;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.StockWeightHistory;
import ca.jusjoken.data.entity.StockSavedQuery.StockViewStyle;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.StockRepository;
import ca.jusjoken.data.service.StockSavedQueryService;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.StockWeightHistoryService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "stock", layout = MainLayout.class)
@PermitAll
public class StockView extends Main implements ListRefreshNeededListener, HasDynamicTitle, HasUrlParameter<String> {

    private final StockRepository stockRepository;
    private final StockTypeRepository stockTypeRepository;
    private final StockTypeService stockTypeService;
    private final LitterService litterService;
    private final StockService stockService;
    private final StockSavedQueryService queryService;
    private final StockStatusHistoryService statusService;
    private final StockWeightHistoryService weightService;
    private String currentSearchName = "";
    private StockSavedQuery currentStockSavedQuery;
    private String currentSavedQueryId = null;
    private Long stockListCountAll = 0L;
    private Long stockListCount = 0L;
    private Grid<Stock> list = new Grid<>();
    private GridLazyDataView<Stock> listDataView;
    private Stock selectedStock;
    private Section sidebar;
    private NativeLabel countLabel = new NativeLabel();
    private Button saveOptionsButton = new Button(FontAwesome.Regular.SAVE.create());
    private Button applyOptionsButton = new Button(FontAwesome.Regular.CHECK_SQUARE.create());
    private Button deleteOptionsButton = new Button(FontAwesome.Regular.TRASH_CAN.create());
    private Button resetOptionsButton = new Button(FontAwesome.Solid.UNDO.create());
    private final DialogCommon dialogCommon;
    private final StatusEditor statusEditor;
    private final WeightEditor weightEditor;
    private ConfirmDialog saveQueryDialog = new ConfirmDialog();
    private ConfirmDialog deleteQueryDialog = new ConfirmDialog();
    private ConfirmDialog resetQueryDialog = new ConfirmDialog();
    private TextField saveQueryName = new TextField("Stock query name");
    private Select<StockType> stockTypeChoice = new Select<>();
    private RadioButtonGroup<BreederFilter> breederFilter = new RadioButtonGroup<>();
    private Select<StockStatus> stockStatusFilter = new Select<>();
    private Select<ColumnName> stockSort1Column = new Select<>();
    private RadioButtonGroup<String> sort1Direction = new RadioButtonGroup<>();
    private Select<ColumnName> stockSort2Column = new Select<>();
    private RadioButtonGroup<String> sort2Direction = new RadioButtonGroup<>();
    private Select<StockViewStyle> stockViewStyleChoice = new Select<>();
    private Boolean skipSidebarUpdates = Boolean.FALSE;

    public StockView(StockRepository stockRepository, LitterService litterService, StockTypeRepository stockTypeRepository, StockTypeService stockTypeService, StockService stockService, StockSavedQueryService queryService, StockStatusHistoryService statusService, StockWeightHistoryService weightService) {
        this.stockRepository = stockRepository;
        this.stockTypeRepository = stockTypeRepository;
        this.stockTypeService = stockTypeService;
        this.litterService = litterService;
        this.stockService = stockService;
        this.queryService = queryService;
        this.statusService = statusService;
        this.weightService = weightService;
        
        //this.defaultStockSort.addOrder(new SortOrder(currentSortDirection.name(), "tattoo"));
        this.dialogCommon = new DialogCommon();
        this.statusEditor = new StatusEditor();
        this.weightEditor = new WeightEditor();
        setupListeners();  
        
        addClassNames(Display.FLEX, Height.FULL, Overflow.HIDDEN);
        //loadFilters();
        add(createSidebar(), createContent());
        //applyFilters();
        closeSidebar();
        saveOptionsConfigure();
        updateStockTypeCount();
    }

    private void setupListeners(){
        dialogCommon.addListener(this);
        statusEditor.addListener(this);
        weightEditor.addListener(this);
    }

    private Hr createHr() {
        Hr hr = new Hr();
        hr.addClassNames(Margin.NONE);
        return hr;
    }
    
    private Section createSidebar() {
        H2 title = new H2("Options");
        title.addClassNames(FontSize.MEDIUM);
        saveOptionsButton.addClickListener(click -> {
            saveOptions();
        });
        applyOptionsButton.addClickListener(click -> {
            applyFilters();
        });
        deleteOptionsButton.addClickListener(click -> {
            deleteOptions();
        });
        resetOptionsButton.addClickListener(click -> {
            resetOptions();
        });

        saveOptionsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveOptionsButton.setAriaLabel("Save current options");
        saveOptionsButton.setTooltipText("Save current options");

        applyOptionsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        applyOptionsButton.setAriaLabel("Apply current options");
        applyOptionsButton.setTooltipText("Apply current options");

        deleteOptionsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        deleteOptionsButton.setAriaLabel("Delete current saved options");
        deleteOptionsButton.setTooltipText("Delete current saved options");

        resetOptionsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetOptionsButton.setAriaLabel("Reset current saved options");
        resetOptionsButton.setTooltipText("Reset current saved options");

        //Button close = new Button(LineAwesomeIcon.TIMES_SOLID.create(), e -> closeSidebar());
        Button close = new Button(FontAwesome.Solid.CLOSE.create(), e -> closeSidebar());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        close.setAriaLabel("Close options");
        close.setTooltipText("Close options");

        HorizontalLayout buttonLayout = UIUtilities.getHorizontalLayoutNoWidthCentered();
        buttonLayout.add(resetOptionsButton,deleteOptionsButton ,applyOptionsButton,saveOptionsButton,close);
        
        Layout header = new Layout(title, buttonLayout);
        header.addClassNames(Padding.End.MEDIUM, Padding.Start.LARGE, Padding.Vertical.SMALL);
        header.setAlignItems(Layout.AlignItems.CENTER);
        header.setJustifyContent(Layout.JustifyContent.BETWEEN);

        stockTypeChoice.setAriaLabel("Stock type");
        stockTypeChoice.setLabel("Stock type");
        stockTypeChoice.addClassNames(MinWidth.NONE, Padding.Top.MEDIUM);
        stockTypeChoice.setItemLabelGenerator(StockType::getName);
        stockTypeChoice.addValueChangeListener(event -> {
            currentStockSavedQuery.setStockType(event.getValue());
            updateStockTypeCount();
            sidebarChanged(Boolean.TRUE);
        });
        stockTypeChoice.setItems(stockTypeRepository.findAll());
        if(currentStockSavedQuery!=null){
            stockTypeChoice.setValue(currentStockSavedQuery.getStockType());
        }

        breederFilter.setItems(BreederFilter.filterList());
        breederFilter.setValue(BreederFilter.BREEDER);
        breederFilter.setLabel("Breeder Filter");
        breederFilter.setTooltipText("Breeder Filter");

        breederFilter.addValueChangeListener(event -> {
            if(event.getValue()!=null){
                switch (event.getValue()) {
                    case ALL -> currentStockSavedQuery.setBreeder(null);
                    case NONBREEDER -> currentStockSavedQuery.setBreeder(Boolean.FALSE);
                    case BREEDER -> currentStockSavedQuery.setBreeder(Boolean.TRUE);
                }
                sidebarChanged(Boolean.TRUE);
            }
        });

        stockStatusFilter.setAriaLabel("Status Filter");
        stockStatusFilter.setLabel("Status Filter");
        stockStatusFilter.addClassNames(MinWidth.NONE);
        
        stockStatusFilter.setRenderer(new ComponentRenderer<>(stockStatus -> {
            return new Item(stockStatus.getLongName(), stockStatus.getIcon().getIconSource());
        }));        
        
        Collection<StockStatus> stockStatusList = Utility.getInstance().getStockStatusList(true);
        stockStatusFilter.setItems(stockStatusList);
        stockStatusFilter.addComponents(Utility.getInstance().getStockStatus("all"), new Hr());
        stockStatusFilter.addValueChangeListener(event -> {
            currentStockSavedQuery.setStockStatus(event.getValue());
            sidebarChanged(Boolean.TRUE);
        });
        
        Layout filterForm = new Layout(stockTypeChoice, breederFilter, stockStatusFilter);
        filterForm.addClassNames(Padding.Horizontal.LARGE);
        filterForm.setFlexDirection(Layout.FlexDirection.COLUMN);

        stockSort1Column.setAriaLabel("Sort by (first column)");
        stockSort1Column.setLabel("Sort by (first column)");
        stockSort1Column.addClassNames(MinWidth.NONE);
        stockSort1Column.setItems(Utility.getInstance().getStockColumnNameList());
        stockSort1Column.setItemLabelGenerator(ColumnName::getDisplayName);
        stockSort1Column.addValueChangeListener(event -> {
            currentStockSavedQuery.setSort1Column(event.getValue().getColumnName());
            sidebarChanged(Boolean.TRUE);
        });
        
        sort1Direction.setItems(Sort.Direction.ASC.name(), Sort.Direction.DESC.name());
        sort1Direction.setLabel("Sort Direction");
        sort1Direction.setTooltipText("Sort Direction");
        sort1Direction.addValueChangeListener(event -> {
            currentStockSavedQuery.setSort1Direction(event.getValue());
            sidebarChanged(Boolean.TRUE);
        });

        stockSort2Column.setEmptySelectionAllowed(true);
        stockSort2Column.setAriaLabel("Sort by (second column)");
        stockSort2Column.setLabel("Sort by (second column)");
        stockSort2Column.addClassNames(MinWidth.NONE);
        stockSort2Column.setItems(Utility.getInstance().getStockColumnNameList());
        stockSort2Column.setEmptySelectionCaption("None selected");
        stockSort2Column.setItemLabelGenerator(column -> {
            if(column==null){
                return "None selected";
            }else{
                return column.getDisplayName();
            }
        });
        stockSort2Column.addValueChangeListener(event -> {
            if(event.getValue()==null){
                currentStockSavedQuery.setSort2Column(null);
            }else{
                currentStockSavedQuery.setSort2Column(event.getValue().getColumnName());
            }
            sidebarChanged(Boolean.TRUE);
        });
        
        sort2Direction.setItems(Sort.Direction.ASC.name(), Sort.Direction.DESC.name());
        sort2Direction.setLabel("Sort Direction");
        sort2Direction.setTooltipText("Sort Direction");
        sort2Direction.addValueChangeListener(event -> {
            currentStockSavedQuery.setSort2Direction(event.getValue());
            sidebarChanged(Boolean.TRUE);
        });

        Layout sortForm = new Layout(stockSort1Column, sort1Direction, stockSort2Column, sort2Direction);

        sortForm.addClassNames(Padding.Horizontal.LARGE);
        sortForm.setFlexDirection(Layout.FlexDirection.COLUMN);

        stockViewStyleChoice.setAriaLabel("View Style");
        stockViewStyleChoice.setLabel("View Style");
        stockViewStyleChoice.addClassNames(MinWidth.NONE, Padding.Top.MEDIUM);
        stockViewStyleChoice.setItemLabelGenerator(StockViewStyle::getShortName);
        stockViewStyleChoice.addValueChangeListener(event -> {
            currentStockSavedQuery.setViewStyle(event.getValue());
            sidebarChanged(Boolean.TRUE);
        });
        List<StockViewStyle> styleList = Stream.of(StockViewStyle.values()).collect(Collectors.toList());
        stockViewStyleChoice.setItems(styleList);
        if(currentStockSavedQuery!=null){
            stockViewStyleChoice.setValue(currentStockSavedQuery.getViewStyle());
        }

        Layout styleForm = new Layout(stockViewStyleChoice);

        styleForm.addClassNames(Padding.Horizontal.LARGE);
        styleForm.setFlexDirection(Layout.FlexDirection.COLUMN);
        
        this.sidebar = new Section(header, createSectionHeader("Filter options", false), filterForm, createSectionHeader("Sort options", true), sortForm, styleForm);
        this.sidebar.addClassNames("backdrop-blur-3xl", "var(--lumo-tint-90pct)", Border.RIGHT,
                Display.FLEX, FlexDirection.COLUMN, Position.ABSOLUTE, "lg:static", "bottom-1", "top-0",
                "transition-all", "z-10");
        this.sidebar.setWidth(20, Unit.REM);
        return this.sidebar;
    }
    
    @SuppressWarnings("unused")
    private void sidebarSetValues(){
        skipSidebarUpdates = Boolean.TRUE;
        stockTypeChoice.setValue(currentStockSavedQuery.getStockType());
        breederFilter.setRenderer(new TextRenderer<>(filter -> {
            if(filter.equals(BreederFilter.BREEDER)) {
                return currentStockSavedQuery.getStockType().getBreederName();
            }else if(filter.equals(BreederFilter.NONBREEDER)) {
                return currentStockSavedQuery.getStockType().getNonBreederName();
            }else{
                return "All";
            }
        }));        
        breederFilter.setValue(BreederFilter.fromIsBreeder(currentStockSavedQuery.getBreeder()));

        stockStatusFilter.setValue(currentStockSavedQuery.getStockStatus());
        stockSort1Column.setValue(Utility.getInstance().getColumnName(currentStockSavedQuery.getSort1().getColumnName()));
        sort1Direction.setValue(currentStockSavedQuery.getSort1Direction());
        if(currentStockSavedQuery.getSort2().getColumnName()==null){
            stockSort2Column.setValue(null);
        }else{
            stockSort2Column.setValue(Utility.getInstance().getColumnName(currentStockSavedQuery.getSort2().getColumnName()));
        }
        if(currentStockSavedQuery.getSort2()!=null){
            sort2Direction.setValue(currentStockSavedQuery.getSort2Direction());
        }
        stockViewStyleChoice.setValue(currentStockSavedQuery.getViewStyle());
        skipSidebarUpdates = Boolean.FALSE;
    }
    
    private void sidebarChanged(Boolean changed){
        if(skipSidebarUpdates){
        }else{
            currentStockSavedQuery.setDirty(changed);
            currentStockSavedQuery.setNeedsSaving(changed);
            applyOptionsButton.setEnabled(changed);
            saveOptionsButton.setEnabled(changed);
            resetOptionsButton.setEnabled(changed);
            if(currentSavedQueryId.equals("0")){
                deleteOptionsButton.setEnabled(false);
            }else{
                deleteOptionsButton.setEnabled(true);
            }
        }
    }
    
    //call one time to configure save options dialog
    private void saveOptionsConfigure() {
        saveQueryDialog.setHeader("Save current options?");
        Span message = new Span("Are you sure you want to save the current options to the following named saved query?");
        saveQueryName.setHelperText("Change the name to save as a new query!");
        saveQueryName.setWidthFull();
        VerticalLayout messageLayout = UIUtilities.getVerticalLayout();
        messageLayout.add(message,saveQueryName);
        
        saveQueryDialog.setText(messageLayout);
                
        saveQueryDialog.setCancelable(true);
        saveQueryDialog.addCancelListener(event -> {
            //do nothing
        });

        saveQueryDialog.setConfirmText("Save");
        saveQueryDialog.setConfirmButtonTheme("error primary");
        saveQueryDialog.addConfirmListener(event -> {
            //save the item
            if(currentStockSavedQuery.getSavedQueryName().equals(saveQueryName.getValue())){
                UIUtilities.showNotification("Saving current query overwritting:id:" + currentStockSavedQuery.getId() + " :" + currentStockSavedQuery.getSavedQueryName());
                //save the currentStockSavedQuery to the database
                currentSavedQueryId = queryService.saveQuery(currentStockSavedQuery).toString();
                //setting the dialog id the Empty String will ensure the fireEvent will NOT navigate as we are already there
                saveQueryDialog.setId("");
                loadFilters();
                applyFilters();
                sidebarChanged(Boolean.FALSE);
            }else{
                UIUtilities.showNotification("Saving current query as NEW :" + saveQueryName.getValue());
                currentSavedQueryId = queryService.saveAsQuery(currentStockSavedQuery, saveQueryName.getValue()).toString();
                //setting the dialog id will ensure the fireEvent will navigate to the id specified
                saveQueryDialog.setId(currentSavedQueryId);
            }
            ComponentUtil.fireEvent(UI.getCurrent(), new ComponentConfirmEvent(saveQueryDialog, false));
        });
    }
    
    private void saveOptions() {
        saveQueryName.setValue(currentStockSavedQuery.getSavedQueryName());

        saveQueryDialog.open();
    }
    
    private void deleteOptions(){
        deleteQueryDialog.setHeader("Delete current saved options?");
        deleteQueryDialog.setText("Are you sure you want to delete '" + currentStockSavedQuery.getSavedQueryName() + "'?");
                
        deleteQueryDialog.setCancelable(true);
        deleteQueryDialog.addCancelListener(event -> {
            //do nothing
        });

        deleteQueryDialog.setConfirmText("Delete");
        deleteQueryDialog.setConfirmButtonTheme("error primary");
        deleteQueryDialog.addConfirmListener(event -> {
            //delete the item
            UIUtilities.showNotification("Deleting current saved query:id:" + currentStockSavedQuery.getId() + " :" + currentStockSavedQuery.getSavedQueryName());
            //delete the currentStockSavedQuery from the database
            queryService.deleteQuery(currentSavedQueryId);
            //load the first query from the database and make it the current one
            currentSavedQueryId = queryService.getSavedQueryList().get(0).getId().toString();
            //setting the dialog id will ensure the fireEvent will navigate to the id specified
            deleteQueryDialog.setId(currentSavedQueryId);
            ComponentUtil.fireEvent(UI.getCurrent(), new ComponentConfirmEvent(deleteQueryDialog, false));
        });
        deleteQueryDialog.open();
    }

    private void resetOptions(){
        resetQueryDialog.setHeader("Reset/Undo current saved options?");
        resetQueryDialog.setText("Are you sure you want to reset/undo options for '" + currentStockSavedQuery.getSavedQueryName() + "'?");
                
        resetQueryDialog.setCancelable(true);
        resetQueryDialog.addCancelListener(event -> {
            //do nothing
        });

        resetQueryDialog.setConfirmText("Reset");
        resetQueryDialog.setConfirmButtonTheme("error primary");
        resetQueryDialog.addConfirmListener(event -> {
            //reset/reload the item
            //UIUtilities.showNotification("Reseting/reloading current saved query:id:" + currentStockSavedQuery.getId() + " :" + currentStockSavedQuery.getSavedQueryName());
            //reload the currentStockSavedQuery from the database
            loadFilters();
            applyFilters();
            sidebarChanged(Boolean.FALSE);
        });
        resetQueryDialog.open();
    }
    
    private Layout createSectionHeader(String title, Boolean topPadding){
        Span sectionTitle = new Span(title);
        Layout sectionHeader = new Layout(sectionTitle, createHr());
        sectionHeader.setFlexDirection(Layout.FlexDirection.COLUMN);
        sectionHeader.addClassNames(Padding.Left.MEDIUM);
        if(topPadding) sectionHeader.addClassNames(Padding.Top.MEDIUM);
        return sectionHeader;
    }

    private Component createToolbar() {
        TextField search = new TextField();
        search.addClassNames(Flex.GROW, MinWidth.NONE, Padding.Vertical.NONE);
        search.setAriaLabel("Search");
        search.setClearButtonVisible(true);
        search.setMaxWidth(25, Unit.REM);
        search.setPlaceholder("Search");
        search.setPrefixComponent(LumoIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(event -> {
            currentSearchName = event.getValue();
            applyFilters();
        });
        
        //try a menubar
        MenuBar menuBar = new MenuBar();
        menuBar.setWidthFull();

        //adding a textfield to a menubar is not recommended but seems to work well and on mobile does a good job of bringing up the overflow menu
        menuBar.addItem(search);
        
        //Options button
        Icon optionsItemIcon = new Icon(FontAwesome.Solid.COG.create().getIcon());
        optionsItemIcon.getStyle().setWidth("var(--lumo-icon-size-s)");
        optionsItemIcon.getStyle().setHeight("var(--lumo-icon-size-s)");
        optionsItemIcon.getStyle().setMarginRight("var(--lumo-space-s)");
        optionsItemIcon.getStyle().setMarginLeft("var(--lumo-space-s)");
        MenuItem optionsItem = menuBar.addItem(optionsItemIcon);
        optionsItem.add(new Text("Options"));
        optionsItem.addClickListener(e -> toggleSidebar());

        Icon gotoStartIcon = new Icon(FontAwesome.Solid.ANGLE_DOUBLE_UP.create().getIcon());
        gotoStartIcon.getStyle().setWidth("var(--lumo-icon-size-s)");
        gotoStartIcon.getStyle().setHeight("var(--lumo-icon-size-s)");
        gotoStartIcon.getStyle().setMarginRight("var(--lumo-space-s)");
        gotoStartIcon.getStyle().setMarginLeft("var(--lumo-space-s)");
        
        MenuItem gotoStartItem = menuBar.addItem(gotoStartIcon);
        gotoStartItem.addClickListener(e -> list.scrollToStart());

        Icon gotoEndIcon = new Icon(FontAwesome.Solid.ANGLE_DOUBLE_DOWN.create().getIcon());
        gotoEndIcon.getStyle().setWidth("var(--lumo-icon-size-s)");
        gotoEndIcon.getStyle().setHeight("var(--lumo-icon-size-s)");
        gotoEndIcon.getStyle().setMarginRight("var(--lumo-space-s)");
        gotoEndIcon.getStyle().setMarginLeft("var(--lumo-space-s)");
        MenuItem gotoEndItem = menuBar.addItem(gotoEndIcon);
        gotoEndItem.addClickListener(e -> list.scrollToEnd());
        
        MenuItem countLabelItem = menuBar.addItem(countLabel);
        countLabelItem.setClassName(FontSize.SMALL);
        
        Layout toolbar = new Layout(menuBar);
        toolbar.setAlignItems(Layout.AlignItems.BASELINE);
        toolbar.addClassNames(Border.BOTTOM, Padding.Horizontal.LARGE, Padding.Vertical.SMALL);
        toolbar.setGap(Layout.Gap.MEDIUM);
        
        return toolbar;
    }
    
    private void updateCounts(){
        countLabel.setText(stockListCount + "/" + stockListCountAll + " " + currentStockSavedQuery.getStockType().getName());
        countLabel.addClassNames(FontSize.SMALL);
    }
    
    private void updateStockTypeCount(){
        if(currentStockSavedQuery!=null){
            stockListCountAll = stockRepository.countByStockType(currentStockSavedQuery.getStockType());
            
        }
    }
    
    private void loadFilters(){
        System.out.println("loadFilters: currentSavedQueryId:" + currentSavedQueryId + " saveQuery:" + currentStockSavedQuery );
        if(currentSavedQueryId==null){
            currentStockSavedQuery = queryService.getSavedQueryList().get(0);
        }else if(Long.parseLong(currentSavedQueryId) < 0){
            //negative query ids are defaults created by the service so get them by the stocktype with the same id
            currentStockSavedQuery = queryService.getDefaultSaveQueryByType(stockTypeService.findById(Integer.valueOf(currentSavedQueryId)));
        }else{
            currentStockSavedQuery = queryService.getSavedQueryById(currentSavedQueryId);
        }
        //clear selectedStock
        selectedStock = null;
        sidebarChanged(Boolean.FALSE);
        updateStockTypeCount();
    }
    
    private void applyFilters(){
        //System.out.println("applyFilters: stock(start):" + currentStockSavedQuery.toString() );
        Stock tempSelected = selectedStock;
        list.setPageSize(25);
        list.setItemsPageable((pageable) -> {
            return stockService.findStockWithCustomMatcherPageable(pageable, currentSearchName, currentStockSavedQuery);
        }, countCallback -> {
            stockListCount = stockService.findStockWithCustomMatcherCount(currentSearchName, currentStockSavedQuery);
            updateCounts();
            return stockListCount;
        });
        //the above loses the selected item - so restore it from the temp one saved before
        selectedStock = tempSelected;
        
        if(selectedStock==null){
            list.scrollToIndex(0);
        }else{
            try {
                list.scrollToItem(selectedStock);
            } catch (NoSuchElementException e) {
                System.out.println("applyFilters: scrollToItem cannot find the item in the list:" + selectedStock);
                //e.printStackTrace();
            }
            //runBeforeClientResponse(ui -> list.scrollToItem(selectedStock));
            listDataView.refreshItem(selectedStock);
        }

        list.removeAllColumns();
        if(currentStockSavedQuery==null){
            list.addColumn(stockCardRenderer);
        }else{
            if(currentStockSavedQuery.getViewStyle().equals(StockViewStyle.QUICK)){
                list.addColumn(Stock::getDisplayName).setHeader("Name");
                list.addColumn(Stock::getBreed).setHeader("Breed");

                //ContextMenu menu = createContextMenu(selectedStock);
                //menu.setTarget(list);
                //menu.setOpenOnClick(false);
            }else if(currentStockSavedQuery.getViewStyle().equals(StockViewStyle.VALUE)){
                list.addColumn(Stock::getDisplayName).setHeader("Name");
                list.addColumn(Stock::getBreed).setHeader("Breed");
                list.addColumn(Stock::getStatus).setHeader("Status");
                list.addColumn(Stock::getStockValue).setHeader("Value");
            }else{
                list.addColumn(stockCardRenderer);
            }
        }
        
        applyOptionsButton.setEnabled(false);
        currentStockSavedQuery.setDirty(Boolean.FALSE);
        saveOptionsButton.setEnabled(currentStockSavedQuery.getNeedsSaving());
        sidebarSetValues();
    }
    
/*     private void runBeforeClientResponse(SerializableConsumer<UI> command) {
        getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this, context -> command.accept(ui)));
    }        
 */    
    private Component createContent() {
        Layout content = new Layout(createToolbar(), createList());
        content.addClassNames(Flex.GROW);
        content.setFlexDirection(Layout.FlexDirection.COLUMN);
        content.setOverflow(Layout.Overflow.HIDDEN);
        return content;
    }
    
    private Component createList(){
        list.setPageSize(25);
        list.setItemsPageable((pageable) -> {
            return stockService.findStockWithCustomMatcherPageable(pageable, currentSearchName, currentStockSavedQuery);
        }, countCallback -> {
            stockListCount = stockService.findStockWithCustomMatcherCount(currentSearchName, currentStockSavedQuery);
            updateCounts();
            return stockListCount;
        });

        listDataView = list.getLazyDataView();
        listDataView.setItemIndexProvider((item, query) -> {
            if(item==null) return null;
            // Use the same query to fetch the full data set with filter and sort
            List<Stock> items = stockService.listByExample(currentSearchName, currentStockSavedQuery);
            // Find the index of the matching item in the filtered and sorted list
            int index = items.indexOf(item);
            System.out.println("createList: setItemIndexProvider: index:" + index + " looking for item:" + item);
            return index >= 0 ? index : null;            
        });        

        if(currentStockSavedQuery==null){
            list.addColumn(stockCardRenderer);
        }else{
            if(currentStockSavedQuery.getViewStyle().equals(StockViewStyle.QUICK)){
                list.addColumn(Stock::getDisplayName).setHeader("Name");
                list.addColumn(Stock::getBreed).setHeader("Breed");

                //ContextMenu menu = createContextMenu(selectedStock);
                //menu.setTarget(list);
                //menu.setOpenOnClick(false);
            }else if(currentStockSavedQuery.getViewStyle().equals(StockViewStyle.VALUE)){
                list.addColumn(Stock::getDisplayName).setHeader("Name");
                list.addColumn(Stock::getBreed).setHeader("Breed");
                list.addColumn(Stock::getStatus).setHeader("Status");
                list.addColumn(Stock::getStockValue).setHeader("Value");
            }else{
                list.addColumn(stockCardRenderer);
            }
        }
        createContextMenu(list);

        list.setWidthFull();
        list.asSingleSelect().addValueChangeListener(event -> {
            Stock selected = event.getValue();
            if(selected==null){
                selectedStock = null;
            }else{
                selectedStock = selected;
            }
        });
        return list;        
    }
    
    private final ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stock -> {
                return createListItemLayout(stock);
            });    

    private Details createListItemLayout(Stock stock) {
        // Image and favourite button
//        Avatar avatar = new Avatar();
//        Div avatarDiv = new Div(avatar);
//        avatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
//        avatar.setHeight("6em");
//        avatar.setWidth("6em");
//        avatar.setImageHandler(DownloadHandler.forFile(stock.getProfileFile()));
        AvatarDiv avatarDiv = stock.getAvatar(Boolean.TRUE);
        avatarDiv.getAvatar().setHeight("6em");
        avatarDiv.getAvatar().setWidth("6em");

        
//        UIUtilities.setBorders(avatar, stock, true);
        
        avatarDiv.getAvatar().getElement().addEventListener("click", click -> {
            //open image dialog
            //dialogCommon.setDisplayMode(DialogCommon.DisplayMode.PROFILE_IMAGE);
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,DialogCommon.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");        
        
        Layout outerHeader = new Layout();
        outerHeader.add(stock.getHeader());
        
        // Extra Info - Id, Breed, Color next to avatar
        Layout extraInfo = new Layout();
        extraInfo.addClassNames(FontSize.XSMALL);
        extraInfo.addClassNames(FontWeight.LIGHT);
        extraInfo.setFlexDirection(Layout.FlexDirection.COLUMN);
        extraInfo.setAlignItems(Layout.AlignItems.START);
        extraInfo.setGap(Layout.Gap.XSMALL);
        extraInfo.setWidth(130, Unit.PIXELS);
        
        String stockId = stock.getTattoo();
        if(!stockId.isEmpty() && !stockId.equals(stock.getDisplayName())){
            Span stockIdInfo = new Span("Id: " + stockId);
            //stockIdInfo.addClassNames(FontWeight.MEDIUM);
            extraInfo.add(stockIdInfo);
        }
        
        Span breedInfo = new Span("Breed: " + stock.getBreed());

        Span colorInfo = new Span("Color: " + stock.getColor());

        extraInfo.add(breedInfo,colorInfo);
        
        // Content layout
        HorizontalLayout detailedInfo2 = new HorizontalLayout(extraInfo,createTags(stock,false));
        detailedInfo2.setFlexGrow(1, extraInfo);

        //top section layout
        Layout topLayout = new Layout(outerHeader,detailedInfo2);
        topLayout.setFlexDirection(Layout.FlexDirection.COLUMN);
        topLayout.setGap(Layout.Gap.SMALL);
        topLayout.setWidthFull();

        //top outer section layout
        Layout topOuterLayout = new Layout(avatarDiv,topLayout);
        topOuterLayout.setFlexDirection(Layout.FlexDirection.ROW);
        topOuterLayout.setGap(Layout.Gap.SMALL);
        topOuterLayout.setWidthFull();
        
        // Main layout
        Layout layout = new Layout(topOuterLayout,createTags(stock,true));
        layout.addClassNames(Padding.Top.MEDIUM, Padding.Right.MEDIUM, Padding.Left.MEDIUM, Padding.Bottom.NONE);
        layout.setFlexDirection(Layout.FlexDirection.COLUMN);
        layout.addClassNames(AlignItems.STRETCH);
        layout.setPosition(Layout.Position.RELATIVE);
        layout.setSizeUndefined();
        layout.addClassNames(Margin.NONE);

        //Main layout as a Details panel - summary visible all times and details content EXTRA info
        Details detailsPanel = new Details(layout);
        detailsPanel.add(createTabbedContentArea(stock));
        detailsPanel.addClassNames(Padding.NONE,Margin.Left.MEDIUM,Margin.Right.MEDIUM);
        UIUtilities.setBorders(detailsPanel, stock, UIUtilities.BorderSize.SMALL);
        detailsPanel.addClassNames(Border.ALL,BorderRadius.LARGE, BoxShadow.SMALL);
        detailsPanel.addThemeVariants(DetailsVariant.LUMO_REVERSE);
        
        //ContextMenu menu = createContextMenu(stock);
        //menu.setTarget(detailsPanel);
        //menu.setOpenOnClick(false);
        
        return detailsPanel;
    }

    private GridContextMenu<Stock> createContextMenu(Grid<Stock> grid) {
        GridContextMenu<Stock> menu = new GridContextMenu<>(grid);

        menu.setDynamicContentHandler(stockEntity -> {
            if(stockEntity==null) return false;
            menu.removeAll();
            String stockName = stockEntity.getDisplayName();
            Div heading = new Div();
            heading.setText(stockName);
            heading.getStyle().set("text-align", "center");
            heading.getStyle().set("font-weight", "bold");
            heading.getStyle().set("padding", "8px");
            //menu.setOpenOnClick(true);
            
            //add a label at the top with the stock name
            menu.addComponentAsFirst(heading);
            menu.addSeparator();

            GridMenuItem<Stock> addNewMenu = menu.addItem(new Item("Add new", Utility.ICONS.ACTION_ADDNEW.getIconSource()));
            addNewMenu.addMenuItemClickListener(click -> {
                //open stock edit dialog
                Stock newStock = new Stock();
                newStock.setExternal(false);
                newStock.setBreeder(true);
                newStock.setStockType(stockEntity.getStockType());
                newStock.setWeight(0);
                //newStock.setStatus("active");
                //newStock.setStatusDate(LocalDateTime.now());
                
                dialogCommon.setDialogTitle("Create new");
                dialogCommon.dialogOpen(newStock,DialogCommon.DisplayMode.STOCK_DETAILS);
                
            });
            GridMenuItem<Stock> editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
            editMenu.addMenuItemClickListener(click -> {
                //open stock edit dialog
                dialogCommon.setDialogTitle("Edit Stock");
                dialogCommon.dialogOpen(stockEntity,DialogCommon.DisplayMode.STOCK_DETAILS);
            });
            GridMenuItem<Stock> editImageMenu = menu.addItem(new Item("Edit Image", Utility.ICONS.ACTION_EDIT_IMAGE.getIconSource()));
            editImageMenu.addMenuItemClickListener(click -> {
            //open image dialog
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stockEntity,DialogCommon.DisplayMode.PROFILE_IMAGE);
            });
            menu.addItem(new Item("Breed", Utility.ICONS.TYPE_BREEDER.getIconSource()));
            menu.addItem(new Item("Birth", Utility.ICONS.ACTION_BIRTH.getIconSource()));
            createStatusMenuItem(menu, stockEntity, "sold");
            createStatusMenuItem(menu, stockEntity, "forsale");
            createStatusMenuItem(menu, stockEntity, "deposit");
            createStatusMenuItem(menu, stockEntity, "butchered");
            createStatusMenuItem(menu, stockEntity, "died");
            createStatusMenuItem(menu, stockEntity, "archived");
            createStatusMenuItem(menu, stockEntity, "culled");
            createStatusMenuItem(menu, stockEntity, "active");
            GridMenuItem<Stock> weighMenu = menu.addItem(new Item("Weigh", Utility.ICONS.ACTION_WEIGH.getIconSource()));
            weighMenu.addMenuItemClickListener(click -> {
                weightEditor.dialogOpen(stockEntity);
            });
            menu.addSeparator();
            GridMenuItem<Stock> pedigreeEditorMenu = menu.addItem(new Item("Pedigree", Utility.ICONS.ACTION_PEDIGREE.getIconSource()));
            pedigreeEditorMenu.addMenuItemClickListener(click -> {
                UI.getCurrent().navigate(StockPedigreeEditor.class, stockEntity.getId().toString());
            });
            menu.addSeparator();
            GridMenuItem<Stock> deleteMenu = menu.addItem(new Item("Delete", Utility.ICONS.ACTION_DELETE.getIconSource()));
            deleteMenu.addMenuItemClickListener(click -> {
                deleteStockEntityWithConfirm(stockEntity);
            });

            return true;
        });

        return menu;
    }
    
    private void createStatusMenuItem(GridContextMenu<Stock> menu, Stock stockEntity, String statusToEdit){
        if(!stockEntity.getStatus().equals(statusToEdit)){
            StockStatus status = Utility.getInstance().getStockStatus(statusToEdit);
            GridMenuItem<Stock> item = menu.addItem(new Item(status.getActionName(), status.getIcon().getIconSource()));
            item.addMenuItemClickListener(click -> {
                //open status editor
                statusEditor.dialogOpen(stockEntity, statusToEdit);
            });
        }
    }
    
    private void deleteStockEntityWithConfirm(Stock stockEntity){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete \"" + stockEntity.getDisplayName() + "\"?");
        dialog.setText(
                "Are you sure you want to permanently delete " + stockEntity.getDisplayName() + "?");

        dialog.setCancelable(true);
        //dialog.addCancelListener(event -> setStatus("Canceled"));

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            if(stockService.checkInUse(stockEntity)){ //if in use then do not delete and WARN
                warnStockInUse(stockEntity);
            }else{
                //delete status
                statusService.deleteByStockId(stockEntity.getId());
                //delete weight
                weightService.deleteByStockId(stockEntity.getId());
                //delete stock
                stockService.delete(stockEntity.getId());
                listRefreshNeeded();
            }
        });     
        dialog.open();
    }

    private void warnStockInUse(Stock stockEntity){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("\"" + stockEntity.getDisplayName() + "\" in use!");
        dialog.setText(
                "Cannot permanently delete " + stockEntity.getDisplayName() + " as is in use as a parent!");

        //dialog.setCancelable(false);
        //dialog.addCancelListener(event -> setStatus("Canceled"));

        dialog.setConfirmText("Ok");
        //dialog.setConfirmButtonTheme("error primary");
        dialog.open();
    }

    
    private Layout createTabbedContentArea(Stock stock){
        Layout content = new Layout();
        content.addClassNames(Padding.Top.NONE, Padding.Right.MEDIUM, Padding.Left.MEDIUM,Padding.Bottom.MEDIUM);
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Overview", createTabOverview(stock));
        tabs.add(createTab("Litters", TabType.COUNT, litterService.getLitterCountForParent(stock).toString()), createTabLitters(stock));
        tabs.add(createTab(stock.getStockType().getNonBreederName(), TabType.COUNT, stockService.getKitCountForParent(stock).toString()),createTabKits(stock));
        tabs.add(createTab("Notes", TabType.HASDATA, stock.getNotes()),createTabNotes(stock));
        tabs.add(createTab("Status'", TabType.NONE, Utility.EMPTY_VALUE),createTabStatuses(stock));
        tabs.add(createTab("Weights'", TabType.NONE, Utility.EMPTY_VALUE),createTabWeights(stock));
        
        content.add(tabs);
        return content;
    }
    
    private Tab createTab(String labelText, TabType tabType, String itemData){
        Span label = new Span(labelText);
        if(tabType.equals(TabType.COUNT) && !itemData.equals(Utility.EMPTY_VALUE)){
            Span counter = new Span(itemData);
            counter.getElement().getThemeList().add("badge pill small contrast");
            counter.getStyle().set("margin-inline-start", "var(--lumo-space-s)");
            // Accessible badge label
            String counterLabel = itemData;
            counter.getElement().setAttribute("aria-label", counterLabel);
            counter.getElement().setAttribute("title", counterLabel);
            return new Tab(label, counter);        
        }else if(tabType.equals(TabType.HASDATA) && !itemData.isEmpty()){
            Icon paperClip = FontAwesome.Solid.PAPERCLIP.create();
            paperClip.getStyle().set("padding", "var(--lumo-space-xs)");
            paperClip.setColor("var(--lumo-primary-color)");
            return new Tab(label, paperClip);        
        }else{
            return new Tab(label);        
        }
    }

    private LazyComponent createTabOverview(Stock stock) {
        Layout layout = new Layout();
        //DialogCommon overviewDialog = new DialogCommon(DialogCommon.DisplayMode.STOCK_DETAILS);
        //overviewDialog.setDialogMode(DialogCommon.DialogMode.VIEW);
        //layout.add(overviewDialog.showItem(stock, DialogCommon.DisplayMode.STOCK_DETAILS,true,false));
        layout.add(new StockDetailsFormLayout(stock));
        return new LazyComponent(() -> layout);
    }
    
    private LazyComponent createTabLitters(Stock stock) {
        Layout layout = new Layout();
        List<Litter> litters = litterService.getLitters(stock);
        Grid<Litter> litterGrid = new Grid<>(Litter.class,false);
        litterGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        litterGrid.setHeight("200px");

        litterGrid.addColumn(Litter::getPrefix).setHeader("Prefix");
        litterGrid.addColumn(Litter::getName).setHeader("Name");
        litterGrid.addColumn(Litter::getParentsFormatted).setHeader("Parents");
        litterGrid.addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY")).setHeader("Born");
        litterGrid.addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY")).setHeader("Bred");
        litterGrid.addColumn(item -> item.getKitsCount()).setHeader(stock.getStockType().getNonBreederName());
        litterGrid.addColumn(item -> item.getSurvivalRate()).setHeader("Survival Rate");
        litterGrid.addColumn(item -> item.getDiedKitsCount()).setHeader("Died");
        litterGrid.addColumn(item -> item.getKitsSurvivedCount()).setHeader("Survived");

        litterGrid.setItemDetailsRenderer(new ComponentRenderer<>(item -> {
            List<Stock> kits = stockService.getKitsForLitter(item);
            Layout kitListLayoutForOffset = new Layout(getKitListGrid(kits));
            kitListLayoutForOffset.addClassNames(Padding.Left.LARGE);
            return kitListLayoutForOffset;
        }));        
        
        litterGrid.setItems(litters);
        layout.add(litterGrid);
        return new LazyComponent(() -> layout);
    }

    private LazyComponent createTabKits(Stock stock) {
        Layout layout = new Layout();
        List<Stock> kits = stockService.getKitsForParent(stock);
        
        layout.add(getKitListGrid(kits));
        return new LazyComponent(() -> layout);
    }

    private Component getKitListGrid(List<Stock> kits){
        Grid<Stock> kitGrid = new Grid<>(Stock.class, false);
        //GridCompact<Stock> kitGrid = new GridCompact();
        kitGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        kitGrid.setHeight("200px");

        kitGrid.addColumn(Stock::getDisplayName).setHeader("Name");
        kitGrid.addColumn(Stock::getColor).setHeader("Color");
        kitGrid.addColumn(new ComponentRenderer<>(item -> {
            return new Html(item.getWeightInLbsOz());
        })).setHeader("Weight");
        kitGrid.setItems(kits);
        return kitGrid;
    }
    
    private Component createTabNotes(Stock stock) {
        //Add Edit and Save on this panel for notes
        Layout layout = new Layout();
        TextArea notes = new TextArea();
        notes.setWidthFull();
        notes.setValue(stock.getNotes());
        notes.setReadOnly(true);
        layout.add(notes);
        return layout;
    }
    
    private LazyComponent createTabStatuses(Stock stock) {
        Layout layout = new Layout();
        List<StockStatusHistory> statuses = statusService.findByStockId(stock.getId());
        Grid<StockStatusHistory> grid = new Grid<>(StockStatusHistory.class,false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        grid.setHeight("200px");

        grid.addComponentColumn(item -> {
            Icon editIcon = new Icon("lumo", "edit");
            editIcon.addClickListener(e -> {
                statusEditor.dialogOpen(stock,item.getStatusName(), item, StatusEditor.DialogMode.EDIT);
            });
            return editIcon;
        }).setWidth("50px").setFlexGrow(0).setFrozen(true);
        //only add the DELETE action column if there are more than 1 status so as to not remove the last one
        if(statuses.size()>1){
            grid.addComponentColumn(item -> {
                Icon detailsDeleteIcon = new Icon("lumo", "cross");
                detailsDeleteIcon.setTooltipText("Delete status entry");
                detailsDeleteIcon.setColor("red");
                detailsDeleteIcon.addClickListener(e -> {
                    //confirm delete
                        deleteStockStatusWithConfirm(item, stock);
                });
                return detailsDeleteIcon;
            }).setWidth("50px").setFlexGrow(0);
        }
        grid.addColumn(StockStatusHistory::getStatusName).setHeader("Status");
        grid.addColumn(item -> {
            return item.getAge(stock, item.getSortDate().toLocalDate());
        }).setHeader("Age");
        grid.addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getSortDate,"MM-dd-YYYY HHmm")).setHeader("Status Date");
        grid.addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getCreatedDate,"MM-dd-YYYY HHmm")).setHeader("Created");
        grid.addColumn(new LocalDateTimeRenderer<>(StockStatusHistory::getLastModifiedDate,"MM-dd-YYYY HHmm")).setHeader("Modified");
        grid.addColumn(StockStatusHistory::getNote).setHeader("Note");

        grid.setItems(statuses);
        layout.add(grid);
        return new LazyComponent(() -> layout);
    }
    
    private void deleteStockStatusWithConfirm(StockStatusHistory status, Stock stock){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete \"" + status.getFormattedStatus() + "\"?");
        dialog.setText(
                "Are you sure you want to permanently delete " + status.getFormattedStatus() + " for " + stock.getDisplayName() + "?");

        dialog.setCancelable(true);

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            statusService.delete(status,stock);
            listRefreshNeeded();
        });     
        dialog.open();
    }

    private LazyComponent createTabWeights(Stock stock) {
        Layout layout = new Layout();
        List<StockWeightHistory> weights = weightService.findByStockId(stock.getId());
        Grid<StockWeightHistory> grid = new Grid<>(StockWeightHistory.class,false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        grid.setHeight("200px");

        grid.addComponentColumn(item -> {
            Icon editIcon = new Icon("lumo", "edit");
            editIcon.addClickListener(e -> {
                weightEditor.dialogOpen(stock, item, WeightEditor.DialogMode.EDIT);
            });
            return editIcon;
        }).setWidth("50px").setFlexGrow(0).setFrozen(true);
        //only add the DELETE action column if there are more than 1 status so as to not remove the last one
        if(weights.size()>1){
            grid.addComponentColumn(item -> {
                Icon detailsDeleteIcon = new Icon("lumo", "cross");
                detailsDeleteIcon.setTooltipText("Delete weight entry");
                detailsDeleteIcon.setColor("red");
                detailsDeleteIcon.addClickListener(e -> {
                    //confirm delete
                        deleteStockWeightWithConfirm(item, stock);
                });
                return detailsDeleteIcon;
            }).setWidth("50px").setFlexGrow(0);

        }
        grid.addColumn(StockWeightHistory::getWeightInLbsOz).setHeader("Weight");
        grid.addColumn(item -> {
            return item.getAge(stock, item.getSortDate().toLocalDate());
        }).setHeader("Age");
        grid.addColumn(new LocalDateTimeRenderer<>(StockWeightHistory::getSortDate,"MM-dd-YYYY HHmm")).setHeader("Weight Date");
        grid.addColumn(new LocalDateTimeRenderer<>(StockWeightHistory::getCreatedDate,"MM-dd-YYYY HHmm")).setHeader("Created");
        grid.addColumn(new LocalDateTimeRenderer<>(StockWeightHistory::getLastModifiedDate,"MM-dd-YYYY HHmm")).setHeader("Modified");
        grid.addColumn(StockWeightHistory::getNote).setHeader("Note");

        grid.setItems(weights);
        layout.add(grid);
        return new LazyComponent(() -> layout);
    }
    
    private void deleteStockWeightWithConfirm(StockWeightHistory weight, Stock stock){
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete \"" + weight.getWeightInLbsOz() + "\"?");
        dialog.setText(
                "Are you sure you want to permanently delete " + weight.getWeightInLbsOz() + " for " + stock.getDisplayName() + "?");

        dialog.setCancelable(true);

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            weightService.delete(weight,stock);
            listRefreshNeeded();
        });     
        dialog.open();
    }

    private Layout createTags(Stock stock, Boolean topLayout){
        Layout tags = new Layout(
                createPanel("Litters",litterService.getLitterCountForParent(stock).toString(),false,Utility.PanelType.LITTERS), 
                createPanel(stock.getStockType().getNonBreederName(), stockService.getKitCountForParent(stock).toString(),false,Utility.PanelType.KITS), 
                createPanel("Age", stock.getAge().getAgeFormattedHTML(),true,Utility.PanelType.NONE), 
                createPanel("Weight", stock.getWeightInLbsOz(),true,Utility.PanelType.NONE));
        tags.setAlignItems(Layout.AlignItems.STRETCH);
        tags.setJustifyContent(Layout.JustifyContent.AROUND);
        //tags.setAlignSelf(Layout.AlignSelf.CENTER);
        tags.setGap(Layout.Gap.SMALL);
        //tags.addClassNames(Padding.Left.LARGE);
        tags.addClassNames(BorderRadius.MEDIUM);
        tags.setWidthFull();
        //tags.getStyle().set("max-width", "70%");
        if(topLayout){  //tags displayed at the bottom on SMALL screens
            tags.addClassNames(Display.Breakpoint.Small.HIDDEN);
       }else{  //tags displayed beside extra info on LARGE screens
            tags.addClassNames(Display.HIDDEN,Display.Breakpoint.Small.FLEX);
        }
        //setBorders(tags, stock, false);
        return tags;
        
    }
    
    private Layout createPanel(String title, String value, Boolean sourceHTML, Utility.PanelType panelType){
        if(!sourceHTML){
            value = "<p>" + value + "</p>";
        }

        Span panelTitle = new Span(title.toUpperCase());
        panelTitle.addClassNames(FontWeight.LIGHT, FontSize.XXSMALL);
        Layout layoutTitle = new Layout(panelTitle);
        
        Div panelValue = new Div();
        panelValue.getElement().setProperty("innerHTML", value);
        //panelValue.addClassNames(FontWeight.BOLD, FontSize.LARGE);
        panelValue.addClassName("tag-panel-value-responsive");
        
        Layout layoutValue = new Layout(panelValue);

        Layout panel = new Layout(layoutValue,layoutTitle);
        panel.setId(panelType.typeName);
        panel.setFlexDirection(Layout.FlexDirection.COLUMN);
        panel.addClassNames(AlignItems.CENTER);
        panel.addClassNames(Padding.SMALL);
        
        return panel;
    }
    
    private void toggleSidebar() {
        if (this.sidebar.isEnabled()) {
            closeSidebar();
        } else {
            openSidebar();
        }
    }

    private void openSidebar() {
        this.sidebar.setEnabled(true);
        this.sidebar.addClassNames(Border.RIGHT);
        // Desktop
        this.sidebar.getStyle().remove("margin-inline-start");
        // Mobile
        this.sidebar.addClassNames("start-0");
        this.sidebar.removeClassName("-start-full");
    }

    private void closeSidebar() {
        this.sidebar.setEnabled(false);
        this.sidebar.removeClassName(Border.RIGHT);
        // Desktop
        this.sidebar.getStyle().set("margin-inline-start", "-20rem");
        // Mobile
        this.sidebar.addClassNames("-start-full");
        this.sidebar.removeClassName("start-0");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ComponentUtil.addListener(attachEvent.getUI(), DialogCommonEvent.class, event ->{
            System.out.println("StockView: onAttach: Dialog Common event called: stock:" + event.getSource().getReturnStock());
            if(event.getSource().getReturnStock()!=null){
                selectedStock = event.getSource().getReturnStock();
            }
            applyFilters();
        });
    }
    

    @Override
    public void listRefreshNeeded() {
        //System.out.println("listRefreshNeeded: selectedStock:" + selectedStock);
        if(dialogCommon.getReturnStock()!=null) selectedStock = dialogCommon.getReturnStock();
        applyFilters();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        currentSavedQueryId = parameter;
        loadFilters();
        applyFilters();
    }    

    @Override
    public String getPageTitle() {
        if(currentStockSavedQuery==null) return "Stock List";
        return currentStockSavedQuery.getSavedQueryName();
    }

}
