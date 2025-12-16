/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken;

import ca.jusjoken.component.ButtonNumberField;
import ca.jusjoken.component.Item;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import org.vaadin.lineawesome.LineAwesomeIcon;

/**
 *
 * @author birch
 */
public class UIUtilities {
    
    public static String borderSizeSmall = "2px";
    public static String borderSizeLarge = "6px";

    public static String borderColorFemale = "#ca195a";
    public static String borderColorMale = "#00a7d0";
    //public static String borderColorFemale = "var(--lumo-error-color-50pct)";
    //public static String borderColorMale = "var(--lumo-primary-color-50pct)";
    public static String borderColorNA = "var(--lumo-border-contrast)";
    public static String borderColorError = "var(--lumo-border-error)";

    public static final String boxShadowStyle = "inset 0px 0px 3px 4px var(--lumo-success-color)";
    public static final String boxShadowStyleRadius = "6px";
    
    
    public static void showNotification(String text) {
        Notification.show(text, 3000, Notification.Position.BOTTOM_CENTER);
    }

    public static void showNotificationError(String text) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setPosition(Notification.Position.BOTTOM_CENTER);

        Div textDiv = new Div(new Text(text));

        Button closeButton = new Button(new Icon("lumo", "cross"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.getElement().setAttribute("aria-label", "Close");
        closeButton.addClickListener(event -> {
            notification.close();
        });

        HorizontalLayout layout = new HorizontalLayout(textDiv, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.open();
    }

    public static NumberField getNumberField(String label, Double number){
        NumberField numberField = getNumberField(label);
        numberField.setValue(number);
        return numberField;
    }

    public static NumberField getNumberField(Boolean readOnly){
        return getNumberField("",readOnly,"$");
    }
    public static NumberField getNumberField(){
        return getNumberField("",Boolean.TRUE, "$");
    }
    public static NumberField getNumberField(String label){
        return getNumberField(label,Boolean.TRUE, "$");
    }

    public static NumberField getNumberField(String label, Boolean readOnly, String prefix){
        Div prefixDiv = new Div();
        prefixDiv.setText(prefix);
        NumberField numberField = new NumberField();
        if(!label.isEmpty()){
            numberField.setLabel(label);
        }
        numberField.setReadOnly(readOnly);
        numberField.setPrefixComponent(prefixDiv);
        numberField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return numberField;
    }

    public static ButtonNumberField getButtonNumberField(String label, Boolean readOnly, String prefix){
        Div prefixDiv = new Div();
        prefixDiv.setText(prefix);
        ButtonNumberField numberField = new ButtonNumberField();
        if(!label.isEmpty()){
            numberField.setLabel(label);
        }
        numberField.setReadOnly(readOnly);
        numberField.setPrefixComponent(prefixDiv);
        numberField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return numberField;
    }


    public static TextField getTextFieldRO(String label, String text, String width ){
        TextField textField = getTextFieldRO(label,text);
        textField.setWidth(width);
        return textField;
    }
    public static TextField getTextFieldRO(String label, String text){
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setValue(text);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return textField;
    }

    public static TextField getTextField(){
        return getTextField("");
    }
    public static TextField getTextField(String label){
        TextField textField = new TextField();
        if(!label.isEmpty()){
            textField.setLabel(label);
        }
        textField.setReadOnly(false);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return textField;
    }

    public static HorizontalLayout getHorizontalLayout(){
        return getHorizontalLayout(false,false,false);
    }
    public static HorizontalLayout getHorizontalLayout(Boolean paddingEnabled, Boolean spacingEnabled, Boolean marginEnabled){
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setPadding(paddingEnabled);
        horizontalLayout.setSpacing(spacingEnabled);
        horizontalLayout.setMargin(marginEnabled);
        horizontalLayout.setWidthFull();
        return horizontalLayout;
    }

    public static HorizontalLayout getHorizontalLayoutNoWidthCentered(){
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setPadding(false);
        horizontalLayout.setSpacing(false);
        horizontalLayout.setMargin(false);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        return horizontalLayout;
    }

    public static VerticalLayout getVerticalLayout(){
        return getVerticalLayout(false,false,false);
    }
    public static VerticalLayout getVerticalLayout(Boolean paddingEnabled, Boolean spacingEnabled, Boolean marginEnabled){
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(paddingEnabled);
        verticalLayout.setSpacing(spacingEnabled);
        verticalLayout.setMargin(marginEnabled);
        verticalLayout.setWidthFull();
        return verticalLayout;
    }

    public static String singlePlural(int count, String singular, String plural)
    {
        return count==1 ? singular : plural;
    }

    public static TextField createSmallTextField(String label) {
        TextField textField = new TextField(label);
        textField.addValueChangeListener(event ->{
            //setTooltip(event.getSource().getValue(),event.getSource());
        });
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        return textField;
    }

    public static void setBorders(Component component, Stock stock, Boolean largeBorder){
        if(stock==null){
            component.getStyle().set("border-color", UIUtilities.borderColorError);
            component.getStyle().set("border-width", UIUtilities.borderSizeSmall);
            component.getStyle().set("border-style", "solid");
            return;
        }
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            component.getStyle().set("border-color", UIUtilities.borderColorFemale);
        }else if(stock.getSex().equals(Utility.Gender.MALE)){
            component.getStyle().set("border-color", UIUtilities.borderColorMale);
        }else{
            component.getStyle().set("border-color", UIUtilities.borderColorNA);
        }
        if(largeBorder){
            component.getStyle().set("border-width", UIUtilities.borderSizeLarge);
        }else{
            component.getStyle().set("border-width", UIUtilities.borderSizeSmall);
        }
        component.getStyle().set("border-style", "solid");
    }
    
    public static Component createDefaultActions() {
        //MenuBar menuBar = new MenuBar();
        Button menuButton = new Button(LineAwesomeIcon.ELLIPSIS_H_SOLID.create());
        menuButton.setAriaLabel("Actions");
        menuButton.setTooltipText("Actions");
        menuButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        ContextMenu menu = new ContextMenu(menuButton);
        menu.setOpenOnClick(true);

        //this code will make sure the Details panel does not expand/colapse when the menu button is clicked
        menuButton.getElement().addEventListener("click", click -> {
            //do nothing
        }).addEventData("event.stopPropagation()");
        
        menu.addItem(new Item("Edit", Utility.ICONS.ACTION_EDIT.getIconSource()));
        menu.addItem(new Item("Breed", Utility.ICONS.TYPE_BREEDER.getIconSource()));
        menu.addItem(new Item("Birth", Utility.ICONS.ACTION_BIRTH.getIconSource()));
        menu.addItem(new Item("Cage Card", Utility.ICONS.ACTION_CAGE_CARD.getIconSource()));
        menu.addItem(new Item("Mark For Sale", Utility.ICONS.ACTION_MARK_FOR_SALE.getIconSource()));
        menu.addItem(new Item("Sell", Utility.ICONS.STATUS_SOLD.getIconSource()));
        menu.addItem(new Item("Deposit taken", Utility.ICONS.STATUS_SOLD_W_DEPOSIT.getIconSource()));
        menu.addItem(new Item("Butcher", Utility.ICONS.STATUS_BUTHERED.getIconSource()));
        menu.addItem(new Item("Died", Utility.ICONS.STATUS_DIED.getIconSource()));
        menu.addItem(new Item("Archive", Utility.ICONS.STATUS_ARCHIVED.getIconSource()));
        menu.addItem(new Item("Cull", Utility.ICONS.STATUS_CULLED.getIconSource()));
        menu.addItem(new Item("Delete", Utility.ICONS.ACTION_DELETE.getIconSource()));

        return menuButton;
    }
    
}
