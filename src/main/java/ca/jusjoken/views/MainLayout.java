package ca.jusjoken.views;

import ca.jusjoken.component.ComponentConfirmEvent;
import ca.jusjoken.component.UserDialog;
import ca.jusjoken.data.entity.StockSavedQuery;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockSavedQueryService;
import ca.jusjoken.views.stock.StockView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
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
public class MainLayout extends AppLayout{
    
    private final transient AuthenticationContext authContext;
    private final StockSavedQueryService queryService;
    //private StockView stockView = new StockView();
    
    private Registration registration;
    private SideNav nav = new SideNav();

    private H1 viewTitle;

    public MainLayout(AuthenticationContext authContext) {
        this.queryService = Registry.getBean(StockSavedQueryService.class);
        this.authContext = authContext;
        
        //StockView.class.addListener(this);
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        String userName = "None";
        if(authContext.isAuthenticated()){
            userName = authContext.getAuthenticatedUser(UserDetails.class).get().getUsername();
        }
        
        Avatar avatar = new Avatar(userName);
        avatar.addClassNames(LumoUtility.Margin.Horizontal.SMALL);
        avatar.setTooltipEnabled(true);
        avatar.addClassNames(LumoUtility.Position.ABSOLUTE, LumoUtility.Position.End.XSMALL, LumoUtility.Position.Top.XSMALL);

        UserDialog userDialog = new UserDialog(authContext);
        userDialog.setTarget(avatar);
        
        addToNavbar(true, toggle, viewTitle, avatar);
    }

    private void addDrawerContent() {
        Span appName = new Span("Farm Tracks");
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(nav);
        createNavigation();
        addToDrawer(header, scroller, createFooter());
    }

    private void createNavigation() {
        System.out.println("MainLayout: createNavigation");
        nav.removeAll();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        //TODO build nav items for each stock query from database
        List<StockSavedQuery> stockQueryList = queryService.getSavedQueryList();
        for(StockSavedQuery query: stockQueryList){
            SideNavItem sn = new SideNavItem(query.getSavedQueryName(), StockView.class, query.getId().toString());
            sn.setPrefixComponent(VaadinIcon.LIST.create());
            sn.getElement().addEventListener("click", click -> {
                viewTitle.setText(query.getSavedQueryName());
            });
            nav.addItem(sn);
        }
        
    }
    
    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        System.out.println("MainLayout: afterNavigation");
        viewTitle.setText(getCurrentPageTitle());
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

    
    
}
