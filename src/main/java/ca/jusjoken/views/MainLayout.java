package ca.jusjoken.views;

import ca.jusjoken.component.UserDialog;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {
    
    private final transient AuthenticationContext authContext;

    private H1 viewTitle;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;
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

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
