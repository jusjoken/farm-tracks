package ca.jusjoken.views.utility;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.data.entity.AppSettings;
import ca.jusjoken.data.service.AppSettingsService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/app-settings", layout = MainLayout.class)
@PageTitle("App Settings")
@RolesAllowed("ADMIN")
public class AppSettingsView extends Div {

    private final AppSettingsService appSettingsService;
    private final Binder<AppSettings> binder = new Binder<>(AppSettings.class);

    private final TextField farmName = new TextField("Farm Name");
    private final TextField farmAddressLine1 = new TextField("Farm Address Line 1");
    private final TextField farmAddressLine2 = new TextField("Farm Address Line 2");
    private final TextField farmEmail = new TextField("Farm Email");
    private final TextField farmPrefix = new TextField("Farm Prefix");
    private final TextField defaultLitterPrefix = new TextField("Default Litter Prefix");
    private final TextField overrideNextLitterNumber = new TextField("Override Next Litter Number");

    private final Button saveButton = new Button("Save");
    private final Button reloadButton = new Button("Reload");

    private AppSettings appSettings;

    public AppSettingsView(AppSettingsService appSettingsService) {
        this.appSettingsService = appSettingsService;
        setSizeFull();
        addClassName("app-settings-view");

        configureForm();
        add(buildContent());
        load();
    }

    private void configureForm() {
        farmName.setWidthFull();
        farmAddressLine1.setWidthFull();
        farmAddressLine2.setWidthFull();
        farmEmail.setWidthFull();
        farmPrefix.setWidthFull();
        defaultLitterPrefix.setWidthFull();
        overrideNextLitterNumber.setWidthFull();
        binder.forField(farmName)
                .asRequired("Farm name is required")
                .bind(AppSettings::getFarmName, AppSettings::setFarmName);

        binder.forField(farmAddressLine1)
                .asRequired("Address line 1 is required")
                .bind(AppSettings::getFarmAddressLine1, AppSettings::setFarmAddressLine1);

        binder.forField(farmAddressLine2)
                .bind(AppSettings::getFarmAddressLine2, AppSettings::setFarmAddressLine2);

        binder.forField(farmEmail)
                .withValidator(new EmailValidator("Enter a valid email address"))
                .bind(AppSettings::getFarmEmail, AppSettings::setFarmEmail);

        binder.forField(farmPrefix)
                .asRequired("Farm prefix is required")
                .bind(AppSettings::getFarmPrefix, AppSettings::setFarmPrefix);

        binder.forField(defaultLitterPrefix)
                .asRequired("Default litter prefix is required")
                .bind(AppSettings::getDefaultLitterPrefix, AppSettings::setDefaultLitterPrefix);

        binder.forField(overrideNextLitterNumber)
                .bind(AppSettings::getOverrideNextLitterNumber, AppSettings::setOverrideNextLitterNumber);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(click -> save());

        reloadButton.addClickListener(click -> load());
    }

    private VerticalLayout buildContent() {
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.add(farmName, farmAddressLine1, farmAddressLine2, farmEmail, farmPrefix, defaultLitterPrefix, overrideNextLitterNumber);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("48em", 2));

        HorizontalLayout actions = new HorizontalLayout(saveButton, reloadButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(formLayout, actions);
        layout.setWidthFull();
        layout.setMaxWidth("72rem");
        layout.setPadding(true);
        layout.setSpacing(true);
        return layout;
    }

    private void load() {
        appSettings = appSettingsService.getAppSettings();
        binder.readBean(appSettings);
    }

    private void save() {
        if (appSettings == null) {
            appSettings = appSettingsService.getAppSettings();
        }

        try {
            binder.writeBean(appSettings);
            appSettings = appSettingsService.save(appSettings);
            showSuccess("App settings saved.");
        } catch (ValidationException validationException) {
            showError("Please fix validation errors before saving.");
        }
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
