package ca.jusjoken.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextFieldVariant;

import ca.jusjoken.UIUtilities;

public class ButtonNumberField extends CustomField<Double> {
    private final NumberField numberField = new NumberField();
    private final Button button = new Button();

    public ButtonNumberField() {
        super();
        HorizontalLayout layout = UIUtilities.getHorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(numberField,button);
        add(layout);
    }

    @Override
    protected Double generateModelValue() {
        return numberField.getValue();
    }

    @Override
    protected void setPresentationValue(Double aDouble) {
        numberField.setValue(aDouble);
    }

    public void setButtonIcon(Component icon) {
        button.setIcon(icon);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addClickListener(ComponentEventListener listener) {
        button.addClickListener(listener);
    }

    public void setPrefixComponent(Component component){
        numberField.setPrefixComponent(component);
    }

    public void addThemeVariants(TextFieldVariant textFieldVariant){
        numberField.addThemeVariants(textFieldVariant);
    }

    public NumberField getNumberField() {
        return numberField;
    }
}
