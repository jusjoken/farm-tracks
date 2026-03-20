/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import ca.jusjoken.component.Layout;

/**
 *
 * @author birch
 */
@PageTitle("Welcome to Farm Tracks")
@Route(value = "home", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RouteAlias(value = "welcome", layout = MainLayout.class)
@AnonymousAllowed
public class WelcomeView extends Layout {
    
    public WelcomeView() {
        Span welcomeMessage = new Span("Welcome to Farm Tracks");
        add(welcomeMessage);
    }
}
