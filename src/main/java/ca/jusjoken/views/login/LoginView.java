/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.login;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 *
 * @author birch
 */
@Route(value = "login", autoLayout = false) 
@PageTitle("Login")
@AnonymousAllowed 
public class LoginView extends Main implements BeforeEnterObserver {

    private final LoginForm login;

    public LoginView() {
        //addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER,LumoUtility.AlignItems.CENTER);
        login = new LoginForm();
        login.setAction("login"); 
        
        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.add(login);
        layout.setSizeFull();
        add(layout);
        setSizeFull();
       
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                 .getQueryParameters()
                 .getParameters()
                 .containsKey("error")) {
            login.setError(true); 
        }
    }
}