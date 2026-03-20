/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.login;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import ca.jusjoken.services.AppVersionService;

/**
 *
 * @author birch
 */
@Route(value = "login", autoLayout = false) 
@PageTitle("Login")
@AnonymousAllowed 
public class LoginView extends Main implements BeforeEnterObserver {

    private final LoginForm login;
    private final AppVersionService appVersionService;

    public LoginView(AppVersionService appVersionService) {
        this.appVersionService = appVersionService;
        //addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER,LumoUtility.AlignItems.CENTER);
        login = new LoginForm();
        login.setAction("login");
        login.addForgotPasswordListener(e ->
            Notification.show("Please contact your administrator to reset your password.",
                    6000, Notification.Position.MIDDLE));

        Span version = new Span("v" + this.appVersionService.getDisplayVersion());
        version.addClassName("login-version");

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.add(login, version);
        layout.setSizeFull();
        add(layout);
        setSizeFull();
       
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Unauthenticated users do not have saved UI settings yet; follow system preference,
        // and keep following it if the OS theme changes while login is open.
        attachEvent.getUI().getPage().executeJs(
            "const root = document.documentElement;"
            + "const attr = 'theme';"
            + "const apply = (isDark) => {"
            + "  const tokens = (root.getAttribute(attr) || '')"
            + "    .split(/\\s+/)"
            + "    .filter(Boolean)"
            + "    .filter(token => token !== 'dark');"
            + "  if (isDark) { tokens.push('dark'); }"
            + "  if (tokens.length) { root.setAttribute(attr, tokens.join(' ')); }"
            + "  else { root.removeAttribute(attr); }"
            + "};"
            + "if (window.__farmTracksLoginMql && window.__farmTracksLoginThemeHandler) {"
            + "  window.__farmTracksLoginMql.removeEventListener('change', window.__farmTracksLoginThemeHandler);"
            + "}"
            + "const mql = window.matchMedia('(prefers-color-scheme: dark)');"
            + "const handler = (event) => apply(event.matches);"
            + "apply(mql.matches);"
            + "mql.addEventListener('change', handler);"
            + "window.__farmTracksLoginMql = mql;"
            + "window.__farmTracksLoginThemeHandler = handler;"
        );
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