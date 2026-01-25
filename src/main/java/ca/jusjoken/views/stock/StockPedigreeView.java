/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ca.jusjoken.views.stock;

import ca.jusjoken.UIUtilities;
import ca.jusjoken.component.Layout;
import ca.jusjoken.component.ListRefreshNeededListener;
import ca.jusjoken.data.Utility;
import ca.jusjoken.data.Utility.Gender;
import ca.jusjoken.data.entity.Generation;
import ca.jusjoken.data.entity.Stock;
import ca.jusjoken.data.entity.StockType;
import ca.jusjoken.data.service.StockService;
import ca.jusjoken.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author birch
 */
@Route(value = "pedigree", layout = MainLayout.class)
@PermitAll
public class StockPedigreeView extends Main implements ListRefreshNeededListener, HasDynamicTitle, HasUrlParameter<String>  {
    private final StockService stockService;
    private Stock stock;
    private StockType viewStockType;
    private Boolean hasStock;
    private String name = Utility.emptyValue;

    public StockPedigreeView(StockService stockService) {
        System.out.println("StockPedigreeView constructor called: hasStock:" + hasStock);
        
        this.stockService = stockService;
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Height.FULL, LumoUtility.Overflow.HIDDEN);
    }

    private Component createContent() {
        System.out.println("StockPedigreeView createContent: hasStock:" + hasStock);
        Layout content;
        if(stock!=null && hasStock){
            content = new Layout(createPedigree());
        }else{ //blank view
            content = new Layout(new Span("No stock"));
        }
        content.addClassNames(LumoUtility.Flex.GROW);
        content.setFlexDirection(Layout.FlexDirection.COLUMN);
        content.setOverflow(Layout.Overflow.HIDDEN);
        return content;
    }
    
    private Component createPedigree(){
        
        HorizontalLayout pedigreeLayout = UIUtilities.getHorizontalLayout(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        VerticalLayout pedCol1 = createPedigreeColumn();
        VerticalLayout pedCol2 = createPedigreeColumn();
        VerticalLayout pedCol3 = createPedigreeColumn();
        VerticalLayout pedCol4 = createPedigreeColumn();
        pedigreeLayout.add(pedCol1,pedCol2,pedCol3,pedCol4);
        
        List<Generation> genList = loadGenerations();

        for(Generation g: genList){
            //System.out.println("Item:" + g.toString());
            switch (g.getLevel()) {
                case 1:
                    pedCol1.add(g.getDetails());
                    break;
                case 2:
                    pedCol2.add(g.getDetails());
                    break;
                case 3:
                    pedCol3.add(g.getDetails());
                    break;
                case 4:
                    pedCol4.add(g.getDetails());
                    break;
                default:
                    break;
            }
        }

        for(Generation g: genList){
            joinPedItems(g.getDetails(), g.getFather().getDetails());
            joinPedItems(g.getDetails(), g.getMother().getDetails());
        }
        
        return pedigreeLayout;
    }
    
    public Details createPedigreeItem(String name){
        Details pedItem = new Details();
        pedItem.addThemeVariants(DetailsVariant.LUMO_FILLED);
        pedItem.setSummaryText(name);
        return pedItem;
    }
    
    
    private VerticalLayout createPedigreeColumn(){
        VerticalLayout pedCol = UIUtilities.getVerticalLayout(Boolean.FALSE, Boolean.TRUE, Boolean.TRUE);
        pedCol.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        return pedCol;
    }
    
    private void joinPedItems(Details item1, Details item2){
        System.out.println("joinPedItems: item1:" + item1.getSummaryText() + " item2:" + item2.getSummaryText());
    }
    
    private List<Generation> loadGenerations(){
        List<Generation> list = new ArrayList<>();
        Generation B = new Generation(stock, 1);
        createGeneration(B, list);
        return list;
    }
    
    private Generation createGeneration(Generation gen, List<Generation> list){
        //get parent stock from child passed in 
        Stock stockF;
        Stock stockM;
        if(gen.getStock()!=null){ //get from parent
            stockF = stockService.getFather(gen.getStock());
            if(stockF==null){
                stockF = createGenericStock(Gender.MALE);
            }
            stockM = stockService.getMother(gen.getStock());
            if(stockM==null){
                stockM = createGenericStock(Gender.FEMALE);
            }
        }else{
            stockF = createGenericStock(Gender.MALE);
            stockM = createGenericStock(Gender.FEMALE);
        }
        Generation genF = new Generation(stockF, gen.getLevel()+1);
        Generation genM = new Generation(stockM, gen.getLevel()+1);
        gen.setFather(genF);
        gen.setMother(genM);
        list.add(gen);
        if(gen.getLevel()<4){
            createGeneration(genF, list);
            createGeneration(genM, list);
        }
        return gen;
    }
    
    private Stock createGenericStock(Gender sex){
        Stock genStock = new Stock(Utility.emptyValue ,Boolean.TRUE, viewStockType);
        genStock.setSex(sex);
        return genStock;
    }
    
    
    @Override
    public void listRefreshNeeded() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String getPageTitle() {
        System.out.println("getPageTitle: called");
        if(hasStock) return "Pedigree for:" + stock.getDisplayName();
        return "No Pedigree available";
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        System.out.println("setParameter: called with:" + parameter);
        if(parameter==null){
            hasStock = Boolean.FALSE;
        }else{
            stock = stockService.findById(Integer.valueOf(parameter));
            if(stock==null) {
                hasStock = Boolean.FALSE;
            }else{
                hasStock = Boolean.TRUE;
                viewStockType = stock.getStockType();
                name = stock.getDisplayName();
            }
        }
        add(createContent());
    }

}
