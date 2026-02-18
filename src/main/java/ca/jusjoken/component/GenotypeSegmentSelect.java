package ca.jusjoken.component;

import java.util.List;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;

import ca.jusjoken.data.entity.GenotypeSegment;

public class GenotypeSegmentSelect extends AbstractCompositeField<HorizontalLayout,GenotypeSegmentSelect,GenotypeSegment> {
    private Select<String> segment1 = new Select<>();
    private Select<String> segment2 = new Select<>();
    private GenotypeSegment genotypeSegment;
    private Span labelText = new Span();

    public GenotypeSegmentSelect() {
        super(null);

        labelText.setWidth("5%");
        segment1.setWidth("40%");
        segment2.setWidth("40%");

        getContent().addToStart(labelText);
        getContent().addToEnd(segment1,segment2);
        getContent().setPadding(false);
        getContent().setSpacing(true);
        getContent().setMargin(false);
        getContent().setWidthFull();
        getContent().setAlignItems(FlexComponent.Alignment.BASELINE);

        segment1.addValueChangeListener(event -> {
            genotypeSegment.setValue1(event.getValue());
            setModelValue(genotypeSegment, true);
        });

        segment2.addValueChangeListener(event -> {
            genotypeSegment.setValue2(event.getValue());
            setModelValue(genotypeSegment, true);
        });

    }

    @Override
    protected void setPresentationValue(GenotypeSegment newPresentationValue) {
        if(newPresentationValue!=null){
            genotypeSegment = newPresentationValue;
            labelText.setText(genotypeSegment.getName());

            segment1.setItems(genotypeSegment.getValues());
            segment1.setValue(genotypeSegment.getValue1());
            segment2.setItems(genotypeSegment.getValues());
            segment2.setValue(genotypeSegment.getValue2());

            getElement().setPropertyBean("value", genotypeSegment);
        }

    }

    @Override
    public void setValue(GenotypeSegment value) {
        super.setValue(value);
        genotypeSegment = value;
    }

    @Override
    public GenotypeSegment getValue() {
        return genotypeSegment;
    }

    public void setLabel(String label){
        labelText.setText(label);
    }

    public void setItems(List<String> items){
        segment1.setItems(items);
        segment2.setItems(items);
    }

    public Select<String> getSegment1(){
        return segment1;
    }

    public Select<String> getSegment2(){
        return segment2;
    }

}
