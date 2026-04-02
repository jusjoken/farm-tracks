/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;

import ca.jusjoken.component.ButtonNumberField;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.entity.Stock;

/**
 *
 * @author birch
 */
public class UIUtilities {

    public static enum DialogWidthPreset {
        COMPACT,
        MEDIUM,
        LARGE,
        WIDE,
        XLARGE
    }
    
    public static enum BorderSize{
        LARGE("6px"), SMALL("2px"), XSMALL("1px");

        public final String sizeName;
        private BorderSize(String s) {
            this.sizeName = s;
        }
    }
    
    public static String borderSizeSmall = "1px";
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

    public static void applyDialogWidth(VerticalLayout layout, DialogWidthPreset preset) {
        applyDialogWidth(null, layout, preset);
    }

    public static void applyDialogWidth(Dialog dialog, VerticalLayout layout, DialogWidthPreset preset) {
        String width;
        switch (preset) {
            case COMPACT -> {
                width = "min(34rem, 92vw)";
            }
            case MEDIUM -> {
                width = "min(44rem, 92vw)";
            }
            case LARGE -> {
                width = "min(48rem, 92vw)";
            }
            case WIDE -> {
                width = "min(64rem, 92vw)";
            }
            case XLARGE -> {
                width = "min(72rem, 92vw)";
            }
            default -> {
                width = "min(48rem, 92vw)";
            }
        }

        if (dialog != null) {
            dialog.setWidth(width);
            dialog.setMaxWidth("92vw");
        }

        layout.getStyle()
                .set("width", "100%")
                .set("min-width", "0")
                .set("max-width", "100%");
    }

    public static void applyResponsiveDialogFooter(HorizontalLayout footerLayout) {
        footerLayout.setWidthFull();
        footerLayout.setPadding(false);
        footerLayout.setMargin(false);
        footerLayout.setSpacing(true);
        footerLayout.setWrap(true);
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    }

    public static void applyDialogDraggableForViewport(Dialog dialog) {
        // Desktop keeps drag support; mobile disables it to avoid accidental shifts.
        dialog.setDraggable(false);
        dialog.getElement().executeJs(
            "this.draggable = !window.matchMedia('(max-width: 767px)').matches;"
        );
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

    public static void setBorders(Component component, Stock stock, BorderSize borderSize){
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
        component.getStyle().set("border-width", borderSize.sizeName);
        /*
        if(largeBorder){
            component.getStyle().set("border-width", UIUtilities.borderSizeLarge);
        }else{
            component.getStyle().set("border-width", UIUtilities.borderSizeSmall);
        }
        */
        component.getStyle().set("border-style", "solid");
    }

    public static void setCardBorders(Component component, Stock stock){
        if(stock.getSex().equals(Utility.Gender.FEMALE)){
            component.getStyle().set("--vaadin-card-border-color", UIUtilities.borderColorFemale);
        }else if(stock.getSex().equals(Utility.Gender.MALE)){
            component.getStyle().set("--vaadin-card-border-color", UIUtilities.borderColorMale);
        }else{
            component.getStyle().set("--vaadin-card-border-color", UIUtilities.borderColorNA);
        }
    }

    public static Span getSuperScriptSpan(String text){
        Span span = new Span(text);
        span.getStyle().set("font-size", "0.6em");
        span.getStyle().set("line-height", "1");
        span.getStyle().set("display", "inline-block");
        span.getStyle().set("transform", "translateY(-0.5em)");

        // span.getStyle().set("font-size", "0.72em");
        // span.getStyle().set("line-height", "1");
        // span.getStyle().set("transform", "translateY(-0.45em)");
        span.getStyle().set("margin-left", "0.15em");
        span.getStyle().set("margin-right", "0.2em");

        return span;
    } 

    public static Component getContextMenuHeader(String text){
        //return a layout with the text centered and bold, and with a bottom border
        HorizontalLayout header = new HorizontalLayout(new Text(text));
        header.setPadding(true);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.getStyle().set("font-weight", "bold");
        // Use Lumo variables so contrast works in both light and dark modes.
        header.getStyle().set("background", "var(--lumo-contrast-10pct)");
        header.getStyle().set("color", "var(--lumo-body-text-color)");
        header.getStyle().set("border-radius", "0 0 0.25em 0.25em");
        return header;
    }

    public static Badge createBadge(String prefix, String text, BadgeVariant... variants){
        if(prefix!=null){
            text = prefix + ": " + text;
        }
        Badge badge = new Badge(text);
        badge.addClassNames(FontSize.SMALL, FontWeight.MEDIUM);
        badge.addThemeVariants(variants);
        return badge;
    }

    public static Button createGridHeaderContextMenuButton(String ariaLabel) {
        Button menuButton = new Button(VaadinIcon.COGS.create());
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        menuButton.getElement().setAttribute("title", "Grid actions");
        menuButton.getElement().setAttribute(
                "aria-label",
                ariaLabel == null || ariaLabel.isBlank() ? "Open grid menu" : ariaLabel);
        menuButton.getStyle().set("flex-shrink", "0");
        menuButton.addClickListener(event -> menuButton.getElement().executeJs(
                "const btn=this;"
                        + "const grid=btn.closest('vaadin-grid');"
                        + "if(!grid){return;}"
                        + "const rect=btn.getBoundingClientRect();"
                        + "grid.dispatchEvent(new MouseEvent('contextmenu', {"
                        + "bubbles:true,cancelable:true,composed:true,view:window,clientX:rect.left + rect.width/2,clientY:rect.bottom"
                        + "}));"));
        return menuButton;
    }

}
