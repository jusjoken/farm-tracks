package ca.jusjoken.component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.Utility.TaskLinkType;
import ca.jusjoken.data.entity.PlanTemplate;
import ca.jusjoken.data.entity.PlanTemplateTask;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.data.service.PlanTemplateService;
import ca.jusjoken.data.service.PlanTemplateTaskService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.TaskPlanService;
import ca.jusjoken.data.service.TaskService;

public class PlanEditor {

    public enum DialogMode {
        EDIT, CREATE
    }

    private DialogMode dialogMode = DialogMode.CREATE;

    private final Dialog dialog = new Dialog();
    private Stock stockEntity;
    private String dialogTitle = "";

    private final Button dialogOkButton = new Button("Save Plan");
    private final Button dialogCancelButton = new Button("Cancel");
    private final Button dialogCloseButton = new Button(new Icon("lumo", "cross"));
    private final Select<PlanTemplate> planTemplateSelect = new Select<>();
    private List<PlanTemplate> filteredPlanTemplates;
    private final Select<TaskLinkType> taskLinkTypeSelect = new Select<>();

    private final VerticalLayout dialogLayout = new VerticalLayout();
    private final List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();
    private final PlanTemplateService planTemplateService;
    private final PlanTemplateTaskService planTemplateTaskService;
    private final StockService stockService;
    private final Select<Stock> femaleStockSelect = new Select<>();
    private final Select<Stock> maleStockSelect = new Select<>();
    private final DatePicker startDatePicker = new DatePicker("Start Date");
    private PlanTemplate selectedPlanTemplate;
    private TaskLinkType selectedTaskLinkType;
    private TaskPlan editingTaskPlan;
    private final TaskService taskService;
    private final TaskPlanService taskPlanService;
    private final Grid<GeneratedTaskRow> taskGrid = new Grid<>(GeneratedTaskRow.class, false);
    private final List<GeneratedTaskRow> generatedTaskRows = new ArrayList<>();

    public PlanEditor() {
        this.planTemplateService = Registry.getBean(PlanTemplateService.class);
        this.planTemplateTaskService = Registry.getBean(PlanTemplateTaskService.class);
        this.stockService = Registry.getBean(StockService.class);
        this.taskService = Registry.getBean(TaskService.class);
        this.taskPlanService = Registry.getBean(TaskPlanService.class);
        dialogConfigure();
    }

    private void dialogConfigure() {
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "320px").set("max-width", "100%");
        dialog.add(dialogLayout);

        dialogCloseButton.addClickListener(e -> dialogClose());
        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.setCloseOnEsc(true);

        dialogCancelButton.addClickListener(e -> dialogClose());
        dialogCancelButton.setAutofocus(true);
        dialogCancelButton.setEnabled(true);

        dialogOkButton.addClickListener(e -> dialogSave());
        dialogOkButton.setEnabled(true);
        dialogOkButton.setDisableOnClick(true);
        dialogOkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(dialogCancelButton, dialogOkButton);

        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);

        dialog.getFooter().add(footerLayout);

        //configure fields
        taskLinkTypeSelect.setLabel("Plan Type");
        taskLinkTypeSelect.setItems(TaskLinkType.values());
        taskLinkTypeSelect.setWidthFull();
        taskLinkTypeSelect.addValueChangeListener(e -> {
            selectedTaskLinkType = e.getValue();
            filteredPlanTemplates = planTemplateService.findAllByTaskLinkType(selectedTaskLinkType);
            planTemplateSelect.setItems(filteredPlanTemplates);
            if(!filteredPlanTemplates.isEmpty()) {
                planTemplateSelect.setValue(filteredPlanTemplates.get(0));
            } else {
                planTemplateSelect.clear();
            }
            editingTaskPlan.setType(selectedTaskLinkType);
            refreshGeneratedTasks();
            valuesChanged();
        });

        planTemplateSelect.setLabel("Plan Template");
        planTemplateSelect.setWidthFull();
        planTemplateSelect.addValueChangeListener(e -> {
            if(e.getValue() != null) {
                selectedPlanTemplate = e.getValue();
            } else {
                selectedPlanTemplate = null;
            }
            refreshGeneratedTasks();
            valuesChanged();
        });
        femaleStockSelect.setWidthFull();
        maleStockSelect.setWidthFull();
        femaleStockSelect.addValueChangeListener(e -> {
            Stock selectedFemaleStock = e.getValue();
            editingTaskPlan.setLinkMotherId(selectedFemaleStock != null ? selectedFemaleStock.getId() : null);
            valuesChanged();
        });
        maleStockSelect.addValueChangeListener(e -> {
            Stock selectedMaleStock = e.getValue();
            editingTaskPlan.setLinkFatherId(selectedMaleStock != null ? selectedMaleStock.getId() : null);
            valuesChanged();
        });
        startDatePicker.setWidthFull();
        startDatePicker.setRequired(true);
        startDatePicker.addValueChangeListener(e -> {
            refreshGeneratedTasks();
            valuesChanged();
        });

        configureTaskGrid();
    }

    private void configureTaskGrid() {
        taskGrid.addColumn(GeneratedTaskRow::getName).setHeader("Task");
        taskGrid.addComponentColumn(row -> {
            DatePicker datePicker = new DatePicker();
            datePicker.setValue(row.getTaskDate());
            datePicker.addValueChangeListener(e -> row.setTaskDate(e.getValue()));
            return datePicker;
        }).setHeader("Date");
        taskGrid.setWidthFull();
        taskGrid.setAllRowsVisible(true);
    }

    private void refreshGeneratedTasks() {
        generatedTaskRows.clear();

        if (selectedPlanTemplate == null || startDatePicker.getValue() == null) {
            taskGrid.setItems(generatedTaskRows);
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        List<PlanTemplateTask> templateTasks = planTemplateTaskService.findAllByPlanTemplateId(selectedPlanTemplate.getId());

        for (PlanTemplateTask templateTask : templateTasks) {
            Integer daysFromStart = templateTask.getDaysFromStart() == null ? 0 : templateTask.getDaysFromStart();
            generatedTaskRows.add(new GeneratedTaskRow(
                templateTask.getDisplayName(),
                startDate.plusDays(daysFromStart),
                templateTask.getType()
            ));
        }

        taskGrid.setItems(generatedTaskRows);
    }

    public void dialogOpen(TaskLinkType taskLinkType) {
        dialogOpen(taskLinkType, null);
    }

    public void dialogOpen(TaskLinkType taskLinkType, Stock stockEntity) {
        dialogOpen(taskLinkType, stockEntity, DialogMode.CREATE);
    }

    public void dialogOpen(TaskLinkType taskLinkType, Stock stockEntity, DialogMode mode) {
        dialogOpen(null, taskLinkType, stockEntity, mode);
    }

    public void dialogOpen(Integer taskPlanId, TaskLinkType taskLinkType, Stock stockEntity, DialogMode mode) {
        System.out.println("Opening PlanEditor dialog with taskPlanId: " + taskPlanId + ", taskLinkType: " + taskLinkType + ", stockEntity: " + stockEntity + ", mode: " + mode);

        clearAllFields();

        this.stockEntity = stockEntity;
        this.dialogMode = mode;
        selectedTaskLinkType = taskLinkType;
        if(taskPlanId != null) {
            editingTaskPlan = taskPlanService.findById(taskPlanId).get();
        } else {
            editingTaskPlan = new TaskPlan();
        }
        filteredPlanTemplates = planTemplateService.findAllByTaskLinkType(selectedTaskLinkType);

        planTemplateSelect.setItems(filteredPlanTemplates != null ? filteredPlanTemplates : List.of());

        if(filteredPlanTemplates != null && !filteredPlanTemplates.isEmpty()) {
            selectedPlanTemplate = filteredPlanTemplates.get(0);
        } else {
            selectedPlanTemplate = null;
        }

        if (selectedPlanTemplate != null) {
            planTemplateSelect.setValue(selectedPlanTemplate);
        } else {
            planTemplateSelect.clear();
        }

        dialogTitle = dialogMode == DialogMode.CREATE ? "Create plan" : "Edit plan";

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);

        dialog.setDraggable(true);
        dialog.setResizable(true);

        //add a select field of tasklinktype to select the link type for this editor, and filter the plan templates based on the selected tasklinktype
        taskLinkTypeSelect.setItemLabelGenerator(TaskLinkType::getShortName);
        taskLinkTypeSelect.setValue(taskLinkType);

        planTemplateSelect.setItemLabelGenerator(PlanTemplate::getName);

        //only add the mother father fields for Breeder and Litter plans
        if((selectedTaskLinkType == TaskLinkType.BREEDER || selectedTaskLinkType == TaskLinkType.LITTER)
                && selectedPlanTemplate != null) {
            StockType stockType = selectedPlanTemplate.getStockType();
            femaleStockSelect.setLabel("Select "+ stockType.getFemaleName());
            maleStockSelect.setLabel("Select "+ stockType.getMaleName());

            //populate the mother father selects based on the stock type of the selected plan template
            List<Stock> femaleStocks = stockService.getMothers(null,stockType);
            List<Stock> maleStocks = stockService.getFathers(null,stockType);
            femaleStockSelect.setItems(femaleStocks);
            maleStockSelect.setItems(maleStocks);

            if(dialogMode == DialogMode.EDIT && editingTaskPlan.getId() != null) {
                //if we're editing an existing plan, set the selected mother and father based on the linkMotherId and linkFatherId of the task plan
                if(editingTaskPlan.getLinkMotherId() != null) {
                    Stock linkedMother = stockService.findById(editingTaskPlan.getLinkMotherId());
                    femaleStockSelect.setValue(linkedMother);
                }
                if(editingTaskPlan.getLinkFatherId() != null) {
                    Stock linkedFather = stockService.findById(editingTaskPlan.getLinkFatherId());
                    maleStockSelect.setValue(linkedFather);
                }
            }

            //if the passed in stockEntity is a mother or father, pre-select it in the appropriate select field
            if(stockEntity != null) {
                if(stockEntity.getSex().equals(Gender.FEMALE)) {
                    femaleStockSelect.setValue(stockEntity);
                } else if(stockEntity.getSex().equals(Gender.MALE)) {
                    maleStockSelect.setValue(stockEntity);
                }
            }
            //make these fields required if it's a breeder or litter plan
            femaleStockSelect.setRequiredIndicatorVisible(true);
            maleStockSelect.setRequiredIndicatorVisible(true);

        } else {
            femaleStockSelect.setRequiredIndicatorVisible(false);
            maleStockSelect.setRequiredIndicatorVisible(false);
        }

        femaleStockSelect.setItemLabelGenerator(Stock::getDisplayName);
        maleStockSelect.setItemLabelGenerator(Stock::getDisplayName);

        startDatePicker.setRequired(true);
        if(dialogMode == DialogMode.EDIT && editingTaskPlan.getId() != null) {
            //if we're editing an existing plan, set the start date to the earliest task date for the tasks in this plan
            List<Task> existingTasks = taskService.findByPlanId(editingTaskPlan.getId());
            LocalDate earliestDate = existingTasks.stream()
                    .map(Task::getDate)
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());
            startDatePicker.setValue(earliestDate);
        } else {
            startDatePicker.setValue(LocalDate.now());
    }

        refreshGeneratedTasks();
        buildDialogLayout();
        dialog.open();
    }

    private void clearAllFields() {
        //use this to ensure on open the dialog is set up properly and old values are gone
        selectedPlanTemplate = null;
        selectedTaskLinkType = null;
        femaleStockSelect.clear();
        maleStockSelect.clear();
        startDatePicker.clear();
        generatedTaskRows.clear();
        taskGrid.setItems(generatedTaskRows);
    }

    private void buildDialogLayout() {
        dialogLayout.removeAll();
        dialogLayout.add(taskLinkTypeSelect);
        if(filteredPlanTemplates.isEmpty()) {
            dialogLayout.add(new Span("No plan templates found. Please create a plan template first."));
        } else {
            dialogLayout.add(planTemplateSelect);
        }
        if(selectedTaskLinkType == TaskLinkType.BREEDER || selectedTaskLinkType == TaskLinkType.LITTER) {
            dialogLayout.add(femaleStockSelect);
            dialogLayout.add(maleStockSelect);
        }
        dialogLayout.add(startDatePicker);
        dialogLayout.add(new Span("The following tasks will be created:"));
        dialogLayout.add(taskGrid);
    }

    private void valuesChanged(){
        //check if all required fields are filled in to enable the save button
        
        boolean allFieldsFilled = selectedTaskLinkType != null && selectedPlanTemplate != null;
        if(selectedTaskLinkType == TaskLinkType.BREEDER || selectedTaskLinkType == TaskLinkType.LITTER) {
            allFieldsFilled = allFieldsFilled && femaleStockSelect.getValue() != null && maleStockSelect.getValue() != null;
        }   
        //include the startdate to ensure its filled in
        allFieldsFilled = allFieldsFilled && startDatePicker.getValue() != null;
        dialogOkButton.setEnabled(allFieldsFilled);
    }

    private void dialogSave() {
        if(dialogMode == DialogMode.EDIT && editingTaskPlan.getId() != null) {
            //delete existing tasks if we're editing an existing plan
            System.out.println("Deleting existing tasks for TaskPlan id: " + editingTaskPlan.getId());
            List<Task> existingTasks = taskService.findByPlanId(editingTaskPlan.getId());
            for(Task task : existingTasks) {
                System.out.println("Deleting task: " + task);
                taskService.deleteById(task.getId());
            }
        }

        editingTaskPlan.setType(selectedTaskLinkType);
        editingTaskPlan.setLinkMotherId(femaleStockSelect.getValue() != null ? femaleStockSelect.getValue().getId() : null);
        editingTaskPlan.setLinkFatherId(maleStockSelect.getValue() != null ? maleStockSelect.getValue().getId() : null);
        TaskPlan savedTaskPlan = taskPlanService.save(editingTaskPlan);
        System.out.println("Saved TaskPlan: " + savedTaskPlan + " editingTaskPlan: " + editingTaskPlan);
        for (GeneratedTaskRow row : generatedTaskRows) {
            Task task = new Task();
            task.setName(row.getName());
            task.setDate(row.getTaskDate());
            task.setTaskPlan(savedTaskPlan);
            task.setLinkType(selectedTaskLinkType);
            task.setLinkBreederId(femaleStockSelect.getValue() != null ? femaleStockSelect.getValue().getId() : null);
            task.setType(row.getType());
            System.out.println("Saving task: " + task);
            taskService.save(task);
        }
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

    private static class GeneratedTaskRow {
        private String name;
        private LocalDate taskDate;
        private Utility.TaskType type;



        GeneratedTaskRow(String name, LocalDate taskDate, Utility.TaskType type) {
            this.name = name;
            this.taskDate = taskDate;
            this.type = type;
        }

        public String getName() { return name; }
        public LocalDate getTaskDate() { return taskDate; }
        public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }
        public Utility.TaskType getType() { return type; }
    }
}
