package ca.jusjoken.data.service;

import com.vaadin.flow.component.ComponentEvent;

import ca.jusjoken.views.utility.PlanTemplateView;

public class ListRefreshNeededEvent extends ComponentEvent<PlanTemplateView> {
    public ListRefreshNeededEvent(PlanTemplateView source, boolean fromClient) {
        super(source, fromClient);
    }
}