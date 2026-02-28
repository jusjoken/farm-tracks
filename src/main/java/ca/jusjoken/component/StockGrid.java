package ca.jusjoken.component;

import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

    public static enum StockGridType {
        LITTER, STOCK
    }

    private StockGridType stockGridType = StockGridType.STOCK;

    public StockGrid() {
        super(Stock.class, false);
        this.stockService = Registry.getBean(StockService.class);
        this.stockTypeService = Registry.getBean(StockTypeService.class);
        this.litterService = Registry.getBean(LitterService.class);
    }

    public void createGrid() {
        System.out.println("Creating StockGrid with stockId: " + stockId + " and litterId: " + litterId);
        //only configure grid after stockId or litterId is set and stockType is determined
        if(stockId != null || litterId != null){
            configureGrid();
            refreshGrid();
        }
    }

    private void configureGrid() {
        removeAllColumns();
        if(null == currentViewStyle){
            configureListView(); // Default to list view if style is unrecognized
        }else // Configuration logic for the KitGrid
        switch (currentViewStyle) {
            case LIST -> configureListView();
            case TILE -> configureTileView();
            case VALUE -> configureValueView();
            default -> configureListView(); // Default to list view if style is unrecognized
        }
        //refreshGrid();

    }

    private void configureValueView() {
        System.out.println("Configuring Value View for StockGrid with stockId: " + stockId + " and litterId: " + litterId);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'configureValueView'");
    }

    private void configureTileView() {
        System.out.println("Configuring Tile View for StockGrid with stockId: " + stockId + " and litterId: " + litterId);
        addColumn(stockCardRenderer);
    }

    private void configureListView() {
        System.out.println("Configuring StockGrid with stockId: " + stockId + " and litterId: " + litterId);
        //setDataProvider();
        // Additional configuration such as columns, themes, etc.
        addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);
        //setHeight("270px");
        addComponentColumn(stockEntity -> {
            return stockEntity.getnameAndPrefix(false,true, true);
        }).setHeader("Name").setAutoWidth(true).setFrozen(true).setResizable(true).setKey("name");
        addColumn(Stock::getBreed).setHeader("Breed").setResizable(true).setAutoWidth(true).setKey("breed");
        addColumn(Stock::getColor).setHeader("Colour").setResizable(true).setAutoWidth(true).setKey("color");
        addColumn(Stock::getGenotype).setHeader("Genotype").setResizable(true).setAutoWidth(true).setKey("genotype");
        addColumn(Stock::getNotes).setHeader("Notes").setResizable(true).setAutoWidth(true).setKey("notes");
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

    }

    private final ComponentRenderer<Component, Stock> stockCardRenderer = new ComponentRenderer<>(
            stock -> {
                return createListItemLayout(stock);
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
        System.out.println("Setting ID for StockGrid: " + id + " with StockGridType: " + stockGridType);
        if(stockGridType == StockGridType.STOCK){
            this.stockId = id;
            this.litterId = null; // Clear litterId when stockId is set
        } else if (stockGridType == StockGridType.LITTER) {
            this.litterId = id;
            this.stockId = null; // Clear stockId when litterId is set
        }
        setStockValues();
        System.out.println("Stock values set for StockGrid with stockId: " + stockId + " and litterId: " + litterId);
    }

    private void setStockGridDataProvider() {
        System.out.println("Setting data provider for StockGrid with stockId: " + stockId + " and litterId: " + litterId);
        if(stockId != null){
            System.out.println("Fetching stock for stockId: " + stockId + " stock: " + stock);
            dataProvider = new ListDataProvider<>(stockService.getKitsForParent(stock));
        } else if (litterId != null) {
            System.out.println("Fetching stock for litterId: " + litterId);
            dataProvider = new ListDataProvider<>(stockService.getKitsForLitter(litterId));
        }else{
            dataProvider = new ListDataProvider<>(List.of());
        }
        System.out.println("Data provider set with " + dataProvider.getItems().size() + " items for StockGrid with stockId: " + stockId + " and litterId: " + litterId);

        // IMPORTANT: bind provider to Vaadin Grid
        super.setDataProvider(dataProvider);

    }

    public void refreshGrid() {
        setStockGridDataProvider();
        dataProvider.refreshAll();
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
    
}
