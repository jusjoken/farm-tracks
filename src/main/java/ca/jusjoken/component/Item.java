package ca.jusjoken.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;

/**
 * Convenience class for creating menu items with icons.
 */
public class Item extends Layout {

    public Item(String text, String icon) {
        setAlignItems(Layout.AlignItems.CENTER);
        setGap(Layout.Gap.SMALL);

        Icon tIcon = new Icon(icon);
        tIcon.addClassNames(IconSize.SMALL);
        add(tIcon, new Text(text));
    }

    
}
