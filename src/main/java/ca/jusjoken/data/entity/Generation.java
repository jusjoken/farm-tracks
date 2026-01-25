/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.data.entity;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.AvatarDiv;
import ca.jusjoken.component.Layout;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author birch
 */
public class Generation {
    private Integer level;
    private Generation father;
    private Generation mother;
    private Generation child;
    private Details details = new Details();
    private Layout header = new Layout();
    private Stock stock; //optional

    //pedigree fields
    private String name;
    private String prefix;
    private String tattoo; //ID
    private LocalDate doB;
    private String color;
    private Integer weight; //in oz
    private String legs;
    private String champNo; //GC#
    private String regNo;
    private Gender sex;
    private String breed;

    private List<Generation> breadcrumbs = new ArrayList<>();
    
    public Generation(Stock stock, Integer level) {
        this.stock = stock;
        this.level = level;
        createPedigreeItem();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Generation getFather() {
        return father;
    }

    public void setFather(Generation father) {
        this.father = father;
    }

    public Generation getMother() {
        return mother;
    }

    public void setMother(Generation mother) {
        this.mother = mother;
    }

    public Generation getChild() {
        return child;
    }

    public void setChild(Generation child) {
        this.child = child;
    }

    public Details getDetails() {
        return details;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getTattoo() {
        return tattoo;
    }

    public void setTattoo(String tattoo) {
        this.tattoo = tattoo;
    }

    public LocalDate getDoB() {
        return doB;
    }

    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    public String getWeightInLbsOz(){
        if(stock==null) return "";
        return stock.getWeightInLbsOzAsString();
    }
    
    public String getLegs() {
        return legs;
    }

    public void setLegs(String legs) {
        this.legs = legs;
    }

    public String getChampNo() {
        return champNo;
    }

    public void setChampNo(String champNo) {
        this.champNo = champNo;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public Gender getSex() {
        return sex;
    }

    public void setSex(Gender sex) {
        this.sex = sex;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }
    
    

    public List<Generation> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<Generation> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public void addBreadcrumb(Generation breadcrumb){
        this.breadcrumbs.add(breadcrumb);
    }

    private void createPedigreeItem(){
        if(stock==null){
            if(name==null){ //use defaults
                name = Utility.emptyValue;
                prefix = Utility.emptyValue;
                tattoo = Utility.emptyValue;
                doB = null;
                color = Utility.emptyValue;
                weight = null;
                legs = Utility.emptyValue;
                champNo = Utility.emptyValue;
                regNo = Utility.emptyValue;
                sex = Gender.NA;
                breed = Utility.emptyValue;
            }else{ //use entity
                //prefilled
            }
        }else{  //set from stock
            name = stock.getDisplayName();
            prefix = stock.getPrefix();
            tattoo = stock.getTattoo();
            doB = stock.getDoB();
            color = stock.getColor();
            weight = stock.getWeight();
            legs = stock.getLegs();
            champNo = stock.getChampNo();
            regNo = stock.getRegNo();
            sex = stock.getSex();
            breed = stock.getBreed();
        }
        
        details.addThemeVariants(DetailsVariant.LUMO_FILLED);
        if(stock==null){
            details.setSummaryText(name);
        }else{
            details.setSummary(stock.getHeader(Boolean.TRUE));
        }
        UIUtilities.setBorders(details, stock, UIUtilities.BorderSize.SMALL);
        
        
    }

    public Layout getHeader() {
        header.removeAll();
        header.addClassNames(LumoUtility.Padding.End.XSMALL, LumoUtility.Padding.Start.XSMALL, LumoUtility.Padding.Vertical.SMALL);
        header.addClassNames(LumoUtility.Margin.End.XLARGE);
        header.setAlignItems(Layout.AlignItems.START);
        header.setJustifyContent(Layout.JustifyContent.BETWEEN);
        for(int i = 1; i < getLevel(); i++){
            System.out.println("getHeader: loop:" + name + " i:" + i);
            Div spacer = new Div("");
            spacer.setWidth("80px");
            spacer.getStyle().set("flex-shrink", "0");
            header.add(spacer);
        }
        Layout stockHeader = stock.getHeader(Boolean.TRUE);
        stockHeader.addClassNames(LumoUtility.Padding.End.SMALL, LumoUtility.Padding.Start.SMALL, LumoUtility.Padding.Vertical.SMALL);
        UIUtilities.setBorders(stockHeader, stock, UIUtilities.BorderSize.SMALL);
        header.add(stockHeader);
        header.setWidth("90%");
        return header;
    }
    
    public AvatarDiv getAvatar(){
        return stock.getAvatar(Boolean.FALSE);
    }

    @Override
    public String toString() {
        return "Generation{" + "level=" + level + ", father=" + father + ", mother=" + mother + ", child=" + child + ", details=" + details + ", header=" + header + ", stock=" + stock + ", name=" + name + ", prefix=" + prefix + ", tattoo=" + tattoo + ", doB=" + doB + ", color=" + color + ", weight=" + weight + ", legs=" + legs + ", champNo=" + champNo + ", regNo=" + regNo + ", sex=" + sex + ", breed=" + breed + ", breadcrumbs=" + breadcrumbs + '}';
    }


}
