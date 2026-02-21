package ca.jusjoken.views.stock;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.converter.StringToLongConverter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockTypeRepository;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;



@PageTitle("Stock Types")
@Route(value = "stock-types", layout = MainLayout.class)
@PermitAll
public class StockTypeView extends VerticalLayout {

    private final StockTypeRepository stockTypeRepository;

    private final Grid<StockType> grid = new Grid<>(StockType.class, false);
    private final Binder<StockType> binder = new BeanValidationBinder<>(StockType.class);

    private final FormLayout form = new FormLayout();
    private final TextField idField = new TextField("Id");

    private final Button newButton = new Button("New");
    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");
    private final Button cancelButton = new Button("Cancel");

    private StockType editing;
    private final List<PropertyDescriptor> properties = new ArrayList<>();

    private final MasterDetailLayout masterDetailLayout = new MasterDetailLayout();

    public StockTypeView(StockTypeRepository stockTypeRepository) {
        this.stockTypeRepository = stockTypeRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Stock Types"));

        discoverProperties();
        configureGrid();
        configureForm();
        configureActions();
        configureMasterDetailLayout(); // required for MasterDetailLayout wiring

        add(new HorizontalLayout(newButton), masterDetailLayout);

        refreshGrid();
        clearEditor();
    }

    private void discoverProperties() {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(StockType.class, Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if ("class".equals(pd.getName()) || pd.getReadMethod() == null) continue;
                properties.add(pd);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to inspect StockType properties", e);
        }
    }

    private void configureGrid() {
        for (PropertyDescriptor pd : properties) {
            if (isExcludedFromGrids(pd.getName())) continue;

            grid.addColumn(item -> readValue(item, pd))
                .setHeader(toLabel(pd.getName()))
                .setAutoWidth(true);
        }
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(event -> edit(event.getValue()));
    }

    private boolean isExcludedFromGrids(String propertyName) {
        return "genotypes".equals(propertyName) || "genotypeSegments".equals(propertyName);
    }

    private void configureForm() {
        idField.setReadOnly(true);
        form.add(idField);

        for (PropertyDescriptor pd : properties) {
            if ("id".equals(pd.getName())) continue; // shown read-only via idField
            if (pd.getWriteMethod() == null) continue; // view-only property

            HasValue<?, ?> field = createField(pd);
            bindField(pd, field);

            if (field instanceof com.vaadin.flow.component.Component c) {
                form.add(c);
            }
        }

    }

    private VerticalLayout buildEditorLayout() {
        VerticalLayout editor = new VerticalLayout();
        editor.setWidthFull();
        editor.setPadding(false);
        editor.setSpacing(true);

        form.setWidthFull();
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("600px", 2)
        );

        HorizontalLayout actions = new HorizontalLayout(saveButton, deleteButton, cancelButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        editor.add(form, actions);
        return editor;
    }

    private void configureActions() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        newButton.addClickListener(e -> edit(new StockType()));
        saveButton.addClickListener(e -> save());
        deleteButton.addClickListener(e -> delete());
        cancelButton.addClickListener(e -> clearEditor());
    }

    private void save() {
        if (editing == null) {
            editing = new StockType();
        }

        if (!binder.writeBeanIfValid(editing)) {
            Notification.show("Please fix validation errors.");
            return;
        }

        stockTypeRepository.save(editing);
        Notification.show("Stock type saved.");
        refreshGrid();
        clearEditor();
    }

    private void delete() {
        if (editing == null || editing.getId() == null) {
            return;
        }

        stockTypeRepository.delete(editing);
        Notification.show("Stock type deleted.");
        refreshGrid();
        clearEditor();
    }

    private void refreshGrid() {
        grid.setItems(stockTypeRepository.findAll());
        grid.getDataProvider().refreshAll();
    }

    private void clearEditor() {
        editing = null;
        idField.clear();
        binder.readBean(null);
        grid.deselectAll();
        deleteButton.setEnabled(false);
        saveButton.setEnabled(true);
        masterDetailLayout.setDetail(null);
    }

    private void edit(StockType stockType) {
        if (stockType == null) {
            clearEditor();
            return;
        }

        editing = stockType;
        idField.setValue(stockType.getId() == null ? "" : String.valueOf(stockType.getId()));
        binder.readBean(stockType);
        deleteButton.setEnabled(stockType.getId() != null);
        masterDetailLayout.setDetail(buildEditorLayout());
    }

    private PropertyDescriptor findProperty(String name) {
        for (PropertyDescriptor pd : properties) {
            if (name.equals(pd.getName())) {
                return pd;
            }
        }
        return null;
    }

    private Object readValueRaw(StockType item, PropertyDescriptor pd) {
        if (item == null || pd == null) {
            return null;
        }
        try {
            Method readMethod = pd.getReadMethod();
            if (readMethod == null) {
                return null;
            }
            if (!readMethod.canAccess(item)) {
                readMethod.setAccessible(true);
            }
            return readMethod.invoke(item);
        } catch (Exception e) {
            return null;
        }
    }

    private Object readValue(StockType item, PropertyDescriptor pd) {
        Object value = readValueRaw(item, pd);
        if (value == null) {
            return "";
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        return value;
    }

    private String toLabel(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String normalized = name.trim().replace('_', ' ');
        normalized = normalized.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        String[] parts = normalized.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private void configureMasterDetailLayout() {
        masterDetailLayout.setSizeFull();

        VerticalLayout master = new VerticalLayout(grid);
        master.setSizeFull();
        master.setPadding(false);
        master.setSpacing(false);

        VerticalLayout detail = buildEditorLayout();
        detail.setSizeFull();

        masterDetailLayout.setMaster(master);
        masterDetailLayout.setDetail(detail);
        masterDetailLayout.setDetail(null);
    }

    private HasValue<?, ?> createField(PropertyDescriptor pd) {
        Class<?> type = pd.getPropertyType();
        String label = toLabel(pd.getName());

        if (type == String.class) {
            TextField field = new TextField(label);
            field.setWidthFull();
            return field;
        }
        if (type == Integer.class || type == int.class) {
            IntegerField field = new IntegerField(label);
            field.setWidthFull();
            return field;
        }
        if (type == Boolean.class || type == boolean.class) {
            return new Checkbox(label);
        }
        if (type == LocalDate.class) {
            DatePicker field = new DatePicker(label);
            field.setWidthFull();
            return field;
        }
        if (type.isEnum()) {
            Select<Object> field = new Select<>();
            field.setLabel(label);
            field.setItems(type.getEnumConstants());
            field.setWidthFull();
            return field;
        }

        TextField fallback = new TextField(label);
        fallback.setWidthFull();
        return fallback;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void bindField(PropertyDescriptor pd, HasValue<?, ?> field) {
        final String property = pd.getName();
        final Class<?> type = pd.getPropertyType();

        if (type == String.class) {
            binder.forField((TextField) field).bind(property);
            return;
        }

        if (type == Integer.class || type == int.class) {
            binder.forField((IntegerField) field).bind(property);
            return;
        }

        if (type == Boolean.class || type == boolean.class) {
            binder.forField((Checkbox) field).bind(property);
            return;
        }

        if (type == LocalDate.class) {
            binder.forField((DatePicker) field).bind(property);
            return;
        }

        if (type == Long.class || type == long.class) {
            binder.forField((TextField) field)
                .withNullRepresentation("")
                .withConverter(new StringToLongConverter("Must be a whole number"))
                .bind(property);
            return;
        }

        if (type == Double.class || type == double.class) {
            binder.forField((TextField) field)
                .withNullRepresentation("")
                .withConverter(new StringToDoubleConverter("Must be a number"))
                .bind(property);
            return;
        }

        if (type == BigDecimal.class) {
            binder.forField((TextField) field)
                .withNullRepresentation("")
                .withConverter(
                    s -> (s == null || s.isBlank()) ? null : new BigDecimal(s.trim()),
                    v -> v == null ? "" : v.toPlainString(),
                    "Must be a decimal number"
                )
                .bind(property);
            return;
        }

        if (type.isEnum()) {
            binder.forField((Select) field).bind(property);
            return;
        }

        // Unsupported writable type: display only
        TextField tf = (TextField) field;
        tf.setReadOnly(true);
        binder.forField(tf).bind(
            bean -> {
                Object value = readValueRaw(bean, pd);
                return value == null ? "" : String.valueOf(value);
            },
            (bean, value) -> {
                // ...existing code...
            }
        );
    }
}