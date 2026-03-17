package ca.jusjoken.views.utility;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.data.entity.AppUser;
import ca.jusjoken.data.entity.AppUserRole;
import ca.jusjoken.data.service.AppUserService;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User Management")
@RolesAllowed("ADMIN")
public class UserManagementView extends Div {

    private final AppUserService appUserService;

    private final Grid<AppUser> grid = new Grid<>(AppUser.class, false);

    // Form fields
    private final TextField usernameField = new TextField("Username");
        private final TextField displayNameField = new TextField("Display Name");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Checkbox enabledCheckbox = new Checkbox("Enabled");
    private final CheckboxGroup<AppUserRole> rolesGroup = new CheckboxGroup<>("Roles");

    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");
    private final Button cancelButton = new Button("Cancel");

    private AppUser currentUser = null;
    private boolean isNew = false;

    public UserManagementView(AppUserService appUserService) {
        this.appUserService = appUserService;
        setSizeFull();

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        splitLayout.addToPrimary(createGridLayout());
        splitLayout.addToSecondary(createFormLayout());

        add(splitLayout);
        refreshGrid();
        clearForm();
    }

    private VerticalLayout createGridLayout() {
        Button addButton = new Button("New User", e -> openNewUser());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(AppUser::getUsername).setHeader("Username").setSortable(true).setAutoWidth(true);
        grid.addColumn(u -> u.getEnabled() ? "Yes" : "No").setHeader("Enabled").setAutoWidth(true);
        grid.addColumn(u -> {
            Set<AppUserRole> roles = u.getRoles();
            if (roles == null || roles.isEmpty()) return "";
            return roles.stream().map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("");
        }).setHeader("Roles").setAutoWidth(true);

        grid.asSingleSelect().addValueChangeListener(event -> {
            AppUser selected = event.getValue();
            if (selected != null) {
                populateForm(selected);
            } else {
                clearForm();
            }
        });

        VerticalLayout layout = new VerticalLayout(addButton, grid);
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        return layout;
    }

    private VerticalLayout createFormLayout() {
        H3 formTitle = new H3("User Details");

        rolesGroup.setItems(AppUserRole.values());
        rolesGroup.setItemLabelGenerator(Enum::name);

        passwordField.setHelperText("Leave blank to keep existing password");
        usernameField.setRequired(true);
        usernameField.setMinLength(1);
            displayNameField.setPlaceholder("Full name shown in menus");

            FormLayout formLayout = new FormLayout(usernameField, displayNameField, passwordField, enabledCheckbox, rolesGroup);
        formLayout.setColspan(rolesGroup, 2);
        formLayout.setColspan(enabledCheckbox, 2);
            formLayout.setColspan(displayNameField, 2);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickShortcut(Key.ENTER);
        saveButton.addClickListener(e -> saveUser());

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> confirmDelete());

        cancelButton.addClickListener(e -> {
            clearForm();
            grid.asSingleSelect().clear();
        });

        HorizontalLayout buttons = new HorizontalLayout(saveButton, deleteButton, cancelButton);
        buttons.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(formTitle, formLayout, buttons);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidth("100%");
        return layout;
    }

    private void openNewUser() {
        grid.asSingleSelect().clear();
        currentUser = new AppUser();
        isNew = true;
        usernameField.setValue("");
            displayNameField.setValue("");
        passwordField.setValue("");
        passwordField.setRequired(true);
        passwordField.setHelperText("Required for new users");
        enabledCheckbox.setValue(true);
        rolesGroup.setValue(Set.of(AppUserRole.USER));
        deleteButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        usernameField.focus();
    }

    private void populateForm(AppUser user) {
        currentUser = user;
        isNew = false;
        usernameField.setValue(user.getUsername() != null ? user.getUsername() : "");
            displayNameField.setValue(user.getDisplayName() != null ? user.getDisplayName() : "");
        passwordField.setValue("");
        passwordField.setRequired(false);
        passwordField.setHelperText("Leave blank to keep existing password");
        enabledCheckbox.setValue(Boolean.TRUE.equals(user.getEnabled()));
        rolesGroup.setValue(user.getRoles() != null ? new HashSet<>(user.getRoles()) : new HashSet<>());
        deleteButton.setEnabled(true);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    private void clearForm() {
        currentUser = null;
        isNew = false;
        usernameField.setValue("");
            displayNameField.setValue("");
        passwordField.setValue("");
        passwordField.setRequired(false);
        passwordField.setHelperText("Leave blank to keep existing password");
        enabledCheckbox.setValue(true);
        rolesGroup.setValue(new HashSet<>());
        deleteButton.setEnabled(false);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }

    private void saveUser() {
        if (currentUser == null) return;

        String username = usernameField.getValue().trim();
        if (username.isBlank()) {
            showError("Username is required.");
            return;
        }

        String password = passwordField.getValue();
        if (isNew && password.isBlank()) {
            showError("Password is required for new users.");
            return;
        }

        Integer excludeId = isNew ? null : currentUser.getId();
        if (appUserService.usernameExists(username, excludeId)) {
            showError("Username '" + username + "' is already in use.");
            return;
        }

        currentUser.setUsername(username);
            currentUser.setDisplayName(displayNameField.getValue().trim());
        currentUser.setEnabled(enabledCheckbox.getValue());
        currentUser.setRoles(new HashSet<>(rolesGroup.getValue()));

        appUserService.save(currentUser, password);

        showSuccess(isNew ? "User created." : "User saved.");
        refreshGrid();
        clearForm();
        grid.asSingleSelect().clear();
    }

    private void confirmDelete() {
        if (currentUser == null || currentUser.getId() == null) return;

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete user");
        dialog.setText("Are you sure you want to delete user '" + currentUser.getUsername() + "'?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> deleteUser());
        dialog.open();
    }

    private void deleteUser() {
        if (currentUser == null) return;
        appUserService.delete(currentUser);
        showSuccess("User deleted.");
        refreshGrid();
        clearForm();
        grid.asSingleSelect().clear();
    }

    private void refreshGrid() {
        grid.setItems(appUserService.findAll());
    }

    private void showSuccess(String message) {
        Notification n = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification n = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
