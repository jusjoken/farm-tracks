package ca.jusjoken.views;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.ComponentConfirmEvent;
import ca.jusjoken.component.DialogCommon;
import ca.jusjoken.component.DialogCommonEvent;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockSavedQueryService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.views.stock.StockPedigreeEditor;
import ca.jusjoken.views.stock.StockView;
import ca.jusjoken.views.utility.MaintenanceView;
import com.flowingcode.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements ListRefreshNeededListener, AfterNavigationObserver {
    
    private final transient AuthenticationContext authContext;
    private final StockSavedQueryService queryService;
    private final StockTypeService typeService;
    
    private Registration registration;
    private SideNav nav = new SideNav();
    private AccessAnnotationChecker accessChecker;
    private DialogCommon dialogCommon;

    private H1 viewTitle;

    public MainLayout(AuthenticationContext authContext, AccessAnnotationChecker accessChecker) {
        this.queryService = Registry.getBean(StockSavedQueryService.class);
        this.typeService = Registry.getBean(StockTypeService.class);
        this.authContext = authContext;
        this.accessChecker = accessChecker;
        this.dialogCommon = new DialogCommon();
        dialogCommon.addListener(this);
        
        //StockView.class.addListener(this);
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
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
        
        String userName = "None";
        if(authContext.isAuthenticated()){
            userName = authContext.getAuthenticatedUser(UserDetails.class).get().getUsername();
        }
        
        Avatar avatar = new Avatar(userName);
        avatar.addClassNames(LumoUtility.Margin.Horizontal.SMALL);
        avatar.setTooltipEnabled(true);
        avatar.addClassNames(LumoUtility.Position.ABSOLUTE, LumoUtility.Position.End.XSMALL, LumoUtility.Position.Top.XSMALL);
        avatar.getStyle().set("display","block");
        avatar.getStyle().set("cursor","pointer");
        avatar.getElement().setAttribute("tabindex", "-1");
        
        ContextMenu menu = new ContextMenu(avatar);
        menu.setOpenOnClick(true);

        Div heading = new Div();
        heading.setText(userName);
        heading.getStyle().set("text-align", "center");
        heading.getStyle().set("font-weight", "bold");
        heading.getStyle().set("padding", "8px");
        
        if(authContext.isAuthenticated()){
            menu.addComponent(heading);
            menu.addSeparator();
            //add new stock item for each StockType
            
            List<StockType> stockTypes = typeService.findAllStockTypes();
            for(StockType type: stockTypes){
                MenuItem addNew = menu.addItem("Add new " + type.getNameSingular(), event -> {
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
            
            menu.addSeparator();
            MenuItem mode = menu.addItem("Switch mode", event -> {
                if(ColorScheme.Value.DARK==UI.getCurrentOrThrow().getPage().getColorScheme()){
                    UI.getCurrentOrThrow().getPage().setColorScheme(Value.LIGHT);
                }else{
                    UI.getCurrentOrThrow().getPage().setColorScheme(Value.DARK);
                }
            });
            setModeText(mode);
            mode.addClickListener(listener -> {
                setModeText(mode);
            });

            menu.addItem("Sign out", event -> {
                authContext.logout();
            });
        }else{
            menu.addItem("Sign in", event -> {
                menu.getUI().ifPresent(ui -> ui.navigate("/login"));
            });
        }

        //userMenu.add(button, popover);  
        userMenu.add(avatar);  
        return userMenu;
    }
    
    private void setModeText(MenuItem mode){
        if(ColorScheme.Value.DARK==UI.getCurrentOrThrow().getPage().getColorScheme()){
            mode.setText("Switch to Light Mode");
        }else{
            mode.setText("Switch to Dark Mode");
        }
    }

    private void addDrawerContent() {
        Span appName = new Span("Farm Tracks");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE, LumoUtility.Margin.SMALL);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(nav);
        createNavigation();
        addToDrawer(header, scroller, createFooter());
    }

    private void createNavigation() {
        System.out.println("MainLayout: createNavigation");
        nav.removeAll();

        nav.addItem(new SideNavItem("Welcome", WelcomeView.class, FontAwesome.Solid.HOME.create()));

        //build nav items for each stock query from database
        if(accessChecker.hasAccess(StockView.class)){
            List<StockType> stockTypes = typeService.findAllStockTypes();
            for(StockType type: stockTypes){
                SideNavItem stockGroupItem = new SideNavItem(type.getName());
                stockGroupItem.setExpanded(true);
                List<StockSavedQuery> stockQueryList = queryService.getSavedQueryListByType(type);
                if(stockQueryList.size()>0){
                    nav.addItem(stockGroupItem);
                }
                for(StockSavedQuery query: stockQueryList){
                    SideNavItem sn = new SideNavItem(query.getSavedQueryName(), StockView.class, query.getId().toString());
                    stockGroupItem.addItem(sn);
                    sn.setPrefixComponent(FontAwesome.Solid.PAW.create());
                    sn.getElement().addEventListener("click", click -> {
                        viewTitle.setText(query.getSavedQueryName());
                    });
                }
            }

        }
        

        if(accessChecker.hasAccess(StockPedigreeEditor.class)){
            SideNavItem sn = new SideNavItem("Pedigree Manager", StockPedigreeEditor.class, "");
            nav.addItem(sn);
            sn.setPrefixComponent(FontAwesome.Solid.SITEMAP.create());
        }
        
        if(accessChecker.hasAccess(MaintenanceView.class)){
            nav.addItem(new SideNavItem("Maintenance", MaintenanceView.class, FontAwesome.Solid.COGS.create()));
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
        registration = ComponentUtil.addListener(attachEvent.getUI(), ComponentConfirmEvent.class, event ->{
            createNavigation();
            if(event.getSource().getId().isPresent()){
                String currentId = event.getSource().getId().get();
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
    }

    @Override
    public void listRefreshNeeded() {
        System.out.println("MainLayout: listRefreshNeeded");
        if(dialogCommon.getReturnStock()!=null){
            UIUtilities.showNotification("New " + dialogCommon.getReturnStock().getStockType().getNameSingular()+ " created.");
            ComponentUtil.fireEvent(UI.getCurrent(), new DialogCommonEvent(dialogCommon, false));
        }
    }

    
    
}
