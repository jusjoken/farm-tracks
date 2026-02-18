package ca.jusjoken.data.entity;

import java.util.ArrayList;
import java.util.List;

public class GenotypeSegment {
    private String name;
    private List<String> values = new ArrayList<>();
    private GenotypeValuePair genotypePair = new GenotypeValuePair();

    public GenotypeSegment(String name, List<String> values, GenotypeValuePair genotypePair) {
        this.name = name;
        this.values = values;
        this.genotypePair = genotypePair;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<String> getValues() {
        return values;
    }
    public void setValues(List<String> values) {
        this.values = values;
    }
    public String getValue1() {
        return genotypePair.getValue1();
    }
    public void setValue1(String value1) {
        this.genotypePair.setValue1(value1);
    }
    public String getValue2() {
        return genotypePair.getValue2();
    }
    public void setValue2(String value2) {
        this.genotypePair.setValue1(value2);
    }
    public GenotypeValuePair getGenotypePair() {
        return genotypePair;
    }
    public void setGenotypePair(GenotypeValuePair genotypePair) {
        this.genotypePair = genotypePair;
    }
    @Override
    public String toString() {
        return "GenotypeSegment [name=" + name + ", values=" + values + ", genotypePair=" + genotypePair + "]";
    }

}
