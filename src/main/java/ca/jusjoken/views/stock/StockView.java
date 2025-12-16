package ca.jusjoken.views.stock;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.ComponentConfirmEvent;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.Item;
import ca.jusjoken.component.Layout;
import ca.jusjoken.component.LazyComponent;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.RadioButtonGroupEx;
import ca.jusjoken.data.ColumnName;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.BreederFilter;
import ca.jusjoken.data.Utility.TabType;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.StockRepository;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockSavedQueryService;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.Collection;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;
import org.springframework.data.domain.Sort;

@Route(value = "stock", layout = MainLayout.class)
@PermitAll
public class StockView extends Main implements ListRefreshNeededListener, HasDynamicTitle, HasUrlParameter<String> {

    private final StockRepository stockRepository;
    private final StockTypeRepository stockTypeRepository;
    private final LitterService litterService;
    private final StockService stockService;
    private final StockSavedQueryService queryService;
    private String currentSearchName = "";
    private StockSavedQuery currentStockSavedQuery;
    private String currentSavedQueryId = null;
    private List<Stock> stockList = new ArrayList();
    private Long stockListCountAll = 0L;
    private VirtualList<Stock> list = new VirtualList<>();
    private Section sidebar;
    private NativeLabel countLabel = new NativeLabel();
    private Button saveOptionsButton = new Button(LineAwesomeIcon.SAVE_SOLID.create());
    private Button applyOptionsButton = new Button(LineAwesomeIcon.CHECK_SQUARE_SOLID.create());
    private Button deleteOptionsButton = new Button(LineAwesomeIcon.TRASH_ALT_SOLID.create());
    private Button resetOptionsButton = new Button(LineAwesomeIcon.UNDO_ALT_SOLID.create());
    private DialogCommon dialogCommon;
    private ConfirmDialog saveQueryDialog = new ConfirmDialog();
    private ConfirmDialog deleteQueryDialog = new ConfirmDialog();
    private ConfirmDialog resetQueryDialog = new ConfirmDialog();
    private TextField saveQueryName = new TextField("Stock query name");
    private Select<StockType> stockTypeChoice = new Select<>();
    private RadioButtonGroupEx<BreederFilter> breederFilter = new RadioButtonGroupEx<>();
    private Select<StockStatus> stockStatusFilter = new Select<>();
    private Select<ColumnName> stockSort1Column = new Select<>();
    private RadioButtonGroupEx<String> sort1Direction = new RadioButtonGroupEx<>();
    private Select<ColumnName> stockSort2Column = new Select<>();
    private RadioButtonGroupEx<String> sort2Direction = new RadioButtonGroupEx<>();
    private Boolean skipSidebarUpdates = Boolean.FALSE;

    public StockView(StockRepository stockRepository, LitterService litterService, StockTypeRepository stockTypeRepository, StockService stockService, StockSavedQueryService queryService) {
        this.stockRepository = stockRepository;
        this.stockTypeRepository = stockTypeRepository;
        this.litterService = litterService;
        this.stockService = stockService;
        this.queryService = queryService;
        //this.defaultStockSort.addOrder(new SortOrder(currentSortDirection.name(), "tattoo"));
        this.dialogCommon = new DialogCommon();
        dialogCommon.addListener(this);
        
        addClassNames(Display.FLEX, Height.FULL, Overflow.HIDDEN);
        //loadFilters();
        add(createSidebar(), createContent());
        //applyFilters();
        closeSidebar();
        saveOptionsConfigure();
        updateStockTypeCount();
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

        Button close = new Button(LineAwesomeIcon.TIMES_SOLID.create(), e -> closeSidebar());
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
                if(event.getValue().equals(BreederFilter.ALL)) {
                    currentStockSavedQuery.setBreeder(null);
                }else if(event.getValue().equals(BreederFilter.NONBREEDER)) {
                    currentStockSavedQuery.setBreeder(Boolean.FALSE);
                }else{
                    currentStockSavedQuery.setBreeder(Boolean.TRUE);
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
        
        Collection<StockStatus> stockStatusList = Utility.getInstance().getStockStatusList();
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
            currentStockSavedQuery.setSort1Direction(event.getValue().toString());
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
            currentStockSavedQuery.setSort2Direction(event.getValue().toString());
            sidebarChanged(Boolean.TRUE);
        });

        Layout sortForm = new Layout(stockSort1Column, sort1Direction, stockSort2Column, sort2Direction);

        sortForm.addClassNames(Padding.Horizontal.LARGE);
        sortForm.setFlexDirection(Layout.FlexDirection.COLUMN);
        
        this.sidebar = new Section(header, createSectionHeader("Filter options", false), filterForm, createSectionHeader("Sort options", true), sortForm);
        this.sidebar.addClassNames("backdrop-blur-3xl", "var(--lumo-tint-90pct)", Border.RIGHT,
                Display.FLEX, FlexDirection.COLUMN, Position.ABSOLUTE, "lg:static", "bottom-0", "top-0",
                "transition-all", "z-10");
        this.sidebar.setWidth(20, Unit.REM);
        return this.sidebar;
    }
    
    private void sidebarSetValues(){
        skipSidebarUpdates = Boolean.TRUE;
        stockTypeChoice.setValue(currentStockSavedQuery.getStockType());
        breederFilter.setRenderer(new TextRenderer<BreederFilter>(filter -> {
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
            if(currentSavedQueryId.equals(0)){
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
        
        Button options = new Button("Options", LineAwesomeIcon.COGS_SOLID.create());
        options.setTooltipText("Open the options panel for filter and sort options.");
        options.addClickListener(e -> toggleSidebar());
        
        Button gotoStart = new Button(LineAwesomeIcon.ANGLE_DOUBLE_UP_SOLID.create());
        gotoStart.setTooltipText("Go to the start of the list");
        gotoStart.addClickListener(e -> {
            list.scrollToStart();
        });
        Button gotoEnd = new Button(LineAwesomeIcon.ANGLE_DOUBLE_DOWN_SOLID.create());
        gotoEnd.setTooltipText("Go to the end of the list");
        gotoEnd.addClickListener(e -> {
            list.scrollToEnd();
        });
        
        Layout gotoLayout = new Layout(gotoStart,gotoEnd);
        

        // Remove paddings
        for (Component component : new Component[]{search, options, gotoEnd, gotoStart}) {
            component.addClassNames(Padding.Vertical.NONE);
        }
        
        Layout toolbar = new Layout(search, options, gotoLayout, countLabel);
        toolbar.setAlignItems(Layout.AlignItems.BASELINE);
        toolbar.addClassNames(Border.BOTTOM, Padding.Horizontal.LARGE, Padding.Vertical.SMALL);
        toolbar.setGap(Layout.Gap.MEDIUM);
        
        return toolbar;
    }
    
    private void updateCounts(){
        countLabel.setText(stockList.size() + "/" + stockListCountAll + " " + currentStockSavedQuery.getStockType().getName());
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
        }else{
            currentStockSavedQuery = queryService.getSavedQueryById(currentSavedQueryId);
        }
        sidebarChanged(Boolean.FALSE);
        updateStockTypeCount();
    }
    
    private void applyFilters(){
        System.out.println("applyFilters: stock(start):" + currentStockSavedQuery.toString() );
        stockList = stockService.findStockWithCustomMatcher(currentSearchName, currentStockSavedQuery);
        list.setItems(stockList);
        list.scrollToIndex(0);
        applyOptionsButton.setEnabled(false);
        currentStockSavedQuery.setDirty(Boolean.FALSE);
        saveOptionsButton.setEnabled(currentStockSavedQuery.getNeedsSaving());
        updateCounts();
        sidebarSetValues();
        //System.out.println("applyFilters: stock(end):" + currentStockSavedQuery.toString() );
    }
    
    private Component createContent() {
        Layout content = new Layout(createToolbar(), createList());
        content.addClassNames(Flex.GROW);
        content.setFlexDirection(Layout.FlexDirection.COLUMN);
        content.setOverflow(Layout.Overflow.HIDDEN);
        return content;
    }
    
    private Component createList(){
        list.setItems(stockList);
        list.setRenderer(stockCardRenderer);
        list.setWidthFull();
        return list;        
    }

    private ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stock -> {
                return createListItemLayout(stock);
            });    

    private Details createListItemLayout(Stock stock) {
        // Image and favourite button
        Avatar avatar = new Avatar();
        Div avatarDiv = new Div(avatar);
        avatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
        avatar.setHeight("8em");
        avatar.setWidth("8em");
        avatar.setImageHandler(DownloadHandler.forFile(stock.getProfileFile()));
        
        UIUtilities.setBorders(avatar, stock, true);
        
        avatar.getElement().addEventListener("click", click -> {
            //open image dialog
            dialogCommon.setDialogMode(DialogCommon.DialogMode.EDIT);
            //dialogCommon.setDisplayMode(DialogCommon.DisplayMode.PROFILE_IMAGE);
            dialogCommon.setDialogTitle("Edit Profile Image");
            dialogCommon.dialogOpen(stock,DialogCommon.DisplayMode.PROFILE_IMAGE);
        }).addEventData("event.stopPropagation()");        
        
        Layout outerHeader = new Layout();
        outerHeader.add(stock.getHeader());
        
        // Extra Info
        Layout extraInfo = new Layout();
        extraInfo.addClassNames(FontSize.XSMALL);
        extraInfo.addClassNames(FontWeight.THIN);
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
        UIUtilities.setBorders(detailsPanel, stock, false);
        detailsPanel.addClassNames(Border.ALL,BorderRadius.LARGE, BoxShadow.SMALL);
        
        ContextMenu menu = createContextMenu(stock);
        menu.setTarget(detailsPanel);
        menu.setOpenOnClick(false);
        
        return detailsPanel;
    }

    private ContextMenu createContextMenu(Stock stockEntity) {
        String stockName = stockEntity.getDisplayName();
        Div heading = new Div();
        heading.setText(stockName);
        heading.getStyle().set("text-align", "center");
        heading.getStyle().set("font-weight", "bold");
        heading.getStyle().set("padding", "8px");
        ContextMenu menu = new ContextMenu();
        menu.setOpenOnClick(true);
        
        //add a label at the top with the stock name
        menu.addComponent(heading);
        menu.addSeparator();

        MenuItem editMenu = menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
        editMenu.addClickListener(click -> {
            //open stock edit dialog
            dialogCommon.setDialogMode(DialogCommon.DialogMode.EDIT);
            dialogCommon.setDialogTitle("Edit Stock");
            dialogCommon.dialogOpen(stockEntity,DialogCommon.DisplayMode.STOCK_DETAILS);
        });
        menu.addItem(new Item("Breed", Utility.ICONS.TYPE_BREEDER.getIconSource()));
        menu.addItem(new Item("Birth", Utility.ICONS.ACTION_BIRTH.getIconSource()));
        menu.addItem(new Item("Cage Card", Utility.ICONS.ACTION_CAGE_CARD.getIconSource()));
        menu.addItem(new Item("Mark For Sale", Utility.ICONS.ACTION_MARK_FOR_SALE.getIconSource()));
        menu.addItem(new Item("Sell", Utility.ICONS.STATUS_SOLD.getIconSource()));
        menu.addItem(new Item("Deposit taken", Utility.ICONS.STATUS_SOLD_W_DEPOSIT.getIconSource()));
        menu.addItem(new Item("Butcher", Utility.ICONS.STATUS_BUTHERED.getIconSource()));
        menu.addItem(new Item("Died", Utility.ICONS.STATUS_DIED.getIconSource()));
        menu.addItem(new Item("Archive", Utility.ICONS.STATUS_ARCHIVED.getIconSource()));
        menu.addItem(new Item("Cull", Utility.ICONS.STATUS_CULLED.getIconSource()));
        menu.addItem(new Item("Delete", Utility.ICONS.ACTION_DELETE.getIconSource()));

        return menu;
    }
    
    private Layout createTabbedContentArea(Stock stock){
        Layout content = new Layout();
        content.addClassNames(Padding.Top.NONE, Padding.Right.MEDIUM, Padding.Left.MEDIUM,Padding.Bottom.MEDIUM);
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Overview", createTabOverview(stock));
        tabs.add(createTab("Litters", TabType.COUNT, stock.getLitterCountStr()), createTabLitters(stock));
        tabs.add(createTab("Kits", TabType.COUNT, stock.getKitCountStr()),createTabKits(stock));
        tabs.add(createTab("Notes", TabType.HASDATA, stock.getNotes()),createTabNotes(stock));
        content.add(tabs);
        return content;
    }
    
    private Tab createTab(String labelText, TabType tabType, String itemData){
        Span label = new Span(labelText);
        if(tabType.equals(TabType.COUNT) && !itemData.equals(Utility.emptyValue)){
            Span counter = new Span(itemData);
            counter.getElement().getThemeList().add("badge pill small contrast");
            counter.getStyle().set("margin-inline-start", "var(--lumo-space-s)");
            // Accessible badge label
            String counterLabel = itemData;
            counter.getElement().setAttribute("aria-label", counterLabel);
            counter.getElement().setAttribute("title", counterLabel);
            return new Tab(label, counter);        
        }else if(tabType.equals(TabType.HASDATA) && !itemData.isEmpty()){
            SvgIcon paperClip = LineAwesomeIcon.PAPERCLIP_SOLID.create();
            paperClip.getStyle().set("padding", "var(--lumo-space-xs)");
            paperClip.setColor("var(--lumo-primary-color)");
            return new Tab(label, paperClip);        
        }else{
            return new Tab(label);        
        }
    }

    private LazyComponent createTabOverview(Stock stock) {
        Layout layout = new Layout();
        DialogCommon overviewDialog = new DialogCommon(DialogCommon.DisplayMode.STOCK_DETAILS);
        overviewDialog.setDialogMode(DialogCommon.DialogMode.VIEW);
        layout.add(overviewDialog.showItem(stock, DialogCommon.DisplayMode.STOCK_DETAILS,true,false));
        return new LazyComponent(() -> layout);
    }
    
    private LazyComponent createTabLitters(Stock stock) {
        Layout layout = new Layout();
        List<Litter> litters = litterService.getLitters(stock);
        Grid<Litter> litterGrid = new Grid<Litter>(Litter.class,false);
        litterGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        litterGrid.setHeight("200px");

        litterGrid.addColumn(Litter::getPrefix).setHeader("Prefix");
        litterGrid.addColumn(Litter::getName).setHeader("Name");
        litterGrid.addColumn(Litter::getParentsFormatted).setHeader("Parents");
        litterGrid.addColumn(new LocalDateRenderer<>(Litter::getDoB,"MM-dd-YYYY")).setHeader("Born");
        litterGrid.addColumn(new LocalDateRenderer<>(Litter::getBred,"MM-dd-YYYY")).setHeader("Bred");
        litterGrid.addColumn(item -> item.getKitsCount()).setHeader("Kits");
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
        Grid<Stock> kitGrid = new Grid();
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
    
    private Layout createTags(Stock stock, Boolean topLayout){
        Layout tags = new Layout(
                createPanel("Litters",stock.getLitterCountStr(),false,Utility.PanelType.LITTERS, stock), 
                createPanel("Kits", stock.getKitCountStr(),false,Utility.PanelType.KITS, stock), 
                createPanel("Age", stock.getAge().getAgeFormattedHTML(),true,Utility.PanelType.NONE, stock), 
                createPanel("Weight", stock.getWeightInLbsOz(),true,Utility.PanelType.NONE, stock));
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
    
    private Layout createPanel(String title, String value, Boolean sourceHTML, Utility.PanelType panelType, Stock stock){
        if(!sourceHTML){
            value = "<p>" + value + "</p>";
        }

        Span panelTitle = new Span(title.toUpperCase());
        panelTitle.addClassNames(FontWeight.LIGHT, FontSize.XXSMALL);
        Layout layoutTitle = new Layout(panelTitle);
        
        Div panelValue = new Div();
        panelValue.getElement().setProperty("innerHTML", value);
        panelValue.addClassNames(FontWeight.BOLD, FontSize.LARGE);
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
    public void listRefreshNeeded() {
        applyFilters();
        //list.getDataProvider().refreshItem(stockRepository.findByNameAndTattoo("Aspen", "BR29"),true);
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
