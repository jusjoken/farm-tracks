package ca.jusjoken.views;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;

import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.ColorScheme.Value;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.ComponentConfirmEvent;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.DialogCommonEvent;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.component.PlanEditor;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.PlanTemplate;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.PlanTemplateService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockSavedQueryService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.data.service.UserUiSettingsService;
import ca.jusjoken.views.stock.StockPedigreeEditor;
import ca.jusjoken.views.stock.StockTypeView;
import ca.jusjoken.views.stock.StockView;
import ca.jusjoken.views.utility.LitterListView;
import ca.jusjoken.views.utility.MaintenanceView;
import ca.jusjoken.views.utility.PlanTemplateView;
import ca.jusjoken.views.utility.TaskListView;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements ListRefreshNeededListener, AfterNavigationObserver {
    
    private final transient AuthenticationContext authContext;
    private final StockSavedQueryService queryService;
    private final StockTypeService typeService;
    private final PlanTemplateService planTemplateService;
    private final UserUiSettingsService userUiSettingsService;
    
    private Registration planRefreshRegistration;
    private ContextMenu menu;
    private Button ftSignin;
    private String userName;
    private final SideNav nav = new SideNav();
    private final AccessAnnotationChecker accessChecker;
    private final DialogCommon dialogCommon;
    private final PlanEditor planEditor;

    private H1 viewTitle;
    private static final String DARK_MODE_PREFERENCE_KEY = "main-layout.dark-mode";
    private static final String LAST_STOCK_QUERY_PREFERENCE_KEY = "main-layout.last-stock-saved-query-id";
    private boolean welcomeAutoRedirectPending = true;

    public MainLayout(AuthenticationContext authContext, AccessAnnotationChecker accessChecker) {
        this.queryService = Registry.getBean(StockSavedQueryService.class);
        this.typeService = Registry.getBean(StockTypeService.class);
        this.planTemplateService = Registry.getBean(PlanTemplateService.class);
        this.userUiSettingsService = Registry.getBean(UserUiSettingsService.class);
        this.authContext = authContext;
        this.accessChecker = accessChecker;
        this.dialogCommon = new DialogCommon();
        this.planEditor = new PlanEditor();
        setupListeners();
        
        //StockView.class.addListener(this);
        setPrimarySection(Section.DRAWER);
        applySavedColorScheme(UI.getCurrent());
        addDrawerContent();
        addHeaderContent();
        //createHeader();
    }

    private void setupListeners(){
        dialogCommon.addListener(this);
        planEditor.addListener(this);
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE, LumoUtility.Width.FULL);
        
        addToNavbar(true, toggle, viewTitle, getUserMenu());
    }
    
    private HorizontalLayout getUserMenu(){
        HorizontalLayout userMenu = UIUtilities.getHorizontalLayout(false,false,false);
        userMenu.setAlignItems(FlexComponent.Alignment.END);
        
        userName = "None";
        if(authContext.isAuthenticated()){
            userName = authContext.getAuthenticatedUser(UserDetails.class).get().getUsername();
        }

        Image ftIcon = new Image(
            DownloadHandler.fromInputStream(event -> {
                return new DownloadResponse(
                    getClass().getClassLoader()
                    .getResourceAsStream("META-INF/resources/icons/logo-farm-tracks-icon.svg"),
                    "logo-farm-tracks-icon.svg",
                    "image/svg+xml",
                    -1
                );
            }).inline(),
            "Farm Tracks"
        );           

        ftIcon.getStyle().set("object-fit", "contain");
        ftIcon.setWidth("44px");
        
        if(authContext.isAuthenticated()){
            ftSignin = new Button(ftIcon);
            styleFtSigninButton(ftSignin);
            menu = new ContextMenu(ftSignin);
            buildContextMenu();
            userMenu.addToEnd(ftSignin);  
            return userMenu;
        }else{
            ftSignin = new Button(ftIcon);
            styleFtSigninButton(ftSignin);
            ftSignin.addClickListener(click -> {
                click.getSource().getUI().ifPresent(ui -> ui.navigate("/login"));
            });
            userMenu.addToEnd(ftSignin);
            userMenu.setAlignItems(FlexComponent.Alignment.END);
            userMenu.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            return userMenu;
        }
    }

    private void styleFtSigninButton(Button button) {
        button.getStyle().set("border-radius", "50%");
        button.getStyle().set("width", "40px");
        button.getStyle().set("height", "40px");
        button.getStyle().set("min-width", "40px");
        button.getStyle().set("padding", "0");
        button.getStyle().set("display", "inline-flex");
        button.getStyle().set("align-items", "center");
        button.getStyle().set("justify-content", "center");
        button.getStyle().set("overflow", "hidden");
    }

    private void buildContextMenu(){
        //menu = new ContextMenu(ftSignin);
        //avatar.setName("Farm Tracks Menu");
        menu.removeAll();
    
        menu.setOpenOnClick(true);

        Div heading = new Div();
        heading.setText(userName);
        heading.getStyle().set("text-align", "center");
        heading.getStyle().set("font-weight", "bold");
        heading.getStyle().set("padding", "8px");
    
        menu.addComponent(heading);
        menu.addSeparator();
        //add new stock item for each StockType
        
        List<StockType> stockTypes = typeService.findAllStockTypes();
        for(StockType type: stockTypes){
            menu.addItem("Add new " + type.getNameSingular(), event -> {
                //open stock edit dialog
                Stock newStock = new Stock();
                newStock.setExternal(false);
                newStock.setBreeder(true);
                newStock.setStockType(type);
                newStock.setWeight(0);

                dialogCommon.setDialogTitle("Create new " + type.getNameSingular());
                dialogCommon.dialogOpen(newStock,DialogCommon.DisplayMode.STOCK_DETAILS);
            });
        }

        List<PlanTemplate> planTemplates = planTemplateService.findAllGeneralPlanTemplates();
        if (!planTemplates.isEmpty()) {
            menu.addSeparator();
        }
        for(PlanTemplate template: planTemplates){
            menu.addItem("Add new " + template.getName(), event -> {
                //open plan template edit dialog
                planEditor.dialogOpen(Utility.TaskLinkType.GENERAL);
            });
        }

        menu.addSeparator();
        MenuItem mode = menu.addItem("Switch mode");
        mode.addClickListener(event -> {
            boolean useDarkMode = ColorScheme.Value.DARK != UI.getCurrentOrThrow().getPage().getColorScheme();
            UI.getCurrentOrThrow().getPage().setColorScheme(useDarkMode ? Value.DARK : Value.LIGHT);
            if (authContext.isAuthenticated()) {
                userUiSettingsService.setBooleanForCurrentUser(DARK_MODE_PREFERENCE_KEY, useDarkMode);
            }
            setModeText(mode);
        });
        setModeText(mode);

        menu.addItem("Sign out", event -> {
            authContext.logout();
        });
    }
    
    private void setModeText(MenuItem mode){
        if(ColorScheme.Value.DARK==UI.getCurrentOrThrow().getPage().getColorScheme()){
            mode.setText("Switch to Light Mode");
        }else{
            mode.setText("Switch to Dark Mode");
        }
    }

    private void addDrawerContent() {
        Scroller scroller = new Scroller(nav);
        createNavigation();
        addToDrawer(createBranding(),scroller, createFooter());
    }

    private void createNavigation() {
        System.out.println("MainLayout: createNavigation");
        nav.removeAll();

        nav.addItem(new SideNavItem("Welcome", WelcomeView.class, FontAwesome.Solid.HOME.create()));

        if(accessChecker.hasAccess(TaskListView.class)){
            SideNavItem sn = new SideNavItem("Tasks", TaskListView.class);
            nav.addItem(sn);
            sn.setPrefixComponent(FontAwesome.Solid.TASKS.create());
        }


        //build nav items for each stock query from database
        if(accessChecker.hasAccess(StockView.class)){
            List<StockType> stockTypes = typeService.findAllStockTypes();
            for(StockType type: stockTypes){
                SideNavItem stockGroupItem = new SideNavItem(type.getName());
                stockGroupItem.setExpanded(true);
                List<StockSavedQuery> stockQueryList = queryService.getSavedQueryListByType(type);
                if(!stockQueryList.isEmpty()){
                    nav.addItem(stockGroupItem);
                }
                for(StockSavedQuery query: stockQueryList){
                    SideNavItem sn = new SideNavItem(query.getSavedQueryName(), StockView.class, query.getId().toString());
                    sn.setTooltipText(query.getSavedQueryName());
                    stockGroupItem.addItem(sn);
                    sn.setPrefixComponent(FontAwesome.Solid.PAW.create());
                    sn.getElement().addEventListener("click", click -> {
                        viewTitle.setText(query.getSavedQueryName());
                    });
                }
            }

        }
        

        if(accessChecker.hasAccess(LitterListView.class)){
            SideNavItem sn = new SideNavItem("Litters", LitterListView.class);
            nav.addItem(sn);
            sn.setPrefixComponent(FontAwesome.Solid.LIST_SQUARES.create());
        }

        if(accessChecker.hasAccess(StockPedigreeEditor.class)){
            SideNavItem sn = new SideNavItem("Pedigree Manager", StockPedigreeEditor.class, "");
            nav.addItem(sn);
            sn.setPrefixComponent(FontAwesome.Solid.SITEMAP.create());
        }

        if(accessChecker.hasAccess(MaintenanceView.class) || accessChecker.hasAccess(StockTypeView.class) || accessChecker.hasAccess(PlanTemplateView.class)){
            SideNavItem utilityItem = new SideNavItem("Utility");
            utilityItem.setExpanded(true);
            nav.addItem(utilityItem);
            if(accessChecker.hasAccess(MaintenanceView.class)){
                utilityItem.addItem(new SideNavItem("Import", MaintenanceView.class, FontAwesome.Solid.COGS.create()));
            }
            if(accessChecker.hasAccess(StockTypeView.class)){
                utilityItem.addItem(new SideNavItem("Stock Types", StockTypeView.class, FontAwesome.Solid.COGS.create()));
            }
            if(accessChecker.hasAccess(PlanTemplateView.class)){
                utilityItem.addItem(new SideNavItem("Plan Templates", PlanTemplateView.class, FontAwesome.Solid.COGS.create()));
            }
        }
        
    }
    
    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    private String getCurrentPageTitle() {
        System.out.println("MainLayout: getCurrentPageTitle: " + MenuConfiguration.getPageHeader(getContent()).orElse(""));
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent); 
        System.out.println("MainLayout: onAttach");
        applySavedColorScheme(attachEvent.getUI());
        ComponentUtil.addListener(attachEvent.getUI(), ComponentConfirmEvent.class, event ->{
            createNavigation();
            if(event.getSource().getId().isPresent()){
                String currentId = event.getSource().getId().get();
                  if (currentId.isBlank()) {
                      return;
                  }
                System.out.println("MainLayout: onAttach: listener: dialog id:" + currentId);
                UI.getCurrent().navigate(StockView.class,currentId);
                viewTitle.setText(queryService.getSavedQueryById(currentId).getSavedQueryName());
            }
            
        });
    }

    @Override
    public void afterNavigation(AfterNavigationEvent ane) {
        System.out.println("MainLayout: afterNavigation");
        viewTitle.setText(getCurrentPageTitle());
        // avoid duplicate listeners after repeated navigation
        if (planRefreshRegistration != null) {
            planRefreshRegistration.remove();
            planRefreshRegistration = null;
        }

        Component content = getContent();
        if (content instanceof StockView stockView) {
            welcomeAutoRedirectPending = false;
            saveLastUsedStockSavedQueryId(stockView.getCurrentSavedQueryId());
        } else if (content instanceof WelcomeView) {
            if (welcomeAutoRedirectPending) {
                welcomeAutoRedirectPending = false;
                navigateToLastUsedStockSavedQueryIfAvailable();
            }
        }

        if (content instanceof PlanTemplateView planTemplateView) {
            planRefreshRegistration = planTemplateView.addListRefreshNeededListener(e -> {
                // handle refresh event in layout
                // e.g. rebuild menu / badges / header state
                buildContextMenu();
            });
        }

    }

    @Override
    public void listRefreshNeeded() {
        System.out.println("MainLayout: listRefreshNeeded");
        if(dialogCommon.getReturnStock()!=null){
            UIUtilities.showNotification("New " + dialogCommon.getReturnStock().getStockType().getNameSingular()+ " created.");
            ComponentUtil.fireEvent(UI.getCurrent(), new DialogCommonEvent(dialogCommon, false));
        }
    }

    private HorizontalLayout createBranding() {
        Image logo = new Image(
            DownloadHandler.fromInputStream(event -> {
                return new DownloadResponse(
                    getClass().getClassLoader()
                    .getResourceAsStream("META-INF/resources/images/logo-farm-tracks.svg"),
                    "logo-farm-tracks.svg",
                    "image/svg+xml",
                    -1
                );
            }).inline(),
            "Farm Tracks"
        );           

        logo.getStyle().set("object-fit", "contain");
        HorizontalLayout branding = new HorizontalLayout(logo);
        branding.setSpacing(true);
        branding.setPadding(true);
        //branding.setAlignItems(FlexComponent.Alignment.START);
        return branding;
    }

    private void applySavedColorScheme(UI ui) {
        if (ui == null || !authContext.isAuthenticated()) {
            return;
        }
        boolean useDarkMode = userUiSettingsService.getBooleanForCurrentUser(DARK_MODE_PREFERENCE_KEY, false);
        ui.getPage().setColorScheme(useDarkMode ? Value.DARK : Value.LIGHT);
    }

    private void saveLastUsedStockSavedQueryId(String queryId) {
        if (!authContext.isAuthenticated() || queryId == null || queryId.isBlank()) {
            return;
        }
        userUiSettingsService.setValueForCurrentUser(LAST_STOCK_QUERY_PREFERENCE_KEY, queryId);
    }

    private Optional<String> getLastUsedStockSavedQueryId() {
        if (!authContext.isAuthenticated()) {
            return Optional.empty();
        }
        return userUiSettingsService.getValueForCurrentUser(LAST_STOCK_QUERY_PREFERENCE_KEY)
                .map(String::valueOf)
                .filter(id -> !id.isBlank());
    }

    private void navigateToLastUsedStockSavedQueryIfAvailable() {
        Optional<String> lastUsedQueryId = getLastUsedStockSavedQueryId();
        if (lastUsedQueryId.isEmpty()) {
            return;
        }

        String queryId = lastUsedQueryId.get();
        boolean exists = queryService.getSavedQueryList().stream()
                .anyMatch(query -> query.getId() != null && query.getId().toString().equals(queryId));

        if (exists) {
            UI.getCurrent().navigate(StockView.class, queryId);
        } else {
            userUiSettingsService.setValueForCurrentUser(LAST_STOCK_QUERY_PREFERENCE_KEY, "");
        }
    }

}
