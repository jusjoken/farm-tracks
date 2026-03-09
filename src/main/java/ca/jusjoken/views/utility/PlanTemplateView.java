package ca.jusjoken.views.utility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.masterdetaillayout.MasterDetailLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.MinWidth;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.PlanTemplate;
import ca.jusjoken.data.entity.PlanTemplateTask;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.ListRefreshNeededEvent;
import ca.jusjoken.data.service.PlanTemplateService;
import ca.jusjoken.data.service.PlanTemplateTaskService;
import ca.jusjoken.data.service.StockTypeService;
import ca.jusjoken.utility.TaskType;
import ca.jusjoken.views.MainLayout;
import jakarta.annotation.security.PermitAll;

@Route(value = "plan_template", layout = MainLayout.class)
@PermitAll
@PageTitle("Plan Templates")
public class PlanTemplateView extends MasterDetailLayout{

    private PlanTemplate planTemplate;

    private final Button detailsOkButton = new Button("Save");
    private final Button detailsCloseButton = new Button(new Icon("lumo", "cross"));
    private final Button addTemplateButton = new Button("Add Template");

    private final ComboBox<Utility.TaskLinkType> templateLinkType = new ComboBox<>();
    private final TextField templateName = new TextField("Template Name");
    private final Select<StockType> templateStockTypeChoice = new Select<>();

    private final Select<TaskType> templateTaskTypeSelect = new Select<>();
    private final TextField taskCustomNamField = new TextField();
    private final NumberField taskDaysFromStart = new NumberField();
    private final NumberField taskSequence = new NumberField();

    private final Grid<PlanTemplateTask> taskGrid = new Grid<>(PlanTemplateTask.class, false);

    private final List<PlanTemplateTask> taskBuffer = new ArrayList<>();


    private final PlanTemplateTaskService planTemplateTaskService;
    private final PlanTemplateService planTemplateService;
    private final StockTypeService stockTypeService;

    private final Grid<PlanTemplate> templateGrid = new Grid<>(PlanTemplate.class, false);;

    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    public PlanTemplateView(PlanTemplateTaskService planTemplateTaskService, PlanTemplateService planTemplateService, StockTypeService stockTypeService) {
        this.planTemplateTaskService = planTemplateTaskService;
        this.planTemplateService = planTemplateService;
        this.stockTypeService = stockTypeService;
        setMaster(createMasterLayout());
        configureDetailLayout();
        setDetail(createDetailLayout());
    }

    private VerticalLayout createMasterLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.addToEnd(addTemplateButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        buttonLayout.setMargin(false);
        layout.add(buttonLayout);
        addTemplateButton.addClickListener(e -> {
            planTemplate = new PlanTemplate();
            planTemplate.setType(Utility.TaskLinkType.GENERAL);
            planTemplate.setStockType(stockTypeService.findRabbits());
            taskBuffer.clear();
            refreshTaskGrid();
            setDetail(createDetailLayout());
        });

        templateGrid.setEmptyStateText("No plan templates available.  Use Add Template to create one.");

        templateGrid.addSelectionListener(listener -> {
            if(listener.getFirstSelectedItem().isPresent()){
                planTemplate = listener.getFirstSelectedItem().get();
                System.out.println("Selected template: " + planTemplate);
                //templateName.setValue(planTemplate.getName());
                //templateLinkType.setValue(planTemplate.getType());
                //templateStockTypeChoice.setValue(planTemplate.getStockType());
                taskBuffer.clear();
                taskBuffer.addAll(planTemplateTaskService.findAllByPlanTemplateId(planTemplate.getId()));
                refreshTaskGrid();
                setDetail(createDetailLayout());
            }else{
                planTemplate = null;
                templateName.setValue("");
                templateLinkType.clear();
                templateStockTypeChoice.clear();
                taskBuffer.clear();
                refreshTaskGrid();
                setDetail(null);
            }
        });

        //add a column with an icon to allow deleting a template from the grid
        templateGrid.addComponentColumn(template -> {
            Icon deleteIcon = new Icon("lumo", "cross");
            deleteIcon.setColor("red");
            deleteIcon.setTooltipText("Delete template");
            deleteIcon.addClickListener(e -> {
                //delete all associated tasks as well
                List<PlanTemplateTask> tasksToDelete = planTemplateTaskService.findAllByPlanTemplateId(template.getId());
                for(PlanTemplateTask task : tasksToDelete){
                    planTemplateTaskService.deleteById(task.getId());
                }
                planTemplateService.deleteById(template.getId());
                refreshTemplateGrid();
                setDetail(null);
            });
            return deleteIcon;
        }).setWidth("60px").setFlexGrow(0).setFrozen(true);
        templateGrid.addColumn(PlanTemplate::getName).setHeader("Template Name").setAutoWidth(true);
        templateGrid.addColumn(type -> {
            if(type.getType() != null){
                return type.getType().getShortName();
            }else{
                return "";
            }
        }).setHeader("Task Link Type").setAutoWidth(true);
        templateGrid.addColumn(template -> template.getStockType() != null ? template.getStockType().getName() : "").setHeader("Stock Type").setAutoWidth(true);
        //add a colum that is a count of the number of tasks in the template
        templateGrid.addColumn(template -> planTemplateTaskService.findAllByPlanTemplateId(template.getId()).size()).setHeader("Tasks").setAutoWidth(true);
        refreshTemplateGrid();
        layout.add(templateGrid);
        return layout;
    }

    private void refreshTemplateGrid() {
        var dataProvider = planTemplateService.findAll();
        templateGrid.setItems(dataProvider);
        fireListRefreshNeeded();
    }

    private void configureDetailLayout() {
        detailsCloseButton.getStyle().set("margin-left", "auto");
        detailsCloseButton.addClickListener(e -> closeDetails());
        detailsOkButton.addClickListener(e -> saveDetails());
        templateName.addValueChangeListener(listener ->{
            updateSaveEnabled();
        });
        taskGrid.addThemeVariants(GridVariant.LUMO_COMPACT,GridVariant.LUMO_ROW_STRIPES,GridVariant.LUMO_NO_BORDER);

        taskGrid.getEditor().addCloseListener(event ->{
            refreshTaskGrid();
        });
        templateTaskTypeSelect.addValueChangeListener(e -> {

        });

    }

    private VerticalLayout createDetailLayout() {
        if(planTemplate == null){
            return null;
        }
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.removeAll();

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.addToStart(detailsOkButton);
        buttonLayout.addToEnd(detailsCloseButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        buttonLayout.setMargin(false);

        layout.add(buttonLayout);

        templateName.setWidthFull();
        templateLinkType.setWidthFull();
        templateStockTypeChoice.setWidthFull();

        templateName.setRequired(true);

        templateLinkType.setLabel("Task link type");
        templateLinkType.setItems(Utility.TaskLinkType.values());
        templateLinkType.setItemLabelGenerator(Utility.TaskLinkType::getShortName);
        templateLinkType.setRequired(true);
        templateLinkType.setWidthFull();

        taskCustomNamField.setWidthFull();

        templateStockTypeChoice.setAriaLabel("Stock type");
        templateStockTypeChoice.setLabel("Stock type");
        templateStockTypeChoice.addClassNames(MinWidth.NONE, Padding.Top.MEDIUM);
        templateStockTypeChoice.setItemLabelGenerator(StockType::getName);
        templateStockTypeChoice.setItems(stockTypeService.findAllStockTypes());
        templateStockTypeChoice.setRequiredIndicatorVisible(true);

        //templateTaskTypeSelect.setLabel("Task type");
        templateTaskTypeSelect.setItems(TaskType.values());
        templateTaskTypeSelect.setItemLabelGenerator(TaskType::getShortName);
        templateTaskTypeSelect.setWidthFull();

        taskSequence.setStepButtonsVisible(true);
        taskSequence.setMin(1);
        taskSequence.setWidthFull();
        taskDaysFromStart.setStepButtonsVisible(true);
        taskDaysFromStart.setWidthFull();
        taskDaysFromStart.setMin(0);

        Binder<PlanTemplateTask> taskBinder = new Binder<>(PlanTemplateTask.class);
        taskBinder.forField(templateTaskTypeSelect)
                .asRequired("Task type is required")
                .bind(PlanTemplateTask::getType, PlanTemplateTask::setType);
        taskBinder.forField(taskCustomNamField)
                .bind(PlanTemplateTask::getCustomName, PlanTemplateTask::setCustomName);
        taskBinder.forField(taskDaysFromStart)
                .withConverter(Double::intValue, Integer::doubleValue, "Must be an integer")
                .bind(PlanTemplateTask::getDaysFromStart, PlanTemplateTask::setDaysFromStart);
        taskBinder.forField(taskSequence)
                .withConverter(Double::intValue, Integer::doubleValue, "Must be an integer")
                .bind(PlanTemplateTask::getSequence, PlanTemplateTask::setSequence);        
        Editor<PlanTemplateTask> taskEditor = taskGrid.getEditor();
        taskEditor.setBuffered(false);
        taskEditor.setBinder(taskBinder);

        taskGrid.removeAllColumns();
        taskGrid.setEmptyStateText("No tasks added.  Use Add Task to add one to this plan template.");

        taskGrid.addComponentColumn(item -> {
            Icon editIcon = new Icon("lumo", "edit");
            editIcon.setTooltipText("Edit task");
            editIcon.addClickListener(task -> {
                taskEditor.editItem(item);
                templateTaskTypeSelect.focus();
            });
            return editIcon;
        }).setWidth("50px").setFlexGrow(0).setFrozen(true);
        taskGrid.addComponentColumn(item -> {
            Icon detailsDeleteIcon = new Icon("lumo", "cross");
            detailsDeleteIcon.setTooltipText("Delete task");
            detailsDeleteIcon.setColor("red");
            detailsDeleteIcon.addClickListener(e -> {
                taskBuffer.remove(item);
                refreshTaskGrid();
            });
            return detailsDeleteIcon;
        }).setWidth("50px").setFlexGrow(0).setFrozen(true);

        taskGrid.addColumn(task -> task.getType().getShortName()).setHeader("Type").setAutoWidth(true).setEditorComponent(templateTaskTypeSelect);
        taskGrid.addColumn(PlanTemplateTask::getDisplayName).setHeader("Name").setAutoWidth(true).setEditorComponent(taskCustomNamField);
        taskGrid.addColumn(task -> task.getDaysFromStart()).setHeader("Days From Start").setFlexGrow(1).setEditorComponent(taskDaysFromStart);
        taskGrid.addColumn(task -> task.getSequence()).setHeader("Sequence").setFlexGrow(1).setEditorComponent(taskSequence);
        taskGrid.setWidthFull();
        taskGrid.setHeight("220px");

        FormLayout templateForm = new FormLayout();
        templateForm.add(templateName, templateLinkType, templateStockTypeChoice);

        FormLayout taskForm = new FormLayout();

        Button addTaskButton = new Button("Add Task");
        HorizontalLayout actions = new HorizontalLayout(addTaskButton);

        VerticalLayout content = new VerticalLayout();

        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        content.add(templateForm, taskForm, actions, taskGrid);

        layout.add(content);
        addTaskButton.addClickListener(e -> {
            addTaskToBuffer();
        });

        if(planTemplate.getName() != null){
            templateName.setValue(planTemplate.getName());
            templateName.setHelperText("Change the name to create a new copy");
        }else{
            templateName.setValue("");
            templateName.setHelperText("");
        }
        if(planTemplate.getType() != null){
            templateLinkType.setValue(planTemplate.getType());
        }
        if(planTemplate.getStockType() != null){
            templateStockTypeChoice.setValue(planTemplate.getStockType());
        }

        return layout;
    }

    private void refreshTaskGrid() {
        taskBuffer.sort(Comparator.comparingInt(this::taskSequenceOrMax));
        taskGrid.setItems(taskBuffer);
        taskGrid.getDataProvider().refreshAll();
        updateSaveEnabled();
    }

    private int taskSequenceOrMax(PlanTemplateTask task) {
        // nulls last
        if (task == null || task.getSequence() == null) {
            return Integer.MAX_VALUE;
        }
        return task.getSequence();
    }

    private void updateSaveEnabled() {
        boolean valid = templateName.getValue() != null && !templateName.getValue().trim().isEmpty() && templateLinkType.getValue() != null && templateStockTypeChoice.getValue() != null && !taskBuffer.isEmpty();
        detailsOkButton.setEnabled(valid);
    }

    private void addTaskToBuffer() {
        PlanTemplateTask newTask = new PlanTemplateTask();
        newTask.setType(TaskType.values()[0]);
        newTask.setDaysFromStart(0);
        newTask.setSequence(taskBuffer.size() + 1);
        taskBuffer.add(newTask);
        refreshTaskGrid();
    }

    private void saveDetails() {
        if(planTemplate == null){
            return;
        }
        String originalName = planTemplate.getName();
        if(templateName.getValue().equals(originalName)){
            // Name hasn't changed so save changes to existing template
            planTemplate.setName(templateName.getValue());
            planTemplate.setType(templateLinkType.getValue());
            planTemplate.setStockType(templateStockTypeChoice.getValue());

            PlanTemplate savedTemplate = planTemplateService.save(planTemplate);
            //to handle deleted tasks, we will delete all existing tasks and re-add from the buffer.  This is simpler than trying to determine which tasks were deleted vs added/edited.
            List<PlanTemplateTask> existingTasks = planTemplateTaskService.findAllByPlanTemplateId(savedTemplate.getId());
            for(PlanTemplateTask task : existingTasks){
                planTemplateTaskService.deleteById(task.getId());
            }
            for (PlanTemplateTask task : taskBuffer) {
                PlanTemplateTask newTask = new PlanTemplateTask();
                newTask.setType(task.getType());
                newTask.setDaysFromStart(task.getDaysFromStart());
                newTask.setSequence(task.getSequence());
                newTask.setPlanTemplate(savedTemplate);
                newTask.setCustomName(task.getCustomName());
                planTemplateTaskService.save(newTask); 
            }
        }else{
            //name has changed so save as a new template
            PlanTemplate newTemplate = new PlanTemplate();
            newTemplate.setName(templateName.getValue());
            newTemplate.setType(templateLinkType.getValue());
            newTemplate.setStockType(templateStockTypeChoice.getValue());
            PlanTemplate savedTemplate = planTemplateService.save(newTemplate);
            List<PlanTemplateTask> existingTasks = planTemplateTaskService.findAllByPlanTemplateId(savedTemplate.getId());
            for(PlanTemplateTask task : existingTasks){
                planTemplateTaskService.deleteById(task.getId());
            }
            for (PlanTemplateTask task : taskBuffer) {
                //create a copy of the task for the new template
                PlanTemplateTask newTask = new PlanTemplateTask();
                newTask.setType(task.getType());
                newTask.setDaysFromStart(task.getDaysFromStart());
                newTask.setSequence(task.getSequence());
                newTask.setPlanTemplate(savedTemplate);
                newTask.setCustomName(task.getCustomName());
                planTemplateTaskService.save(newTask); 
            }
        }
        refreshTemplateGrid();

        closeDetails();
    }

    private void closeDetails() {
        setDetail(null);
    }

    public void addListener(ListRefreshNeededListener listener){
        listRefreshNeededListeners.add(listener);
    }

    private void notifyRefreshNeeded(){
        for (ListRefreshNeededListener listener: listRefreshNeededListeners) {
            listener.listRefreshNeeded();
        }
    }

    public Registration addListRefreshNeededListener(
            ComponentEventListener<ListRefreshNeededEvent> listener) {
        return addListener(ListRefreshNeededEvent.class, listener);
    }

    private void fireListRefreshNeeded() {
        fireEvent(new ListRefreshNeededEvent(this, false));
    }

}
