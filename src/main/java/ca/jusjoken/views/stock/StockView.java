package ca.jusjoken.views.stock;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.Badge;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.GridCompact;
import ca.jusjoken.component.Item;
import ca.jusjoken.component.Layout;
import ca.jusjoken.component.LazyComponent;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.Utility.TabType;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.StockRepository;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockSort;
import ca.jusjoken.data.service.StockStatus;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.theme.RadioButtonTheme;
import ca.jusjoken.utility.BadgeVariant;
import ca.jusjoken.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.Collection;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Stock List")
@Route(value = "stock", layout = MainLayout.class)
@Menu(order = 1, icon = LineAwesomeIconUrl.TH_LIST_SOLID)
@PermitAll
public class StockView extends Main implements ListRefreshNeededListener {

    private final StockRepository stockRepository;
    private final StockTypeRepository stockTypeRepository;
    private final LitterService litterService;
    private final StockService stockService;
    private Boolean currentActiveFilterType = Boolean.TRUE;
    private Boolean currentBreederFilterType = Boolean.TRUE;
    private String currentSearchName = "";
    private StockType currentStockType;
    private StockStatus currentStockStatus = Utility.getInstance().getStockStatus("active");
    private Direction currentSortDirection = Sort.Direction.ASC;
    private StockSort defaultStockSort = new StockSort("Name, ID", new Sort.Order(currentSortDirection, "name"));
    private StockSort currentStockSort = defaultStockSort;
    private List<Stock> stockList = new ArrayList();
    private Long stockListCountAll = 0L;
    private VirtualList<Stock> list = new VirtualList<>();
    private Section sidebar;
    private NativeLabel countLabel = new NativeLabel();
    private DialogCommon dialogCommon;
    
    public StockView(StockRepository stockRepository, LitterService litterService, StockTypeRepository stockTypeRepository, StockService stockService) {
        this.stockRepository = stockRepository;
        this.stockTypeRepository = stockTypeRepository;
        this.litterService = litterService;
        this.stockService = stockService;
        this.defaultStockSort.addOrder(new Sort.Order(currentSortDirection, "tattoo"));
        this.dialogCommon = new DialogCommon();
        dialogCommon.addListener(this);
        
        //setSizeFull();
        //addClassNames(Display.FLEX, FlexDirection.COLUMN, Gap.LARGE, Padding.Left.NONE, Padding.Bottom.XSMALL);
        //add(createToolbar(), createHr(), createContent());

        addClassNames(Display.FLEX, Height.FULL, Overflow.HIDDEN);
        add(createSidebar(), createContent());
        closeSidebar();
        updateStockTypeCount();
        applyFilters();
    }

    private Hr createHr() {
        Hr hr = new Hr();
        hr.addClassNames(Margin.NONE);
        return hr;
    }
    
    private Section createSidebar() {
        H2 title = new H2("Options");
        title.addClassNames(FontSize.MEDIUM);

        Button close = new Button(LineAwesomeIcon.TIMES_SOLID.create(), e -> closeSidebar());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        close.setAriaLabel("Close options");
        close.setTooltipText("Close options");

        Layout header = new Layout(title, close);
        header.addClassNames(Padding.End.MEDIUM, Padding.Start.LARGE, Padding.Vertical.SMALL);
        header.setAlignItems(Layout.AlignItems.CENTER);
        header.setJustifyContent(Layout.JustifyContent.BETWEEN);

        Select<StockType> stockTypeChoice = new Select<>();
        stockTypeChoice.setAriaLabel("Stock type");
        stockTypeChoice.setLabel("Stock type");
        stockTypeChoice.addClassNames(MinWidth.NONE, Padding.Top.MEDIUM);
        List<StockType> stockTypeList = stockTypeRepository.findAll();
        stockTypeChoice.setItemLabelGenerator(StockType::getName);
        stockTypeChoice.setItems(stockTypeList);
        currentStockType = stockTypeRepository.findFirstByOrderByDefaultTypeDesc();
        stockTypeChoice.setValue(currentStockType);
        stockTypeChoice.addValueChangeListener(event -> {
            currentStockType = event.getValue();
            updateStockTypeCount();
            applyFilters();
        });
        
        RadioButtonGroup<String> breederFilter = new RadioButtonGroup<>();
        breederFilter.setAriaLabel("Breeder Filter");
        breederFilter.setItems("All", stockTypeChoice.getValue().getBreederName(), stockTypeChoice.getValue().getNonBreederName());
        breederFilter.addClassNames(BoxSizing.BORDER, Padding.XSMALL, Padding.Top.MEDIUM);
        breederFilter.addThemeNames(RadioButtonTheme.EQUAL_WIDTH, RadioButtonTheme.PRIMARY, RadioButtonTheme.TOGGLE);
        breederFilter.getChildren().forEach(component -> {
            component.getElement().getThemeList().add(RadioButtonTheme.PRIMARY);
            component.getElement().getThemeList().add(RadioButtonTheme.TOGGLE);
        });
        breederFilter.setLabel("Breeder Filter");
        breederFilter.setTooltipText("Breeder Filter");
        breederFilter.setValue(stockTypeChoice.getValue().getBreederName());
        breederFilter.addValueChangeListener(event -> {
            if(event.getValue().equals("All")) {
                currentBreederFilterType = null;
            }else if(event.getValue().equals(stockTypeChoice.getValue().getNonBreederName())) {
                currentBreederFilterType = Boolean.FALSE;
            }else{
                currentBreederFilterType = Boolean.TRUE;
            }
            applyFilters();
        });

        Select<StockStatus> stockStatusFilter = new Select<>();
        stockStatusFilter.setAriaLabel("Status Filter");
        stockStatusFilter.setLabel("Status Filter");
        stockStatusFilter.addClassNames(MinWidth.NONE);
        
        stockStatusFilter.setRenderer(new ComponentRenderer<>(stockStatus -> {
            return new Item(stockStatus.getLongName(), stockStatus.getIcon().getIconSource());
        }));        
        
        Collection<StockStatus> stockStatusList = Utility.getInstance().getStockStatusList();
        System.out.println("StusList:" + stockStatusList);
        //stockStatusFilter.setItemLabelGenerator(StockStatus::getLongName);
        stockStatusFilter.setItems(stockStatusList);
        stockStatusFilter.addComponents(Utility.getInstance().getStockStatus("all"), new Hr());
        stockStatusFilter.setValue(currentStockStatus);
        stockStatusFilter.addValueChangeListener(event -> {
            currentStockStatus = event.getValue();
            applyFilters();
        });
        
        Layout filterForm = new Layout(stockTypeChoice, breederFilter, stockStatusFilter);
        filterForm.addClassNames(Padding.Horizontal.LARGE);
        filterForm.setFlexDirection(Layout.FlexDirection.COLUMN);

        Select<StockSort> stockSortOptions = new Select<>();
        stockSortOptions.setAriaLabel("Sort by");
        stockSortOptions.setLabel("Sort by");
        stockSortOptions.addClassNames(MinWidth.NONE);
        List<StockSort> sortItems = new ArrayList<>();
        sortItems.add(defaultStockSort);
        sortItems.add(new StockSort("Age", new Sort.Order(Sort.Direction.ASC, "ageInDays")));
        sortItems.add(new StockSort("Breed", new Sort.Order(Sort.Direction.ASC, "breed")));
        sortItems.add(new StockSort("Birthdate", new Sort.Order(Sort.Direction.ASC, "doB")));
        sortItems.add(new StockSort("Color", new Sort.Order(Sort.Direction.ASC, "color")));
        sortItems.add(new StockSort("ID", new Sort.Order(Sort.Direction.ASC, "tattoo")));
        sortItems.add(new StockSort("Gender", new Sort.Order(Sort.Direction.ASC, "sex")));
        sortItems.add(new StockSort("Name", new Sort.Order(Sort.Direction.ASC, "name")));
        sortItems.add(new StockSort("Prefix", new Sort.Order(Sort.Direction.ASC, "prefix")));
        sortItems.add(new StockSort("Status", new Sort.Order(Sort.Direction.ASC, "status"), new Sort.Order(Sort.Direction.ASC, "name")));
        sortItems.add(new StockSort("Weight", new Sort.Order(Sort.Direction.ASC, "weight")));
        sortItems.add(new StockSort("# of Litters", new Sort.Order(Sort.Direction.ASC, "litterCount")));
        sortItems.add(new StockSort("# of Kits", new Sort.Order(Sort.Direction.ASC, "kitCount")));
        stockSortOptions.setItems(sortItems);
        stockSortOptions.setItemLabelGenerator(StockSort::getName);
        stockSortOptions.setValue(defaultStockSort);
        stockSortOptions.addValueChangeListener(event -> {
            currentStockSort = event.getValue();
            applyFilters();
        });
        
        RadioButtonGroup<String> sortDirection = new RadioButtonGroup<>();
        sortDirection.setAriaLabel("Sort Direction");
        sortDirection.setItems(Sort.Direction.ASC.name(), Sort.Direction.DESC.name());
        sortDirection.addClassNames(BoxSizing.BORDER, Padding.XSMALL, Padding.Top.MEDIUM);
        sortDirection.addThemeNames(RadioButtonTheme.EQUAL_WIDTH, RadioButtonTheme.PRIMARY, RadioButtonTheme.TOGGLE);
        sortDirection.getChildren().forEach(component -> {
            component.getElement().getThemeList().add(RadioButtonTheme.PRIMARY);
            component.getElement().getThemeList().add(RadioButtonTheme.TOGGLE);
        });
        sortDirection.setLabel("Sort Direction");
        sortDirection.setTooltipText("Sort Direction");
        sortDirection.setValue(currentSortDirection.name());
        sortDirection.addValueChangeListener(event -> {
            if(event.getValue().equals(Sort.Direction.ASC.name())) {
                currentSortDirection = Sort.Direction.ASC;
            }else{
                currentSortDirection = Sort.Direction.DESC;
            }
            applyFilters();
        });

        Layout sortForm = new Layout(stockSortOptions, sortDirection);
        sortForm.addClassNames(Padding.Horizontal.LARGE);
        sortForm.setFlexDirection(Layout.FlexDirection.COLUMN);
        
        this.sidebar = new Section(header, createSectionHeader("Filter options", false), filterForm, createSectionHeader("Sort options", true), sortForm);
        this.sidebar.addClassNames("backdrop-blur-3xl", "var(--lumo-tint-90pct)", Border.RIGHT,
                Display.FLEX, FlexDirection.COLUMN, Position.ABSOLUTE, "lg:static", "bottom-0", "top-0",
                "transition-all", "z-10");
        this.sidebar.setWidth(20, Unit.REM);
        return this.sidebar;
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
        countLabel.setText(stockList.size() + "/" + stockListCountAll + " " + currentStockType.getName());
        countLabel.addClassNames(FontSize.SMALL);
    }
    
    private void updateStockTypeCount(){
        stockListCountAll = stockRepository.countByStockType(currentStockType);
    }
    
    private void applyFilters(){
        currentStockSort.setSortDirection(currentSortDirection);
        System.out.println("activeFilters: stock:" + currentStockType.getName() + " active:" + currentActiveFilterType + " breeder:" + currentBreederFilterType + " status:" + currentStockStatus + " sort:" + currentStockSort);
        stockList = stockService.findStockWithCustomMatcher(currentSearchName, currentBreederFilterType, currentStockType, currentStockStatus, currentStockSort);
        System.out.println("List: count:" + stockList.size() + " : " + stockList);
        list.setItems(stockList);
        list.scrollToIndex(0);
        updateCounts();
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
        //setBorders(list, null, Boolean.TRUE);
        list.setWidthFull();
        return list;        
    }

    private ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stock -> {
                return createListItemLayout(stock);
            });    

    private Details createListItemLayout(Stock stock) {
        // Image and favourite button
        Image img = new Image(stock.getDefaultImageSource(), stock.getName());
        img.addClassNames(Padding.NONE);
        img.getStyle().set("max-width", "75px");
        img.getStyle().set("max-height", "75px");
        img.getStyle().set("width", "auto");
        img.getStyle().set("height", "auto");
        img.getStyle().set("border-radius", "50%");
        img.getStyle().set("object-fit", "cover");
        img.getStyle().set("display", "block");
        
        UIUtilities.setBorders(img, stock, true);


        
        Layout outerHeader = new Layout();
        outerHeader.add(stock.getHeader());
        
        Component actionMenu = UIUtilities.createDefaultActions();
        actionMenu.getStyle().set("margin-left", "auto");
        outerHeader.add(actionMenu);
        
        // Extra Info
        Layout extraInfo = new Layout();
        extraInfo.addClassNames(FontSize.XSMALL);
        extraInfo.addClassNames(FontWeight.THIN);
        extraInfo.setFlexDirection(Layout.FlexDirection.COLUMN);
        extraInfo.setAlignItems(Layout.AlignItems.START);
        extraInfo.setGap(Layout.Gap.XSMALL);
        extraInfo.setWidth(130, Unit.PIXELS);
        //extraInfo.setFlexGrow();
        //extraInfo.addClassNames(MaxWidth.FULL,"max-width", "15%");
        //extraInfo.getStyle().set("max-width", "15%");

        //extraInfo.setWidth(15, Unit.PERCENTAGE);
        
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
        Layout topOuterLayout = new Layout(img,topLayout);
        topOuterLayout.setFlexDirection(Layout.FlexDirection.ROW);
        topOuterLayout.setGap(Layout.Gap.SMALL);
        topOuterLayout.setWidthFull();
        
        // Main layout
        Layout layout = new Layout(topOuterLayout,createTags(stock,true));
        layout.addClassNames(Padding.Top.MEDIUM, Padding.Right.MEDIUM, Padding.Left.MEDIUM, Padding.Bottom.NONE);
        layout.setFlexDirection(Layout.FlexDirection.COLUMN);
        layout.addClassNames(AlignItems.STRETCH);
        layout.setPosition(Layout.Position.RELATIVE);
        //setBorders(layout, stock, false);
        //layout.addClassNames(Border.ALL,BorderRadius.LARGE, BoxShadow.SMALL);
        layout.setSizeUndefined();
        layout.addClassNames(Margin.NONE);

        //Main layout as a Details panel - summary visible all times and details content EXTRA info
        Details detailsPanel = new Details(layout);
        detailsPanel.add(createTabbedContentArea(stock));
        detailsPanel.addClassNames(Padding.NONE,Margin.Left.MEDIUM,Margin.Right.MEDIUM);
        UIUtilities.setBorders(detailsPanel, stock, false);
        detailsPanel.addClassNames(Border.ALL,BorderRadius.LARGE, BoxShadow.SMALL);
        
        return detailsPanel;
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

    private Component createTabOverview(Stock stock) {
        Layout layout = new Layout();
        layout.add(new Span("info..."));
        return layout;
    }
    
    private LazyComponent createTabKits(Stock stock) {
        List<Stock> kits = stockService.getKitsForParent(stock);
        return new LazyComponent(() -> getKitListPanel(kits));
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

    private Component getKitListGrid(List<Stock> kits){
        GridCompact<Stock> kitGrid = new GridCompact();
        //kitGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_NO_BORDER);
        //kitGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        kitGrid.setHeight("200px");

        kitGrid.addColumn(Stock::getDisplayName).setHeader("Name");
        kitGrid.addColumn(Stock::getColor).setHeader("Color");
        kitGrid.addColumn(new ComponentRenderer<>(item -> {
            return new Html(item.getWeightInLbsOz());
        })).setHeader("Weight");
        kitGrid.setItems(kits);
        return kitGrid;
    }
    
    private Scroller getKitListPanel(List<Stock> kits){
            Layout div = new Layout();
            Scroller scroll = new Scroller(div);
            scroll.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
            scroll.setMaxHeight("128px");
            div.setFlexWrap(Layout.FlexWrap.WRAP);
            div.addClassNames(Margin.NONE, Padding.NONE);
            for(Stock kit : kits){
                div.add(createMiniKitPanel(kit));
            }
            
            return scroll;
        
    }
    
    /*
    private Card createMiniKitCard(Stock kit){
        Card card = new Card();
        //card.setTitle(new Span(kit.getDisplayName()));
        //card.setSubtitle(new Span(kit.getColor()));
        Layout headerRow = new Layout(new Span(kit.getDisplayName()));
        headerRow.setAlignItems(Layout.AlignItems.BASELINE);
        card.setHeader(headerRow);
        Component actionMenu = createDefaultActions();
        actionMenu.getStyle().set("margin-left", "auto");
        actionMenu.addClassNames(Padding.NONE, Margin.Top.NONE,Margin.Bottom.NONE);
        card.setHeaderSuffix(actionMenu);
        
        card.add(new Html(kit.getWeightInLbsOz()));
        card.addThemeVariants(CardVariant.LUMO_ELEVATED,CardVariant.LUMO_HORIZONTAL,CardVariant.LUMO_OUTLINED);
        card.addClassNames(Margin.XSMALL, Padding.NONE);
        card.setWidth("134px");
        card.setHeight("62px");
        return card;
    }
    */

    private Layout createMiniKitPanel(Stock kit){
        Layout box = new Layout();
        box.setFlexDirection(Layout.FlexDirection.COLUMN);
        box.addClassNames(FontSize.XSMALL);
        box.addClassNames(FontWeight.LIGHT);
        box.setWidth("134px");
        box.setHeight("62px");
        box.addClassNames(Margin.XSMALL, Padding.XSMALL);
        Layout headerRow = new Layout(new Span(kit.getDisplayName()));
        headerRow.setAlignItems(Layout.AlignItems.BASELINE);
        Component actionMenu = UIUtilities.createDefaultActions();
        actionMenu.getStyle().set("margin-left", "auto");
        actionMenu.addClassNames(Padding.NONE, Margin.Top.NONE,Margin.Bottom.NONE);
        headerRow.add(actionMenu);
        box.add(headerRow);
        box.add(new Span(kit.getColor()));
        box.add(new Html(kit.getWeightInLbsOz()));

        UIUtilities.setBorders(box, null, false);
        box.addClassNames(Border.ALL,BorderRadius.MEDIUM, BoxShadow.SMALL);
        return box;
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
        //panel.addClassNames(Border.ALL,BorderColor.CONTRAST_20,BorderRadius.MEDIUM,Padding.SMALL);
        panel.addClassNames(Padding.SMALL);
        
        //Popover panelViewer = new Popover();
        Dialog panelViewer = new Dialog();
        //panelViewer.setTarget(panel);
        panelViewer.setModal(false);
        //panelViewer.setOpenOnClick(false);
        //panelViewer.addThemeVariants(PopoverVariant.ARROW);
        /*
        if(panel.getId().equals(Utility.PanelType.LITTERS.typeName)){
            panelViewer.add(createTabLitters(stock));
        }else if(panel.getId().equals(Utility.PanelType.KITS.typeName)){
            panelViewer.add(createTabKits(stock));
        }
        */
        
        panel.getElement().addEventListener("click", click -> {
            String eventId = click.getSource().getAttribute("id");
            if(eventId.equals(Utility.PanelType.LITTERS.typeName)){
                dialogCommon.setDialogMode(DialogCommon.DialogMode.EDIT);
                dialogCommon.setDisplayMode(DialogCommon.DisplayMode.LITTER_LIST);
                dialogCommon.setDialogTitle("Litters");
                dialogCommon.dialogOpen(stock);

                //panelViewer.removeAll();
                //panelViewer.add(createTabLitters(stock));
                //panelViewer.setWidth("400px");
                //panelViewer.open();
                //open popover or dialog
            }else if(eventId.equals(Utility.PanelType.KITS.typeName)){
                dialogCommon.setDialogMode(DialogCommon.DialogMode.EDIT);
                dialogCommon.setDisplayMode(DialogCommon.DisplayMode.KIT_LIST);
                dialogCommon.setDialogTitle("Kits");
                dialogCommon.dialogOpen(stock);
                //panelViewer.removeAll();
                //panelViewer.add(createTabKits(stock));
                //panelViewer.setWidth("400px");
                //panelViewer.open();
                //open popover or dialog
            }else{
                //do nothing
            }
        }).addEventData("event.stopPropagation()");
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
        stockList = stockService.findAll();
        //grid.setItems(stockList);
        //grid.getDataProvider().refreshAll();
        //onFilterChange();
    }

}
