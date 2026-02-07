package ca.jusjoken.component;

import java.util.stream.Stream;

import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.html.Span;

import ca.jusjoken.utility.BadgeVariant;

public class Badge extends Span implements HasTheme {

    //private Component icon;

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

    public final void addThemeVariants(BadgeVariant... variants) {
        getThemeNames().addAll(Stream.of(variants).map(BadgeVariant::getVariantName).toList());
    }
}
