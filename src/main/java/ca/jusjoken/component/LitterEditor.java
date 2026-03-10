package ca.jusjoken.component;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;

import java.time.LocalDate;
import java.util.Objects;

import com.vaadin.flow.component.select.Select;
import org.springframework.transaction.support.TransactionTemplate;

import ca.jusjoken.data.entity.Litter;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockStatusHistory;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.AppSettingsService;
import ca.jusjoken.data.service.LitterService;
import ca.jusjoken.data.service.Registry;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.data.service.StockStatusHistoryService;
import ca.jusjoken.data.service.TaskPlanService;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Task;
import ca.jusjoken.data.entity.TaskPlan;
import ca.jusjoken.data.service.TaskService;
import ca.jusjoken.utility.TaskType;

public class LitterEditor {

    public enum DialogMode {
        EDIT, CREATE
    }

    private DialogMode dialogMode = DialogMode.CREATE;
    private final Dialog dialog = new Dialog();
    private Litter litter;
    private String dialogTitle = "";

    private final Button dialogOkButton = new Button("OK");
    private final Button dialogCancelButton = new Button("Cancel");
    private final Button dialogCloseButton = new Button(new Icon("lumo", "cross"));

    private final TextField prefix = new TextField("Prefix");
    private final TextField name = new TextField("Name");
    private final TextField breed = new TextField("Breed");
    private final DatePicker bred = new DatePicker("Bred");
    private final DatePicker doB = new DatePicker("Born");
    private final ComboBox<Stock> father = new ComboBox<>("Father");
    private final ComboBox<Stock> mother = new ComboBox<>("Mother");
    private final IntegerField kitsCount = new IntegerField("Total Kits");
    private final IntegerField diedKitsCount = new IntegerField("Died Kits");
    private final TextArea notes = new TextArea("Notes");
    private final Select<TaskPlan> taskPlanSelect = new Select<>("Incomplete Breed Plans");
    private final TaskPlanService taskPlanService;
    private final TaskService taskService;
    private final StockStatusHistoryService stockStatusHistoryService;
    private final TransactionTemplate transactionTemplate;

    private StockType currentStockType;
    private LitterService litterService;
    private StockService stockService;
    private AppSettingsService appSettingsService;
    private VerticalLayout dialogLayout;
    private List<ListRefreshNeededListener> listRefreshNeededListeners = new ArrayList<>();

    public LitterEditor() {
        litterService = Registry.getBean(LitterService.class);
        stockService = Registry.getBean(StockService.class);
        appSettingsService = Registry.getBean(AppSettingsService.class);

        taskPlanService = Registry.getBean(TaskPlanService.class);
        taskService = Registry.getBean(TaskService.class);
        stockStatusHistoryService = Registry.getBean(StockStatusHistoryService.class);
        transactionTemplate = Registry.getBean(TransactionTemplate.class);

        dialogConfigure();
    }

    private void dialogConfigure() {
        dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setMargin(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "270px").set("max-width", "100%");
        dialog.add(dialogLayout);

        dialogCloseButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialogCloseButton.addClickListener(e -> dialogClose());

        dialog.setCloseOnEsc(true);

        dialogCancelButton.setAutofocus(true);
        dialogCancelButton.addClickListener(e -> dialogClose());

        dialogOkButton.setEnabled(false);
        dialogOkButton.setDisableOnClick(true);
        dialogOkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialogOkButton.addClickListener(e -> dialogSave());

        HorizontalLayout footerLayout = new HorizontalLayout(dialogCancelButton, dialogOkButton);
        ShortcutRegistration shortcutRegistration = Shortcuts
                .addShortcutListener(footerLayout, () -> {}, Key.ENTER)
                .listenOn(footerLayout);
        shortcutRegistration.setEventPropagationAllowed(false);
        shortcutRegistration.setBrowserDefaultAllowed(true);
        dialog.getFooter().add(footerLayout);

        father.setItemLabelGenerator(Stock::getDisplayName);
        mother.setItemLabelGenerator(Stock::getDisplayName);

        kitsCount.setMin(0);
        diedKitsCount.setMin(0);

        notes.setMaxLength(4000);

        prefix.setWidthFull();
        name.setWidthFull();
        breed.setWidthFull();
        bred.setWidthFull();
        doB.setWidthFull();
        father.setWidthFull();
        mother.setWidthFull();
        kitsCount.setWidthFull();
        diedKitsCount.setWidthFull();
        notes.setWidthFull();
        taskPlanSelect.setWidthFull();

        taskPlanSelect.addValueChangeListener(e -> applyTaskPlanSelection(e.getValue()));
        prefix.addValueChangeListener(e -> updateSaveEnabled());
        name.addValueChangeListener(e -> updateSaveEnabled());
        breed.addValueChangeListener(e -> updateSaveEnabled());
        bred.addValueChangeListener(e -> updateSaveEnabled());
        doB.addValueChangeListener(e -> updateSaveEnabled());
        father.addValueChangeListener(e -> updateSaveEnabled());
        mother.addValueChangeListener(e -> updateSaveEnabled());
        kitsCount.addValueChangeListener(e -> updateSaveEnabled());
        diedKitsCount.addValueChangeListener(e -> updateSaveEnabled());

        kitsCount.setAutoselect(true);
        diedKitsCount.setAutoselect(true);

    }

    public void dialogOpen() {
        dialogOpen(new Litter(), DialogMode.CREATE, null, null);
    }

    public void dialogOpen(Litter litterEntity, DialogMode mode, StockType stockType) {
        dialogOpen(litterEntity, mode, stockType, null);
    }

    public void dialogOpen(Litter litterEntity, DialogMode mode, StockType stockType, TaskPlan taskPlan) {
        this.litter = litterEntity;
        this.dialogMode = mode;
        this.currentStockType = stockType;

        dialogTitle = (dialogMode == DialogMode.CREATE) ? "Create new litter" : "Edit litter";
        dialogLayout.removeAll();

        dialog.setHeaderTitle(dialogTitle);
        dialog.getElement().setAttribute("aria-label", dialogTitle);
        dialog.getHeader().add(dialogCloseButton);
        dialog.setDraggable(true);
        dialog.setResizable(true);

        if (currentStockType != null) {
            father.setItems(stockService.getFathers(null, currentStockType));
            mother.setItems(stockService.getMothers(null, currentStockType));
        } else {
            father.setItems(stockService.findAllBreeders());
            mother.setItems(stockService.findAllBreeders());
        }

        var breedPlans = loadBreedTaskPlans();
        if (taskPlan != null && breedPlans.stream().noneMatch(p -> Objects.equals(p.getId(), taskPlan.getId()))) {
            breedPlans.add(taskPlan);
        }
        taskPlanSelect.setItems(breedPlans);
        taskPlanSelect.setItemLabelGenerator(taskplan -> taskPlanService.getDisplayName(taskplan));
        taskPlanSelect.clear();

        prefix.setValue(litter.getPrefix() == null ? appSettingsService.getAppSettings().getFarmPrefix() : litter.getPrefix());
        name.setValue(litter.getName() == null ? getNextLitterName() : litter.getName());
        breed.setValue(litter.getBreed() == null ? getBreedFromTaskPlan(taskPlan) : litter.getBreed());
        bred.setValue(litter.getBred());
        doB.setValue(litter.getDoB());
        father.setValue(litter.getFather());
        mother.setValue(litter.getMother());
        kitsCount.setValue(litter.getKitsCount() == null ? 0 : litter.getKitsCount());
        diedKitsCount.setValue(litter.getDiedKitsCount() == null ? 0 : litter.getDiedKitsCount());
        notes.setValue(litter.getNotes() == null ? "" : litter.getNotes());

        if (taskPlan != null) {
            TaskPlan matchingPlan = breedPlans.stream()
                    .filter(p -> Objects.equals(p.getId(), taskPlan.getId()))
                    .findFirst()
                    .orElse(null);
            taskPlanSelect.setValue(matchingPlan);
            taskPlanSelect.setReadOnly(true);
            kitsCount.setAutofocus(true);
        }

        kitsCount.setReadOnly(dialogMode == DialogMode.EDIT);

        dialogLayout.add(new Hr(), taskPlanSelect, prefix, name, breed, bred, doB, father, mother, kitsCount, diedKitsCount, notes);
        updateSaveEnabled();
        dialog.open();
    }

    private String getBreedFromTaskPlan(TaskPlan taskPlan) {
        if (taskPlan == null) {
            return "";  
        }

        if (taskPlan.getLinkMotherId() != null) {
            Stock motherOpt = stockService.findById(taskPlan.getLinkMotherId());
            if (motherOpt != null && motherOpt.getBreed() != null) {
                return motherOpt.getBreed();
            }
        }
        return "";
    }

    private String getNextLitterName() {
        String prefixToUse = appSettingsService.getAppSettings().getDefaultLitterPrefix();
        String nextName = litterService.getNextLitterName(prefixToUse);
        return nextName;
    }

    private void updateSaveEnabled() {
        boolean hasTaskPlan = taskPlanSelect.getValue() != null;
        boolean hasPrefix = prefix.getValue() != null && !prefix.getValue().trim().isEmpty();
        boolean hasName = name.getValue() != null && !name.getValue().trim().isEmpty();
        boolean hasBreed = breed.getValue() != null && !breed.getValue().trim().isEmpty();
        boolean hasBred = bred.getValue() != null;
        boolean hasDob = doB.getValue() != null;
        boolean hasParents = father.getValue() != null && mother.getValue() != null;
        boolean hasKits = kitsCount.getValue() != null && kitsCount.getValue() > 0;
        boolean hasValidDeaths = diedKitsCount.getValue() != null && diedKitsCount.getValue() >= 0;
        dialogOkButton.setEnabled(hasTaskPlan && hasPrefix && hasName && hasBreed && hasBred && hasDob && hasParents && hasKits && hasValidDeaths);
    }

    private void dialogSave() {
        litter.setStockType(currentStockType != null ? currentStockType : litter.getStockType());
        litter.setPrefix(trimOrNull(prefix.getValue()));
        litter.setName(trimOrNull(name.getValue()));
        litter.setBreed(trimOrNull(breed.getValue()));
        litter.setBred(bred.getValue());
        litter.setDoB(doB.getValue());
        litter.setFather(father.getValue());
        litter.setMother(mother.getValue());
        litter.setKitsCount(zeroIfNull(kitsCount.getValue()));
        litter.setDiedKitsCount(zeroIfNull(diedKitsCount.getValue()));
        litter.setNotes(trimOrNull(notes.getValue()));

        if (dialogMode == DialogMode.CREATE) {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Create litter:");
            confirm.setText("This will record the birth as complete, create the litter as well as create the " + litter.getKitsCount() + " stock record(s) for this litter (" + litter.getDiedKitsCount() + " marked as Died).");
            confirm.setCancelable(true);
            confirm.setCancelText("No");
            confirm.setConfirmText("Yes");
            confirm.addConfirmListener(event -> {
                Litter saved = litterService.save(litter);
                //mark the associated birth task as complete
                Task birthTask = taskService.findByPlanId(taskPlanSelect.getValue().getId()).stream()
                        .filter(t -> t.getType() == TaskType.BIRTH)
                        .findFirst()
                        .orElse(null);
                if (birthTask != null) {
                    birthTask.setCompleted(true);
                    taskService.save(birthTask);
                }
                createKitStocks(saved.getId(), zeroIfNull(saved.getKitsCount()), zeroIfNull(saved.getDiedKitsCount()));
                notifyRefreshNeeded();
                dialogClose();
            });
            confirm.addCancelListener(event -> {
                Litter saved = litterService.save(litter);
                notifyRefreshNeeded();
                dialogClose();
            });
            confirm.open();
            return;
        }

        litterService.save(litter);
        notifyRefreshNeeded();
        dialogClose();
    }

    private void createKitStocks(Integer litterId, int count, int diedCount) {
        if (litterId == null || count <= 0) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            Litter managedLitter = litterService.findById(litterId);
            if (managedLitter == null) {
                return;
            }

            for (int i = 1; i <= count; i++) {
                Stock kit = new Stock();
                kit.setStockType(managedLitter.getStockType());
                kit.setLitter(managedLitter);
                kit.setTattoo(managedLitter.getName() + "-" + i);
                kit.setPrefix(managedLitter.getPrefix());
                kit.setBreed(managedLitter.getBreed());
                kit.setDoB(managedLitter.getDoB());
                kit.setAcquired(managedLitter.getDoB());
                kit.setFatherId(managedLitter.getFather().getId());
                kit.setMotherId(managedLitter.getMother().getId());

                stockService.save(kit);

                if (managedLitter.getDoB() != null && i > count - diedCount) {
                    stockStatusHistoryService.save(
                        new StockStatusHistory(kit.getId(), "died", managedLitter.getDoB().atStartOfDay()),
                        kit,
                        Boolean.FALSE
                    );
                }
            }
        });
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer zeroIfNull(Integer value) {
        return value == null ? 0 : value;
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

    private void applyTaskPlanSelection(TaskPlan selectedPlan) {
        if (selectedPlan == null) {
            return;
        }

        father.setValue(selectedPlan.getLinkFatherId() == null ? null : stockService.findById(selectedPlan.getLinkFatherId()));
        mother.setValue(selectedPlan.getLinkMotherId() == null ? null : stockService.findById(selectedPlan.getLinkMotherId()));

        LocalDate bredFromTask = findTaskDateByTaskType(selectedPlan, TaskType.BREED);
        if (bredFromTask != null) {
            bred.setValue(bredFromTask);
        }

        LocalDate bornFromTask = findTaskDateByTaskType(selectedPlan, TaskType.BIRTH);
        if (bornFromTask != null) {
            doB.setValue(bornFromTask);
        }
        breed.setValue(getBreedFromTaskPlan(selectedPlan));

        updateSaveEnabled();
    }

    private List<TaskPlan> loadBreedTaskPlans() {
        List<TaskPlan> plans = new ArrayList<>();
        if (taskPlanService == null) {
            return plans;
        }

        plans = taskPlanService.findAllIncompleteBirthPlans().stream()
                .filter(p -> p.getType() == Utility.TaskLinkType.BREEDER)
                .toList();

        return plans;
    }

    private LocalDate findTaskDateByTaskType(TaskPlan plan, TaskType taskType) {
        if (taskService == null || plan == null) {
            return null;
        }

        List<Task> tasks = taskService.findByPlanId(plan.getId());
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }

        Integer counter = 0;
        for (Task task : tasks) {
            counter++;
            if(task.getType() == taskType) {
                return task.getDate();
            }
        }

        return null;
    }

    public void runTaskAction(Task task, StockType stockType) {
        if (task == null || task.getType() == null || !task.getType().hasAction()) {
            return;
        }

        switch (task.getType()) {
            case BIRTH -> openForBirthTask(task, stockType);
            default -> {
                // future task actions
            }
        }
    }

    private void openForBirthTask(Task task, StockType stockType) {
        TaskPlan taskPlan = null;
        Integer taskPlanId = null;

        try {
            if (task.getTaskPlan() != null) {
                taskPlanId = task.getTaskPlan().getId();
            }
        } catch (Exception ignored) {
            // lazy proxy fallback below
        }

        if (taskPlanId == null) {
            try {
                var method = task.getClass().getMethod("getTaskPlanId");
                Object value = method.invoke(task);
                if (value instanceof Integer id) {
                    taskPlanId = id;
                }
            } catch (Exception ignored) {
                // no plan id available
            }
        }

        if (taskPlanId != null) {
            taskPlan = taskPlanService.findById(taskPlanId).orElse(null);
        }

        dialogOpen(new Litter(), DialogMode.CREATE, stockType, taskPlan);
    }
}
