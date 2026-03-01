package ca.jusjoken.component;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockTypeService;

public class StockGrid extends Grid<Stock>{

    private ListDataProvider<Stock> dataProvider;
    private Integer stockId;
    private Integer litterId;
    private Stock stock;
    private StockType stockType;
    private final StockService stockService;
    private final StockTypeService stockTypeService;
    private final LitterService litterService;
    private StockSavedQuery.StockViewStyle currentViewStyle = StockSavedQuery.StockViewStyle.LIST;
    private Boolean showViewStyleChoice = false;
    private StockSavedQuery currentStockSavedQuery;
    private String currentSearchName;

    public static enum StockGridType {
        LITTER, STOCK, KITS
    }

    private StockGridType stockGridType = StockGridType.KITS;

    public StockGrid() {
        super(Stock.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
        this.litterService = Registry.getBean(LitterService.class);
    }

    public void createGrid() {
        //only configure grid after stockId or litterId is set and stockType is determined
        if(stockId != null || litterId != null || stockGridType == StockGridType.STOCK){
            configureGrid();
            refreshGrid();
        }
    }

    private void configureGrid() {
        removeAllColumns();
        clearAllHeaderRows();

        if(null == currentViewStyle){
            configureListView(); // Default to list view if style is unrecognized
        }else // Configuration logic for the KitGrid
            switch (currentViewStyle) {
                case LIST -> configureListView();
                case TILE -> configureTileView();
                case VALUE -> configureValueView();
                default -> configureListView(); // Default to list view if style is unrecognized
        }

        if(showViewStyleChoice){
            addViewStyleHeaderRow();
        }
        //refreshGrid();

    }

    private void clearAllHeaderRows() {
        while (!getHeaderRows().isEmpty()) {
            removeHeaderRow(getHeaderRows().get(0));
        }
    }

    private void configureValueView() {
        setColumnReorderingAllowed(true);
        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");
        addColumn(Stock::getBreed).setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed");
        addColumn(Stock::getStatus).setHeader("Status").setResizable(true).setAutoWidth(true).setKey("status");
        addColumn(new LocalDateTimeRenderer<>(Stock::getStatusDate,"MM-dd-YYYY HHmm")).setHeader("Status Date").setResizable(true).setAutoWidth(true).setKey("statusdate");
        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00")).setHeader("Value").setResizable(true).setAutoWidth(true).setKey("value").setFooter(getValueFooter()).setTextAlign(ColumnTextAlign.END);

    }

    private String getValueFooter() {
        List<Stock> currentStockList;
        if(stockGridType == StockGridType.STOCK){
            currentStockList = stockService.listByExample(currentSearchName, currentStockSavedQuery);
        } else {
            currentStockList = dataProvider.getItems().stream().toList();
        }

        System.out.println("getValueFooter: count:" + currentStockList.size());
        Double totalValue = 0.0;
        for (Stock item: currentStockList) {
            totalValue = totalValue + item.getStockValue();
        }
        NumberFormat usFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return "Total value: " + usFormat.format(totalValue);
    }



    private void configureTileView() {
        addColumn(stockCardRenderer);
    }

    private void configureListView() {
        //setDataProvider();
        // Additional configuration such as columns, themes, etc.
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        //setHeight("270px");
        setColumnReorderingAllowed(true);
        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");

        if(stockGridType == StockGridType.STOCK){
            addComponentColumn(stockEntity -> {
                        if (stockEntity.getBreeder()) {
                            return new Icon(Utility.ICONS.TYPE_BREEDER.getIconSource());
                        } else {
                            return null;
                        }
                    }).setHeader("Breeder").setResizable(true).setAutoWidth(true).setKey("breeder");   
        }

        addColumn(Stock::getBreed).setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed");
        addColumn(Stock::getColor).setHeader("Colour").setResizable(true).setAutoWidth(true).setKey("color");
        addColumn(Stock::getGenotype).setHeader("Genotype").setResizable(true).setAutoWidth(true).setKey("genotype");

        if(stockGridType == StockGridType.STOCK){
            addColumn(Stock::getChampNo).setHeader("ChampNo").setResizable(true).setAutoWidth(true).setKey("champno");
            addColumn(Stock::getLegs).setHeader("Legs").setResizable(true).setAutoWidth(true).setKey("legs");
            addColumn(Stock::getRegNo).setHeader("RegNo").setResizable(true).setAutoWidth(true).setKey("regno");
        }

        addColumn(Stock::getStatus).setHeader("Status").setResizable(true).setAutoWidth(true).setKey("status");
        addColumn(new LocalDateTimeRenderer<>(Stock::getStatusDate,"MM-dd-YYYY HHmm")).setHeader("Status Date").setResizable(true).setAutoWidth(true).setKey("statusdate");
        addColumn(Stock::getTattoo).setHeader("Tattoo").setResizable(true).setAutoWidth(true).setKey("tattoo");
        addColumn(Stock::getWeightInLbsOzAsString).setHeader("Weight").setResizable(true).setAutoWidth(true).setKey("weight");
        addColumn(new LocalDateRenderer<>(Stock::getAcquired,"MM-dd-YYYY")).setHeader("Aquired").setResizable(true).setAutoWidth(true).setKey("aquired");
        addColumn(new LocalDateRenderer<>(Stock::getDoB,"MM-dd-YYYY")).setHeader("DOB").setResizable(true).setAutoWidth(true).setKey("dob");
        addColumn(stockEntity -> {
            return stockEntity.getAge().getAgeFormattedString();
        }).setHeader("Age").setResizable(true).setAutoWidth(true).setKey("age");
        addColumn(stockEntity -> {
            Stock parent = stockService.getFather(stockEntity);
            if(parent==null) return null;
            return parent.getDisplayName();
        }).setHeader("Father").setResizable(true).setAutoWidth(true).setKey("father");
        addColumn(stockEntity -> {
            Stock parent = stockService.getMother(stockEntity);
            if(parent==null) return null;
            return parent.getDisplayName();
        }).setHeader("Mother").setResizable(true).setAutoWidth(true).setKey("mother");
        addColumn(stockEntity -> {return stockTypeService.getGenderForType(stockEntity.getSex(), stockEntity.getStockType());}).setHeader("Gender").setResizable(true).setAutoWidth(true).setKey("gender");
        addColumn(new NumberRenderer<>(Stock::getStockValue, "$ %(,.2f",Locale.US, "$ 0.00")).setHeader("Value").setResizable(true).setAutoWidth(true).setKey("value");
        addColumn(Stock::getNotes).setHeader("Notes").setResizable(true).setAutoWidth(true).setKey("notes");

    }

    private final ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stockEntity -> {
                return createListItemLayout(stockEntity);
            });    

    private Layout createListItemLayout(Stock stock) {
        AvatarDiv avatarDiv = stock.getAvatar(Boolean.TRUE);
        avatarDiv.getAvatar().setHeight("6em");
        avatarDiv.getAvatar().setWidth("6em");
        
        avatarDiv.getAvatar().getElement().addEventListener("click", click -> {
            //open image dialog
            //dialogCommon.setDialogTitle("Edit Profile Image");
            //dialogCommon.dialogOpen(stock,DialogCommon.DisplayMode.PROFILE_IMAGE);
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

        UIUtilities.setBorders(layout, stock, UIUtilities.BorderSize.SMALL);
        return layout;
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

    private void setStockValues() {
        if(stockId != null){
            stock = stockService.findById(stockId);
            stockType = stock.getStockType();
        }else{
            stock = null;
            stockType = stockTypeService.findRabbits();
        }
    }

    public void setId(Integer id, StockGridType stockGridType) {
        if(stockGridType == StockGridType.KITS){
            this.stockId = id;
            this.litterId = null; // Clear litterId when stockId is set
        } else if (stockGridType == StockGridType.LITTER) {
            this.litterId = id;
            this.stockId = null; // Clear stockId when litterId is set
        }
        setStockValues();
    }

    private void setStockGridDataProvider() {
        if(stockId != null){
            dataProvider = new ListDataProvider<>(stockService.getKitsForParent(stock));
            super.setDataProvider(dataProvider);
        } else if (litterId != null) {
            dataProvider = new ListDataProvider<>(stockService.getKitsForLitter(litterId));
            super.setDataProvider(dataProvider);
        }else{
            //dataProvider = new ListDataProvider<>(getListDataView().getItems().collect(Collectors.toList()));
            //super.setDataProvider(dataProvider);
        }

    }

    public void refreshGrid() {
        setStockGridDataProvider();
        if(stockGridType != StockGridType.STOCK){
            dataProvider.refreshAll();
        }
    }

    public StockGridType getStockGridType() {
        return stockGridType;
    }

    public void setStockGridType(StockGridType stockGridType) {
        this.stockGridType = stockGridType;
    }

    public StockSavedQuery.StockViewStyle getCurrentViewStyle() {
        return currentViewStyle;
    }

    public void setCurrentViewStyle(StockSavedQuery.StockViewStyle currentViewStyle) {
        this.currentViewStyle = currentViewStyle;
    }

    private void addViewStyleHeaderRow() {
        if (getColumns().isEmpty()) {
            return;
        }

        Select<StockSavedQuery.StockViewStyle> viewStyleSelect = new Select<>();
        viewStyleSelect.setWidth("100px");
        viewStyleSelect.setItems(StockSavedQuery.StockViewStyle.values());
        viewStyleSelect.setItemLabelGenerator(style -> style.getShortName());
        viewStyleSelect.setValue(currentViewStyle != null ? currentViewStyle : StockSavedQuery.StockViewStyle.LIST);

        viewStyleSelect.addValueChangeListener(event -> {
            if (!event.isFromClient() || event.getValue() == null || event.getValue() == currentViewStyle) {
                return;
            }
            currentViewStyle = event.getValue();
            configureGrid();
            refreshGrid();
        });

        HorizontalLayout rightAlignedHeader = new HorizontalLayout(new Span("View Style: "), viewStyleSelect);
        rightAlignedHeader.setWidthFull();
        rightAlignedHeader.setPadding(false);
        rightAlignedHeader.setSpacing(false);
        rightAlignedHeader.setJustifyContentMode(HorizontalLayout.JustifyContentMode.START);
        rightAlignedHeader.setAlignItems(HorizontalLayout.Alignment.BASELINE);

        // Ensure first-created/default header row exists (cannot be joined).
        if (getHeaderRows().isEmpty()) {
            HeaderRow baseHeaderRow = appendHeaderRow();
            for (Grid.Column<Stock> column : getColumns()) {
                baseHeaderRow.getCell(column).setText("");
            }
        }

        // Top-most control row.
        HeaderRow controlHeaderRow = prependHeaderRow();
        Grid.Column<Stock>[] columns = getColumns().toArray(new Grid.Column[0]);

        if (columns.length == 1) {
            controlHeaderRow.getCell(columns[0]).setComponent(rightAlignedHeader);
        } else {
            controlHeaderRow.join(columns).setComponent(rightAlignedHeader);
        }
        System.out.println("Added view style header row: row count: " + getHeaderRows().size());
    }

    public Boolean getShowViewStyleChoice() {
        return showViewStyleChoice;
    }

    public void setShowViewStyleChoice(Boolean showViewStyleChoice) {
        this.showViewStyleChoice = showViewStyleChoice;
    }

    public StockSavedQuery getCurrentStockSavedQuery() {
        return currentStockSavedQuery;
    }

    public void setCurrentStockSavedQuery(StockSavedQuery currentStockSavedQuery) {
        this.currentStockSavedQuery = currentStockSavedQuery;
    }

    public String getCurrentSearchName() {
        return currentSearchName;
    }

    public void setCurrentSearchName(String currentSearchName) {
        this.currentSearchName = currentSearchName;
    }

}
