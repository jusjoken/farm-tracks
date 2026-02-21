package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.TaskService;

public class TaskEditor {

    public enum DialogMode {
        EDIT, CREATE
    }

    private DialogMode dialogMode = DialogMode.CREATE;

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(TaskEditor.class);

    private final Dialog dialog = new Dialog();
    private Task task;
    private String dialogTitle = "";

    private final Button dialogOkButton = new Button("OK");
    private final Button dialogCancelButton = new Button("Cancel");
    private final Button dialogCloseButton = new Button(new Icon("lumo", "cross"));

    private final ComboBox<Utility.TaskType> type = new ComboBox<>();
    private final TextField name = new TextField();
    private final DatePicker date = new DatePicker();
    private final ComboBox<Utility.TaskLinkType> linkType = new ComboBox<>();
    private final Select<Litter> linkLitter = new Select<>();
    private final Select<Stock> linkBreeder = new Select<>();
    private final Checkbox completed = new Checkbox("Completed");

    private final VerticalLayout dialogLayout = new VerticalLayout();

    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private final TaskService taskService;
    private final LitterService litterService;
    private final StockService stockService;

    private StockType currentStockType;   

    public TaskEditor() {
        taskService = Registry.getBean(TaskService.class);
        litterService = Registry.getBean(LitterService.class);
        stockService = Registry.getBean(StockService.class);
        dialogConfigure();
    }

    private void dialogConfigure() {
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "360px").set("max-width", "100%");

        dialog.add(dialogLayout);
        dialogCloseButton.addClickListener((e) -> dialogClose());
        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.setCloseOnEsc(true);
        dialogCancelButton.addClickListener((e) -> dialogClose());
        dialogCancelButton.setAutofocus(true);
        dialogCancelButton.setEnabled(true);

        dialogOkButton.addClickListener(event -> dialogSave());
        dialogOkButton.setEnabled(false);
        dialogOkButton.setDisableOnClick(true);
        dialogOkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(dialogCancelButton, dialogOkButton);

        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        type.setLabel("Task type");
        type.setItems(Utility.TaskType.values());
        type.setItemLabelGenerator(Utility.TaskType::getShortName);
        type.setRequired(true);
        type.setWidthFull();

        name.setLabel("Name");
        name.setRequired(true);
        name.setWidthFull();

        date.setLabel("Due Date");
        date.setRequired(true);
        date.setWidthFull();

        linkType.setLabel("Task link type");
        linkType.setItems(Utility.TaskLinkType.values());
        linkType.setItemLabelGenerator(Utility.TaskLinkType::getShortName);
        linkType.setRequired(true);
        linkType.setWidthFull();

        linkBreeder.setLabel("Breeder");
        linkBreeder.setItemLabelGenerator(Stock::getDisplayName);
        linkBreeder.setWidthFull();

        linkLitter.setLabel("Litter");
        linkLitter.setItemLabelGenerator(Litter::getDisplayName);
        linkLitter.setWidthFull();

        completed.setWidthFull();

        type.addValueChangeListener(e -> updateSaveEnabled());
        name.addValueChangeListener(e -> updateSaveEnabled());
    }

    public void dialogOpen() {
        dialogOpen(new Task(), DialogMode.CREATE, null);
    }

    public void dialogOpen(Task taskEntity, DialogMode mode, StockType currentStockType) {
        this.task = taskEntity;
        this.dialogMode = mode;
        this.currentStockType = currentStockType;

        if (dialogMode.equals(DialogMode.CREATE)) {
            dialogTitle = "Create new task";
        } else {
            dialogTitle = "Edit task";
        }

        dialogLayout.removeAll();
        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);
        dialog.setDraggable(true);
        dialog.setResizable(true);

        VerticalLayout fieldsLayout = UIUtilities.getVerticalLayout(true, true, true);

        //hide/show the id fields based on the current value of the linktype field
        linkType.addValueChangeListener(event -> {
            changeLinkType(event.getValue());
        });

        type.setValue(task.getType());
        name.setValue(task.getName() == null ? "" : task.getName());
        date.setValue(task.getDate());
        changeLinkType(task.getLinkType());

        completed.setValue(Boolean.TRUE.equals(task.getCompleted()));

        fieldsLayout.add(type, name, date, linkType, linkBreeder, linkLitter, completed);
        dialogLayout.add(new Hr(), fieldsLayout);

        updateSaveEnabled();
        dialog.open();
    }

    private void changeLinkType(Utility.TaskLinkType newLinkType) {
        linkType.setValue(newLinkType);
        if (newLinkType == null) {
            linkBreeder.setVisible(false);
            linkBreeder.clear();
            linkLitter.setVisible(false);
            linkLitter.clear();
        } else switch (newLinkType) {
            case BREEDER -> {
                linkBreeder.setVisible(true);
                linkBreeder.setItems(stockService.findAllBreeders(currentStockType));
                if(task.getLinkBreederId()!=null){
                    Stock breeder = stockService.findById(task.getLinkBreederId());
                    linkBreeder.setValue(breeder);
                } else {
                    linkBreeder.setValue(null);
                }
                linkLitter.setVisible(false);
                linkLitter.clear();
            }
            case LITTER -> {
                linkBreeder.setVisible(false);
                linkBreeder.clear();
                linkLitter.setVisible(true);
                linkLitter.setItems(litterService.getActiveLitters(currentStockType));
                if(task.getLinkLitterId()!=null){
                    Litter litter = litterService.findById(task.getLinkLitterId());
                    linkLitter.setValue(litter);
                } else {    
                    linkLitter.setValue(null);
                }
            }
            default -> {
                linkBreeder.setVisible(false);
                linkBreeder.clear();
                linkLitter.setVisible(false);
                linkLitter.clear();
            }
        }
    }

    private void updateSaveEnabled() {
        boolean valid = type.getValue() != null
                && name.getValue() != null
                && !name.getValue().trim().isEmpty();
        dialogOkButton.setEnabled(valid);
    }

    private void dialogSave() {
        task.setType(type.getValue());
        task.setName(name.getValue() == null ? null : name.getValue().trim());
        task.setDate(date.getValue());
        task.setLinkType(linkType.getValue());
        if(linkType.getValue() == null){
            task.setLinkBreederId(null);
            task.setLinkLitterId(null);
        } else switch (linkType.getValue()) {
            case BREEDER -> {
                task.setLinkBreederId(linkBreeder.getValue() != null ? linkBreeder.getValue().getId() : null);
                task.setLinkLitterId(null);
            }
            case LITTER -> {
                task.setLinkBreederId(null);
                task.setLinkLitterId(linkLitter.getValue() != null ? linkLitter.getValue().getId() : null);
            }
            default -> {
                task.setLinkBreederId(null);
                task.setLinkLitterId(null);
            }
        }
        task.setCompleted(Boolean.TRUE.equals(completed.getValue()));

        taskService.save(task);
        notifyRefreshNeeded();
        dialogClose();
    }

    private void dialogClose() {
        dialog.close();
    }

    public void addListener(ListRefreshNeededListener listener) {
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded() {
        for (ListRefreshNeededListener listener : listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }
}
