/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views;

import ca.jusjoken.component.Layout;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 *
 * @author birch
 */
@PageTitle("Welcome to Farm Tracks")
@Route(value = "home", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RouteAlias(value = "welcome", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.TH_LIST_SOLID)
@AnonymousAllowed
public class WelcomeView extends Layout {
    
    public WelcomeView() {
        //String version = env.getProperty("DM_APPLICATION_RELEASE_VERSION");
        //String header = "Welcome to DeliverMore Admin application (v" + version + ")";
        String header = "Welcome to Farm Tracks";

        Text welcomeMessage = new Text(header);

        addClassNames(LumoUtility.Margin.AUTO);

        add(welcomeMessage);
    }
}
