package ca.jusjoken.component;

import ca.jusjoken.utility.BadgeVariant;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.html.Span;

import java.util.stream.Stream;

public class Badge extends Span implements HasTheme {

    private Component icon;

    public Badge() {
        addThemeName("badge");
    }

    public Badge(String text) {
        this();
        add(new Span(text));
    }

    public Badge(String text, BadgeVariant... variants) {
        this(text);
        addThemeVariants(variants);
    }

    public void addThemeVariants(BadgeVariant... variants) {
        getThemeNames().addAll(Stream.of(variants).map(BadgeVariant::getVariantName).toList());
    }
}
